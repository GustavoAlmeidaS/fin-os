package io.github.gustavoalmeidas.finos.shared.exception;

public class ForbiddenOperationException extends BusinessException {

    public ForbiddenOperationException(String message) {
        super("operation.forbidden", message);
    }

    public ForbiddenOperationException(String code, String message) {
        super(code, message);
    }
}
