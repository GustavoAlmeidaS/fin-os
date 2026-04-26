package io.github.gustavoalmeidas.finos.shared.exception;

public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super("resource.not_found", message);
    }

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
