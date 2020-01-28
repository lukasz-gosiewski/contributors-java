package com.gosiewski.contributorsjava.service;


import com.gosiewski.contributorsjava.error.Error;
import com.gosiewski.contributorsjava.service.domain.Contributor;
import com.gosiewski.contributorsjava.service.domain.Repository;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
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
        final Either<Error, Seq<Repository>> result = service.getOrganizationRepos("dook");

        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).isNotNull();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    void shouldReturnOrganizationReposWithPagination() {
        final Either<Error, Seq<Repository>> result = service.getOrganizationRepos("intive");

        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).isNotNull();
        assertThat(result.get()).hasSize(70);
    }

    @Test
    void shouldReturnRepoContributorsWhenNoPagination() {
        final Either<Error, Seq<Contributor>> result = service.getRepoContributors("dook", "internal-tools");

        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).isNotNull();
        assertThat(result.get()).hasSize(1);
    }

    @Test
    void shouldReturnRepoContributorsWithPagination() {
        final Either<Error, Seq<Contributor>> result = service.getRepoContributors("typelevel", "cats");

        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).isNotNull();
        assertThat(result.get()).hasSize(350);
    }
}
