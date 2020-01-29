package com.gosiewski.contributorsjava.error;

public final class BlankOrganisationNameError extends DomainError {
    public BlankOrganisationNameError(String reason) {
        super(reason);
    }
}
