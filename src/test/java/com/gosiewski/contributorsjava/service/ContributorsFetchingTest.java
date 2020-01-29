package com.gosiewski.contributorsjava.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gosiewski.contributorsjava.dto.incoming.ContributorRequestDto;
import com.gosiewski.contributorsjava.error.ApiCallError;
import com.gosiewski.contributorsjava.error.IllegalArgumentError;
import com.gosiewski.contributorsjava.error.NotFoundError;
import com.gosiewski.contributorsjava.service.domain.Contributor;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.vavr.api.VavrAssertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ContributorsFetchingTest {

    @Inject
    private GitHubAPIService service;

    @Inject
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public  void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void shouldReturnErrorWhenOwnerNameBlank() {
        //given
        final var ownerName = "      ";
        final var repoName = "repositoryName";

        // when
        final var result = service.getRepoContributors(ownerName, repoName);

        // then
        assertThat(result).containsLeftInstanceOf(IllegalArgumentError.class);
    }

    @Test
    void shouldReturnErrorWhenRepoNameBlank() {
        //given
        final var ownerName = "ownerName";
        final var repoName = "       ";

        // when
        final var result = service.getRepoContributors(ownerName, repoName);

        // then
        assertThat(result).containsLeftInstanceOf(IllegalArgumentError.class);
    }

    @Test
    void shouldFetchRepoContributors() throws URISyntaxException, JsonProcessingException {
        //given
        final var ownerName = "ownerName";
        final var repoName = "repoName";
        final var expectedResult = List.of(
                new Contributor("sampleLogin", 5),
                new Contributor("sampleLogin", 12),
                new Contributor("sampleLogin", 123)
        );
        final var response = expectedResult
                .map(contributor -> new ContributorRequestDto(contributor.getLogin(),
                        contributor.getContributionsAmount()));
        final var url = String.format("https://api.github.com/repos/%1$s/%2$s/contributors", ownerName, repoName);

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response.toJavaList()))
                );

        final var result = service.getRepoContributors(ownerName, repoName);

        // then
        assertThat(result).containsOnRight(expectedResult);
    }

    @Test
    void shouldFetchAllPages() throws URISyntaxException, JsonProcessingException {
        //given
        final var ownerName = "ownerName";
        final var repoName = "repoName";
        final var expectedResult = List.of(
                new Contributor("sampleLogin", 5),
                new Contributor("sampleLogin", 12),
                new Contributor("sampleLogin", 123),
                new Contributor("sampleLogin", 0),
                new Contributor("sampleLogin", 3)
        );
        final var response = expectedResult
                .map(contributor -> new ContributorRequestDto(contributor.getLogin(),
                        contributor.getContributionsAmount()));
        final var firstPageUrl = String.format("https://api.github.com/repos/%1$s/%2$s/contributors", ownerName,
                repoName);
        final var secondPageUrl = String.format("https://api.github.com/repos/%1$s/%2$s/contributors?page=2", ownerName,
                repoName);;
        final var firstPageHeaders = new HttpHeaders();
        firstPageHeaders.add("Link",
                "<" + secondPageUrl + ">; rel=\"next\",\n" +
                "<https://api.github.com/resource?page=3>; rel=\"last\"");

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(firstPageUrl)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(firstPageHeaders)
                        .body(mapper.writeValueAsString(response.subSequence(0, 2).toJavaList()))
                );

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(secondPageUrl)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response.subSequence(2).toJavaList()))
                );

        final var result = service.getRepoContributors(ownerName, repoName);

        // then
        assertThat(result).containsOnRight(expectedResult);
    }

    @Test
    void shouldFetchEmptyResult() throws URISyntaxException, JsonProcessingException {
        //given
        final var ownerName = "ownerName";
        final var repoName = "repoName";
        final Seq<Contributor> expectedResult = List.empty();
        final Seq<ContributorRequestDto> response = List.empty();
        final var url = String.format("https://api.github.com/repos/%1$s/%2$s/contributors", ownerName,
                repoName);

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response.toJavaList()))
                );

        final var result = service.getRepoContributors(ownerName, repoName);

        // then
        assertThat(result).containsOnRight(expectedResult);
    }

    @Test
    void shouldForwardNotFound() throws URISyntaxException {
        //given
        final var ownerName = "ownerName";
        final var repoName = "repoName";
        final var url = String.format("https://api.github.com/repos/%1$s/%2$s/contributors", ownerName, repoName);

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                );

        final var result = service.getRepoContributors(ownerName, repoName);

        // then
        assertThat(result).containsLeftInstanceOf(NotFoundError.class);
    }

    @Test
    void shouldReturnErrorWhenResponseCodeUnexpected() throws URISyntaxException {
        //given
        final var ownerName = "ownerName";
        final var repoName = "repoName";
        final var url = String.format("https://api.github.com/repos/%1$s/%2$s/contributors", ownerName, repoName);

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                );

        final var result = service.getRepoContributors(ownerName, repoName);

        // then
        assertThat(result).containsLeftInstanceOf(ApiCallError.class);
    }

    @Test
    void shouldReturnErrorWhenResponseBodyDifferent() throws URISyntaxException {
        //given
        final var ownerName = "ownerName";
        final var repoName = "repoName";
        final var url = String.format("https://api.github.com/repos/%1$s/%2$s/contributors", ownerName, repoName);

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Unexpected body")
                );

        final var result = service.getRepoContributors(ownerName, repoName);

        // then
        assertThat(result).containsLeftInstanceOf(ApiCallError.class);
    }
}
