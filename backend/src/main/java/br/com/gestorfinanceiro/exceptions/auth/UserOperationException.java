package br.com.gestorfinanceiro.exceptions.auth;

public class UserOperationException extends RuntimeException {
    public UserOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
