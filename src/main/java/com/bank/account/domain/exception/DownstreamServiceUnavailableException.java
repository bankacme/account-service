package com.bank.account.domain.exception;

public class DownstreamServiceUnavailableException extends RuntimeException {

    public DownstreamServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " did not respond in time", cause);
    }
}
