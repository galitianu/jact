package io.jact.core.internal;

public class JactRuntimeException extends RuntimeException {
    public JactRuntimeException(String message) {
        super(message);
    }

    public JactRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
