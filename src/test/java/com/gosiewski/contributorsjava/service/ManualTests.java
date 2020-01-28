package com.gosiewski.contributorsjava.service;


import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

// This is just a test for TDD approach, not the production one. It should not be used as it makes real HTTP calls and
// bases on a changing data about GitHub repos and contributors.
// Demonstration + manual testing purposes.
// To use this you should have your own testing GitHub token and place it below.
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(properties = {"GH_TOKEN = PLACE_TOKEN_HERE"})
public class ManualTests {

    @Inject
    private GitHubAPIService service;

    @Test
    void shouldReturnOrganizationReposWhenNoPagination() {
        final var result = service.getOrganizationRepos("dook");

        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).isNotNull();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    void shouldReturnOrganizationReposWithPagination() {
        final var result = service.getOrganizationRepos("intive");

        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).isNotNull();
        assertThat(result.get()).hasSize(70);
    }

    @Test
    void shouldReturnRepoContributorsWhenNoPagination() {
        final var result = service.getRepoContributors("dook", "internal-tools");

        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).isNotNull();
        assertThat(result.get()).hasSize(1);
    }

    @Test
    void shouldReturnRepoContributorsWithPagination() {
        final var result = service.getRepoContributors("typelevel", "cats");

        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).isNotNull();
        assertThat(result.get()).hasSize(350);
    }
}
