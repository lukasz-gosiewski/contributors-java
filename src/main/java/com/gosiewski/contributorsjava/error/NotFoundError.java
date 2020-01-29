package com.gosiewski.contributorsjava.error;

public final class NotFoundError extends DomainError {
    public NotFoundError() {
        super("Not found");
    }
}
