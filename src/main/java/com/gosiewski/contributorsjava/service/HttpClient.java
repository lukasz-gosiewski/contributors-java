package com.gosiewski.contributorsjava.service;

import com.gosiewski.contributorsjava.error.ApiCallError;
import com.gosiewski.contributorsjava.error.DomainError;
import com.gosiewski.contributorsjava.error.NotFoundError;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

@Component
@RequiredArgsConstructor
public class HttpClient {
    final static Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private final static String GITHUB_API_V3_ACCEPT_HEADER_VALUE = "application/vnd.github.v3+json";

    private final static String LINK_HEADER_NAME = "Link";

    private final static String LINKS_DELIMITER = ",";
    private final static String LINKS_PARAM_DELIMITER = ";";

    private final static String METADATA_REL = "rel";
    private final static String METADATA_FIRST = "first";
    private final static String METADATA_LAST = "last";
    private final static String METADATA_PREVIOUS = "prev";
    private final static String METADATA_NEXT = "next";

    @Inject
    private final RestTemplate restTemplate;

    public final <T> Either<DomainError, ResponseEntity<List<T>>> fetchPage(final String url, final Class<T> clazz) {
        try {
            // Add header to accept only particular API version responses
            final var headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT, GITHUB_API_V3_ACCEPT_HEADER_VALUE);
            final var entity = new HttpEntity<>("parameters", headers);

            return Either.right(restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ParameterizedTypeReference.forType(ResolvableType.forClassWithGenerics(List.class, clazz).getType())));
        } catch (final HttpStatusCodeException codeException) {
            if (codeException.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Either.left(new NotFoundError());
            } else {
                logger.error("GitHub responded with unexpected code:");
                logger.error(codeException.getLocalizedMessage());

                return Either.left(new ApiCallError());
            }
        } catch (final Exception ex) {
            logger.error("HTTP call error:");
            logger.error(ex.getLocalizedMessage());

            return Either.left(new ApiCallError());
        }
    }

    public final Option<String> getNextPageLink(final HttpHeaders httpHeaders) {
        return Option.of(parseLinkHeader(httpHeaders).nextPage);
    }

    // Mostly from:
    // https://github.com/eclipse/egit-github/blob/master/org.eclipse.egit.github.core/src/org/eclipse/egit/github/core/client/PageLinks.java#L43-75
    // Did not want to implement my own parser for pagination header, neither to add a library for 2 requests.
    private LinksPagination parseLinkHeader(final HttpHeaders responseHeaders) {
        final var linkHeader = responseHeaders.getFirst(LINK_HEADER_NAME);
        final var result = new LinksPagination();

        if (linkHeader != null) {
            final var links = linkHeader.split(LINKS_DELIMITER);
            for (var link : links) {
                final var segments = link.split(LINKS_PARAM_DELIMITER);
                if (segments.length < 2)
                    continue;

                var linkPart = segments[0].trim();
                if (!linkPart.startsWith("<") || !linkPart.endsWith(">"))
                    continue;
                linkPart = linkPart.substring(1, linkPart.length() - 1);

                for (int i = 1; i < segments.length; i++) {
                    final var rel = segments[i].trim().split("=");
                    if (rel.length < 2 || !METADATA_REL.equals(rel[0]))
                        continue;

                    var relValue = rel[1];
                    if (relValue.startsWith("\"") && relValue.endsWith("\""))
                        relValue = relValue.substring(1, relValue.length() - 1);

                    switch (relValue) {
                        case METADATA_FIRST:
                            result.setFirstPage(linkPart);
                            break;
                        case METADATA_LAST:
                            result.setLastPage(linkPart);
                            break;
                        case METADATA_NEXT:
                            result.setNextPage(linkPart);
                            break;
                        case METADATA_PREVIOUS:
                            result.setPreviousPage(linkPart);
                            break;
                    }
                }
            }
        }

        return result;
    }

    @Getter
    @Setter
    static class LinksPagination {
        private String firstPage;
        private String lastPage;
        private String previousPage;
        private String nextPage;
    }
}
