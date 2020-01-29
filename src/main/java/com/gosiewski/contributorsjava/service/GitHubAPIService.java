package com.gosiewski.contributorsjava.service;

import com.gosiewski.contributorsjava.dto.incoming.ContributorRequestDto;
import com.gosiewski.contributorsjava.dto.incoming.RepositoryRequestDto;
import com.gosiewski.contributorsjava.error.ApiCallError;
import com.gosiewski.contributorsjava.error.DomainError;
import com.gosiewski.contributorsjava.error.IllegalArgumentError;
import com.gosiewski.contributorsjava.service.domain.Contributor;
import com.gosiewski.contributorsjava.service.domain.Repository;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GitHubAPIService {
    final static Logger logger = LoggerFactory.getLogger(GitHubAPIService.class);

    private final static String REPOS_URL = "https://api.github.com/orgs/%1$s/repos";
    private final static String CONTRIBUTORS_URL = "https://api.github.com/repos/%1$s/%2$s/contributors";

    private final HttpClient httpClient;

    final Either<DomainError, Seq<Repository>> getOrganizationRepos(final String organizationName) {
        if (organizationName.isBlank()) {
            return Either.left(new IllegalArgumentError("Organization name cannot be blank."));
        }

        final var url = String.format(REPOS_URL, organizationName);

        return getFullGitHubResource(url, RepositoryRequestDto.class).map(this::mapRepositoryDtos);
    }

    final Either<DomainError, Seq<Contributor>> getRepoContributors(final String ownerName,
                                                                    final String repoName) {
        if (ownerName.isBlank() || repoName.isBlank()) {
            return Either.left(new IllegalArgumentError("Owner name or repo name cannot be blank."));
        }

        final var url = String.format(CONTRIBUTORS_URL, ownerName, repoName);

        return getFullGitHubResource(url, ContributorRequestDto.class).map(this::mapContributorDtos);
    }

    private <T> Either<DomainError, Seq<T>> getFullGitHubResource(final String url, final Class<T> clazz) {
        return fetchMore(List.empty(), url, clazz);
    }

    private <T> Either<DomainError, Seq<T>> fetchMore(final List<T> acc, final String url,
                                                      final Class<T> clazz) {
        var nextPageResult = httpClient.fetchPage(url, clazz);

        if (nextPageResult.isLeft()) {
            return Either.left(nextPageResult.getLeft());
        }

        final var response = nextPageResult.get();

        if (response.getBody() == null) {
            logEmptyBodyError(response.toString());

            return Either.left(new ApiCallError());
        }

        final var nextPageUrl = httpClient.getNextPageLink(response.getHeaders());
        if (!nextPageUrl.isEmpty()) {
            return fetchMore(acc.appendAll(response.getBody()), nextPageUrl.get(), clazz);
        } else {
            return Either.right(acc.appendAll(response.getBody()));
        }
    }

    private Seq<Contributor> mapContributorDtos(final Seq<ContributorRequestDto> dtos) {
        return dtos.map(contributorDto -> new Contributor(contributorDto.getLogin(), contributorDto.getContributions()));
    }

    private Seq<Repository> mapRepositoryDtos(final Seq<RepositoryRequestDto> dtos) {
        return dtos.map(repositoryDto -> new Repository(repositoryDto.getName()));
    }

    private void logEmptyBodyError(final String response) {
        logger.error("GitHub responded with empty body:");
        logger.error(response);
    }
}
