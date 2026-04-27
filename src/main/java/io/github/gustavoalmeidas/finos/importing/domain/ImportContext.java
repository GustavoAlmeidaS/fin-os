package io.github.gustavoalmeidas.finos.importing.domain;

import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportContext {
    private Account account;
    private String fileName;
}
