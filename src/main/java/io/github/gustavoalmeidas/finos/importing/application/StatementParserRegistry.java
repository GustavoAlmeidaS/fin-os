package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.importing.domain.StatementDetectionContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StatementParserRegistry {

    private final List<StatementParser> parsers;

    public StatementParserRegistry(List<StatementParser> parsers) {
        this.parsers = parsers;
    }

    public Optional<StatementParser> findParser(StatementDetectionContext context) {
        return parsers.stream()
                .filter(parser -> parser.supports(context))
                .findFirst();
    }
}
