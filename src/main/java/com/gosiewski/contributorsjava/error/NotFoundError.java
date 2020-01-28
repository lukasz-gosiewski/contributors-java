package com.gosiewski.contributorsjava.error;

public final class NotFoundError extends Error {
    public NotFoundError() {
        super("Not found");
    }
}
