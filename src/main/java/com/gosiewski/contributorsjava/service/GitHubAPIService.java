package com.gosiewski.contributorsjava.service;

import com.gosiewski.contributorsjava.dto.incoming.ContributorRequestDto;
import com.gosiewski.contributorsjava.dto.incoming.RepositoryRequestDto;
import com.gosiewski.contributorsjava.error.ApiCallError;
import com.gosiewski.contributorsjava.error.Error;
import com.gosiewski.contributorsjava.error.IllegalArgumentError;
import com.gosiewski.contributorsjava.service.domain.Contributor;
import com.gosiewski.contributorsjava.service.domain.Repository;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class GitHubAPIService {
    final static Logger logger = LoggerFactory.getLogger(GitHubAPIService.class);

    private final static String REPOS_URL = "https://api.github.com/orgs/%1$s/repos";
    private final static String CONTRIBUTORS_URL = "https://api.github.com/repos/%1$s/%2$s/contributors";

    private final HttpClient httpClient;

    final Either<Error, Seq<Repository>> getOrganizationRepos(final String organizationName) {
        if (organizationName.isBlank()) {
            return Either.left(new IllegalArgumentError("Organization name cannot be blank."));
        }

        final String url = String.format(REPOS_URL, organizationName);

        return getFullGitHubResource(url, RepositoryRequestDto.class).map(this::mapRepositoryDtos);
    }

    final Either<Error, Seq<Contributor>> getRepoContributors(final String ownerName,
                                                              final String repoName) {
        if (ownerName.isBlank() || repoName.isBlank()) {
            return Either.left(new IllegalArgumentError("Owner name or repo name cannot be blank."));
        }

        final String url = String.format(CONTRIBUTORS_URL, ownerName, repoName);

        return getFullGitHubResource(url, ContributorRequestDto.class).map(this::mapContributorDtos);
    }

    private <T> Either<Error, Seq<T>> getFullGitHubResource(final String url, final Class<T> clazz) {
        Either<Error, ResponseEntity<java.util.List<T>>> result = httpClient.fetchFirstPage(url, clazz);

        if (result.isLeft()) {
            return Either.left(result.getLeft());
        }

        ResponseEntity<java.util.List<T>> response = result.get();

        if (response.getBody() == null) {
            logEmptyBodyError(response.toString());

            return Either.left(new ApiCallError());
        }

        final java.util.List<T> fullResult = new ArrayList<>(response.getBody());

        while(httpClient.hasNextPage(response.getHeaders())) {
            result = httpClient.fetchNextPage(response, clazz);

            if (result.isLeft()) {
                return Either.left(result.getLeft());
            }

            response = result.get();

            if (response.getBody() == null) {
                logEmptyBodyError(response.toString());

                return Either.left(new ApiCallError());
            }

            fullResult.addAll(response.getBody());
        }

        return Either.right(List.ofAll(fullResult));
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
