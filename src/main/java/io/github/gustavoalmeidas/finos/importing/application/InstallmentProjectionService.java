package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionStatus;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InstallmentProjectionService {

    private static final Pattern INSTALLMENT_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
    
    private final TransactionRepository transactionRepository;
    private final IdempotencyKeyService idempotencyKeyService;

    public InstallmentProjectionService(TransactionRepository transactionRepository,
                                        IdempotencyKeyService idempotencyKeyService) {
        this.transactionRepository = transactionRepository;
        this.idempotencyKeyService = idempotencyKeyService;
    }

    /**
     * Projects future installments based on a recently saved POSTED transaction.
     */
    @Transactional
    public void projectFutureInstallments(Transaction originalTx) {
        if (originalTx.getStatus() != TransactionStatus.POSTED) {
            return;
        }

        String installmentInfo = originalTx.getInstallmentInfo();
        if (installmentInfo == null || installmentInfo.isBlank()) {
            return;
        }

        Matcher matcher = INSTALLMENT_PATTERN.matcher(installmentInfo);
        if (!matcher.find()) {
            return;
        }

        try {
            int current = Integer.parseInt(matcher.group(1));
            int total = Integer.parseInt(matcher.group(2));

            if (current >= total) {
                return;
            }

            for (int i = current + 1; i <= total; i++) {
                String projectedKey = idempotencyKeyService.generateProjectedKey(originalTx.getIdempotencyKey(), i);
                
                // Idempotency check to avoid recreating if we run this again
                if (transactionRepository.existsByIdempotencyKey(projectedKey)) {
                    continue;
                }

                Transaction projectedTx = new Transaction();
                projectedTx.setUser(originalTx.getUser());
                projectedTx.setAccount(originalTx.getAccount());
                projectedTx.setImportBatch(originalTx.getImportBatch());
                projectedTx.setType(originalTx.getType());
                projectedTx.setAmount(originalTx.getAmount());
                projectedTx.setSource(originalTx.getSource());
                projectedTx.setCategory(originalTx.getCategory());
                projectedTx.setCard(originalTx.getCard());
                projectedTx.setCounterparty(originalTx.getCounterparty());
                projectedTx.setOriginalTransactionId(originalTx.getId());

                // Projection modifications
                projectedTx.setStatus(TransactionStatus.PENDING); // Used for PROJECTED
                projectedTx.setTransactionDate(originalTx.getTransactionDate().plusMonths(i - current));
                projectedTx.setIdempotencyKey(projectedKey);

                // Update strings
                String oldInstallmentStr = matcher.group(0);
                String newInstallmentStr = oldInstallmentStr.replace(String.valueOf(current), String.valueOf(i));
                
                projectedTx.setInstallmentInfo(installmentInfo.replace(oldInstallmentStr, newInstallmentStr));
                
                if (originalTx.getRawDescription() != null) {
                    projectedTx.setRawDescription(originalTx.getRawDescription().replace(oldInstallmentStr, newInstallmentStr));
                } else {
                    projectedTx.setRawDescription("");
                }
                
                if (originalTx.getSearchableDescription() != null) {
                    projectedTx.setSearchableDescription(originalTx.getSearchableDescription().replace(oldInstallmentStr, newInstallmentStr));
                } else {
                    projectedTx.setSearchableDescription("");
                }
                
                if (originalTx.getDescription() != null) {
                    projectedTx.setDescription(originalTx.getDescription().replace(oldInstallmentStr, newInstallmentStr));
                } else {
                    projectedTx.setDescription("");
                }

                transactionRepository.save(projectedTx);
            }
        } catch (NumberFormatException ignored) {
            // If the regex matches but numbers are somehow unparseable, skip safely
        }
    }
}
