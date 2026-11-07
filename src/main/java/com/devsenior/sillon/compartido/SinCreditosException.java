package com.devsenior.sillon.compartido;

public class SinCreditosException extends RuntimeException {
    public SinCreditosException(String message) {
        super(message);
    }
}
