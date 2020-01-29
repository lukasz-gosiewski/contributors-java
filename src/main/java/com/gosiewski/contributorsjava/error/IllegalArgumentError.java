package com.gosiewski.contributorsjava.error;

public final class IllegalArgumentError extends Error {
    public IllegalArgumentError(String reason) {
        super(reason);
    }
}
