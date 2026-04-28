package io.github.gustavoalmeidas.finos.ledger.infrastructure;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByIdAndUser(Long id, User user);
    
    @Query("SELECT t FROM Transaction t WHERE t.importBatch.id = :batchId")
    List<Transaction> findByImportBatchId(@Param("batchId") Long batchId);
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.status = 'PENDING' AND t.amount = :amount AND t.installmentInfo = :installmentInfo AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findProjectedMatches(
        @Param("accountId") Long accountId, 
        @Param("amount") BigDecimal amount, 
        @Param("installmentInfo") String installmentInfo, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t.category.id FROM Transaction t WHERE t.user.id = :userId AND t.searchableDescription = :desc AND t.category IS NOT NULL GROUP BY t.category.id ORDER BY COUNT(t) DESC")
    List<Long> findMostFrequentCategoryIdByDescription(@Param("userId") Long userId, @Param("desc") String desc);
}
