package com.gosiewski.contributorsjava.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class DomainError extends RuntimeException {
    private final String reason;
}
