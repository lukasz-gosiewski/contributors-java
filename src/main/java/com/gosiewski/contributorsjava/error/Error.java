package com.gosiewski.contributorsjava.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class Error {
    private final String reason;
}
