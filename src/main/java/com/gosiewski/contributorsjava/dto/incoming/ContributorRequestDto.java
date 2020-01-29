package com.gosiewski.contributorsjava.dto.incoming;

import lombok.Value;

@Value
public final class ContributorRequestDto {
    private final String login;
    private final int contributions;
}
