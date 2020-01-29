package com.gosiewski.contributorsjava.error;

public final class IllegalArgumentError extends DomainError {
    public IllegalArgumentError(String reason) {
        super(reason);
    }
}
