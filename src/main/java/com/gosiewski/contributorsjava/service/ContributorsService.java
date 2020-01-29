package com.gosiewski.contributorsjava.service;

import com.gosiewski.contributorsjava.dto.outgoing.ContributorDto;
import com.gosiewski.contributorsjava.error.DomainError;
import com.gosiewski.contributorsjava.service.domain.Contributor;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ContributorsService {

    private final GitHubAPIService gitHubAPIService;

    public Either<DomainError, Seq<ContributorDto>> getContributorsByOrganization(final String organizationName) {
        return gitHubAPIService.getOrganizationRepos(organizationName)
                .map(repositories -> List.ofAll(repositories
                        .toJavaParallelStream()
                        .map(repository -> gitHubAPIService.getRepoContributors(organizationName, repository.getName())))
                )
                .flatMap(Either::sequenceRight)
                .map(contributors -> contributors
                        .flatMap(__ -> __)
                        .groupBy(Contributor::getLogin)
                        .values()
                        .flatMap(contributorContributions -> contributorContributions
                                .reduceOption((contribution, otherContribution) -> new Contributor(contribution.getLogin(),
                                        contribution.getContributionsAmount() + otherContribution.getContributionsAmount())))
                        .sorted(Comparator.comparing(Contributor::getContributionsAmount).reversed())
                        .map(contributor -> new ContributorDto(contributor.getLogin(), contributor.getContributionsAmount())));
    }
}
