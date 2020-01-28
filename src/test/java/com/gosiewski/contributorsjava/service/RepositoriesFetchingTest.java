package com.gosiewski.contributorsjava.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gosiewski.contributorsjava.dto.incoming.RepositoryRequestDto;
import com.gosiewski.contributorsjava.error.ApiCallError;
import com.gosiewski.contributorsjava.error.IllegalArgumentError;
import com.gosiewski.contributorsjava.error.NotFoundError;
import com.gosiewski.contributorsjava.service.domain.Repository;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class RepositoriesFetchingTest {

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
    void shouldReturnErrorWhenNameBlank() {
        // given
        final var organizationName = "   ";

        // when
        final var result = service.getOrganizationRepos(organizationName);

        // then
        assertThat(result).containsLeftInstanceOf(IllegalArgumentError.class);
    }

    @Test
    void shouldFetchOrganizationRepos() throws URISyntaxException, JsonProcessingException {
        // given
        final var organizationName = "exampleName";
        final var expectedResult = List.of(
                new Repository("sampleRepository1"),
                new Repository("sampleRepository2")
        );
        final var response = expectedResult
                .map(repository -> new RepositoryRequestDto(repository.getName()));
        final var url = String.format("https://api.github.com/orgs/%1$s/repos", organizationName);

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response.toJavaList()))
                );

        final var result = service.getOrganizationRepos(organizationName);

        // then
        assertThat(result).containsOnRight(expectedResult);
    }

    @Test
    void shouldFetchAllPages() throws URISyntaxException, JsonProcessingException {
        // given
        final var organizationName = "exampleName";
        final var expectedResult = List.of(
                new Repository("sampleRepository1"),
                new Repository("sampleRepository2"),
                new Repository("sampleRepository3"),
                new Repository("sampleRepository4"),
                new Repository("sampleRepository5")
        );
        final var response = expectedResult
                .map(repository -> new RepositoryRequestDto(repository.getName()));
        final var firstPageUrl = String.format("https://api.github.com/orgs/%1$s/repos", organizationName);;
        final var secondPageUrl = String.format("https://api.github.com/orgs/%1$s/repos?page=2",
                organizationName);;
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
                .andExpect(queryParam("page", "2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response.subSequence(2).toJavaList()))
                );

        final var result = service.getOrganizationRepos(organizationName);

        // then
        assertThat(result).containsOnRight(expectedResult);
    }

    @Test
    void shouldFetchEmptyResult() throws URISyntaxException, JsonProcessingException {
        // given
        final var organizationName = "exampleName";
        final Seq<Repository> expectedResult = List.empty();
        final Seq<RepositoryRequestDto> response = List.empty();
        final var url = String.format("https://api.github.com/orgs/%1$s/repos", organizationName);

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(response.toJavaList()))
                );

        final var result = service.getOrganizationRepos(organizationName);

        // then
        assertThat(result).containsOnRight(expectedResult);
    }

    @Test
    void shouldForwardNotFound() throws URISyntaxException {
        // given
        final var organizationName = "nonExistingOrgName";
        final var url = String.format("https://api.github.com/orgs/%1$s/repos", organizationName);

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                );

        final var result = service.getOrganizationRepos(organizationName);

        // then
        assertThat(result).containsLeftInstanceOf(NotFoundError.class);
    }

    @Test
    void shouldReturnErrorWhenResponseCodeUnexpected() throws URISyntaxException {
        // given
        final var organizationName = "nonExistingOrgName";
        final var url = String.format("https://api.github.com/orgs/%1$s/repos", organizationName);

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                );

        final var result = service.getOrganizationRepos(organizationName);

        // then
        assertThat(result).containsLeftInstanceOf(ApiCallError.class);
    }

    @Test
    void shouldReturnErrorWhenResponseBodyDifferent() throws URISyntaxException {
        // given
        final var organizationName = "nonExistingOrgName";
        final var url = String.format("https://api.github.com/orgs/%1$s/repos", organizationName);

        // when
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Unexpected body")
                );

        final var result = service.getOrganizationRepos(organizationName);

        // then
        assertThat(result).containsLeftInstanceOf(ApiCallError.class);
    }
}
