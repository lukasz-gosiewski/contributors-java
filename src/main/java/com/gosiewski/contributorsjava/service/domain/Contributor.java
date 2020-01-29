package com.gosiewski.contributorsjava.service.domain;

import lombok.Value;

@Value
public class Contributor {
    private final String login;
    private final int contributionsAmount;
}
