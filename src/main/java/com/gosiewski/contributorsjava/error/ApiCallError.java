package com.gosiewski.contributorsjava.error;

public final class ApiCallError extends DomainError {
    public ApiCallError() {
        super("External HTTP response was different than expected.");
    }
}
