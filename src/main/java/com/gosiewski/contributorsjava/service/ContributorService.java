package com.gosiewski.contributorsjava.service;
import com.gosiewski.contributorsjava.dto.outgoing.ContributorDto;
import com.gosiewski.contributorsjava.error.DomainError;
import com.gosiewski.contributorsjava.service.domain.Contributor;
import com.gosiewski.contributorsjava.service.domain.Repository;
import io.vavr.collection.Seq;
import io.vavr.concurrent.Future;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContributorService {
    private final Executor executor = Executors.newFixedThreadPool(32);

    private final GitHubApiService gitHubAPIService;

    public Either<DomainError, Seq<ContributorDto>> getContributorsByOrganization(final String organizationName) {
        return gitHubAPIService.getOrganizationRepos(organizationName)
                .map(repositories -> fetchContributorsAsync(repositories, organizationName))
                .flatMap(Either::sequenceRight)
                .map(this::sortAndMergeContributorsEntries);
    }

    private Seq<Either<DomainError, Seq<Contributor>>> fetchContributorsAsync(final Seq<Repository> repositories,
                                                                              final String organizationName) {
        return Future.sequence(executor, repositories
                .map(repository -> gitHubAPIService.getRepoContributors(organizationName, repository.getName()))
                .collect(Collectors.toList()))
                .get();
    }

    private Seq<ContributorDto> sortAndMergeContributorsEntries(final Seq<Seq<Contributor>> contributors) {
        return contributors
                .flatMap(Function.identity())
                .groupBy(Contributor::getLogin)
                .values()
                .flatMap(contributorContributions -> contributorContributions
                        .reduceOption((contribution, otherContribution) -> new Contributor(contribution.getLogin(),
                                contribution.getContributionsAmount() + otherContribution.getContributionsAmount())))
                .sorted(Comparator.comparing(Contributor::getContributionsAmount).reversed())
                .map(this::mapContributorToDto);
    }

    private ContributorDto mapContributorToDto(final Contributor contributor) {
        return new ContributorDto(contributor.getLogin(), contributor.getContributionsAmount());
    }
}