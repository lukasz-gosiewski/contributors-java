package com.gosiewski.contributorsjava.service;

import com.gosiewski.contributorsjava.dto.outgoing.ContributorDto;
import com.gosiewski.contributorsjava.error.ApiCallError;
import com.gosiewski.contributorsjava.error.DomainError;
import com.gosiewski.contributorsjava.service.domain.Contributor;
import com.gosiewski.contributorsjava.service.domain.Repository;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.concurrent.Future;
import io.vavr.control.Either;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrganizationContributorsFetchingTest {

    private ContributorService service;

    private GitHubApiService mockedGitHubAPIService;

    @BeforeEach
    void beforeEach() {
        this.mockedGitHubAPIService = mock(GitHubApiService.class);
        this.service = new ContributorService(mockedGitHubAPIService);
    }

    @Test
    void shouldFetchAllContributorsForEveryRepository() {
        // given
        final var organizationName = "exampleName";
        final var repositoryName1 = "exampleRepository1";
        final var repositoryName2 = "exampleRepository2";
        final var repositoryName3 = "exampleRepository3";
        final Either<DomainError, Seq<Repository>> organizationRepos = Either.right(List.of(
                new Repository(repositoryName1),
                new Repository(repositoryName2),
                new Repository(repositoryName3)
        ));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors1 = Future.successful(Either.right(List.of(
                new Contributor("exampleContributor1", 45),
                new Contributor("exampleContributor2", 12),
                new Contributor("exampleContributor3", 1)
        )));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors2 = Future.successful(Either.right(List.of(
                new Contributor("exampleContributor4", 0),
                new Contributor("exampleContributor5", 80)
        )));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors3 = Future.successful(Either.right(List.empty()));
        final var expectedResult = List.of(
                new ContributorDto("exampleContributor1", 45),
                new ContributorDto("exampleContributor2", 12),
                new ContributorDto("exampleContributor3", 1),
                new ContributorDto("exampleContributor4", 0),
                new ContributorDto("exampleContributor5", 80)
        );


        // when
        when(mockedGitHubAPIService.getOrganizationRepos(organizationName))
                .thenReturn(organizationRepos);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName1))
                .thenReturn(repoContributors1);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName2))
                .thenReturn(repoContributors2);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName3))
                .thenReturn(repoContributors3);

        final var result = service.getContributorsByOrganization(organizationName);

        // then
        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).containsExactlyInAnyOrderElementsOf(expectedResult);
    }

    @Test
    void shouldReturnEmptyListWhenNoContributors() {
        // given
        final var organizationName = "exampleName";
        final var repositoryName1 = "exampleRepository1";
        final var repositoryName2 = "exampleRepository2";
        final var repositoryName3 = "exampleRepository3";
        final Either<DomainError, Seq<Repository>> organizationRepos = Either.right(List.of(
                new Repository(repositoryName1),
                new Repository(repositoryName2),
                new Repository(repositoryName3)
        ));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors1 = Future.successful(Either.right(List.empty()));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors2 = Future.successful(Either.right(List.empty()));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors3 = Future.successful(Either.right(List.empty()));
        final Seq<ContributorDto> expectedResult = List.empty();


        // when
        when(mockedGitHubAPIService.getOrganizationRepos(organizationName))
                .thenReturn(organizationRepos);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName1))
                .thenReturn(repoContributors1);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName2))
                .thenReturn(repoContributors2);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName3))
                .thenReturn(repoContributors3);

        final var result = service.getContributorsByOrganization(organizationName);

        // then
        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).containsExactlyInAnyOrderElementsOf(expectedResult);
    }

    @Test
    void shouldReturnEmptyListWhenNoRepositories() {
        // given
        final var organizationName = "exampleName";
        final Either<DomainError, Seq<Repository>> organizationRepos = Either.right(List.empty());
        final Seq<ContributorDto> expectedResult = List.empty();

        // when
        when(mockedGitHubAPIService.getOrganizationRepos(organizationName))
                .thenReturn(organizationRepos);

        final var result = service.getContributorsByOrganization(organizationName);

        // then
        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).containsExactlyInAnyOrderElementsOf(expectedResult);
    }

    @Test
    void shouldReduceContributors() {
        // given
        final var organizationName = "exampleName";
        final var repositoryName1 = "exampleRepository1";
        final var repositoryName2 = "exampleRepository2";
        final var repositoryName3 = "exampleRepository3";
        final Either<DomainError, Seq<Repository>> organizationRepos = Either.right(List.of(
                new Repository(repositoryName1),
                new Repository(repositoryName2),
                new Repository(repositoryName3)
        ));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors1 = Future.successful(Either.right(List.of(
                new Contributor("exampleContributor2", 45),
                new Contributor("exampleContributor2", 12),
                new Contributor("exampleContributor3", 1)
        )));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors2 = Future.successful(Either.right(List.of(
                new Contributor("exampleContributor4", 0),
                new Contributor("exampleContributor2", 80)
        )));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors3 = Future.successful(Either.right(List.empty()));
        final var expectedResult = List.of(
                new ContributorDto("exampleContributor2", 137),
                new ContributorDto("exampleContributor3", 1),
                new ContributorDto("exampleContributor4", 0)
        );


        // when
        when(mockedGitHubAPIService.getOrganizationRepos(organizationName))
                .thenReturn(organizationRepos);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName1))
                .thenReturn(repoContributors1);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName2))
                .thenReturn(repoContributors2);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName3))
                .thenReturn(repoContributors3);

        final var result = service.getContributorsByOrganization(organizationName);

        // then
        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).containsExactlyInAnyOrderElementsOf(expectedResult);
    }

    @Test
    void shouldSortContributors() {
        // given
        final var organizationName = "exampleName";
        final var repositoryName1 = "exampleRepository1";
        final var repositoryName2 = "exampleRepository2";
        final var repositoryName3 = "exampleRepository3";
        final Either<DomainError, Seq<Repository>> organizationRepos = Either.right(List.of(
                new Repository(repositoryName1),
                new Repository(repositoryName2),
                new Repository(repositoryName3)
        ));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors1 = Future.successful(Either.right(List.of(
                new Contributor("exampleContributor1", 45),
                new Contributor("exampleContributor2", 12),
                new Contributor("exampleContributor3", 1)
        )));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors2 = Future.successful(Either.right(List.of(
                new Contributor("exampleContributor4", 0),
                new Contributor("exampleContributor5", 80)
        )));
        final Future<Either<DomainError, Seq<Contributor>>> repoContributors3 = Future.successful(Either.right(List.empty()));
        final var expectedResult = List.of(
                new ContributorDto("exampleContributor5", 80),
                new ContributorDto("exampleContributor1", 45),
                new ContributorDto("exampleContributor2", 12),
                new ContributorDto("exampleContributor3", 1),
                new ContributorDto("exampleContributor4", 0)
        );


        // when
        when(mockedGitHubAPIService.getOrganizationRepos(organizationName))
                .thenReturn(organizationRepos);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName1))
                .thenReturn(repoContributors1);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName2))
                .thenReturn(repoContributors2);
        when(mockedGitHubAPIService.getRepoContributors(organizationName, repositoryName3))
                .thenReturn(repoContributors3);

        final var result = service.getContributorsByOrganization(organizationName);

        // then
        VavrAssertions.assertThat(result).isRight();
        assertThat(result.get()).containsExactlyElementsOf(expectedResult);
    }

    @Test
    void shouldForwardReposFetchingErrors() {
        // given
        final var organizationName = "exampleName";

        // when
        when(mockedGitHubAPIService.getOrganizationRepos(organizationName))
                .thenReturn(Either.left(new ApiCallError()));

        final var result = service.getContributorsByOrganization(organizationName);

        // then
        VavrAssertions.assertThat(result).containsLeftInstanceOf(ApiCallError.class);
    }

    @Test
    void shouldForwardContributorFetchingErrors() {
        // given
        final var organizationName = "exampleName";
        final var repositoryName1 = "exampleRepository1";
        final Either<DomainError, Seq<Repository>> organizationRepos = Either.right(List.of(
                new Repository(repositoryName1)
        ));

        // when
        when(mockedGitHubAPIService.getOrganizationRepos(organizationName))
                .thenReturn(organizationRepos);
        when(mockedGitHubAPIService.getRepoContributors(any(), any()))
                .thenReturn(Future.successful(Either.left(new ApiCallError())));

        final var result = service.getContributorsByOrganization(organizationName);

        // then
        VavrAssertions.assertThat(result).containsLeftInstanceOf(ApiCallError.class);
    }
}
