package com.smartSure.claimService.exception;

public class InvalidClaimStateException extends RuntimeException {
    public InvalidClaimStateException(String message) {
        super(message);
    }
}
