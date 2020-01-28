package com.gosiewski.contributorsjava.service;

import com.gosiewski.contributorsjava.error.ApiCallError;
import com.gosiewski.contributorsjava.error.Error;
import com.gosiewski.contributorsjava.error.NotFoundError;
import io.vavr.control.Either;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class HttpClient {
    final static Logger logger = LoggerFactory.getLogger(HttpClient.class);

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

    public final <T> Either<Error, ResponseEntity<List<T>>> fetchFirstPage(final String url, final Class<T> clazz) {
        try {
            return Either.right(restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
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
            logger.error("GitHub api call error:");
            logger.error(ex.getLocalizedMessage());

            return Either.left(new ApiCallError());
        }
    }

    public final <T> Either<Error, ResponseEntity<List<T>>> fetchNextPage(final ResponseEntity<List<T>> response,
                                                                          final Class<T> clazz) {
        final LinksPagination linksPagination = parseLinksPaginationHeader(response.getHeaders());

        try {
            return Either.right(restTemplate.exchange(
                    linksPagination.getNextPage(),
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    ParameterizedTypeReference.forType(ResolvableType.forClassWithGenerics(List.class, clazz).getType())));
        } catch(final HttpStatusCodeException codeException) {
            if (codeException.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Either.left(new NotFoundError());
            } else {
                logger.error("GitHub responded with unexpected code:");
                logger.error(codeException.getLocalizedMessage());

                return Either.left(new ApiCallError());
            }
        } catch (final Exception ex) {
            logger.error("GitHub api call error:");
            logger.error(ex.getLocalizedMessage());

            return Either.left(new ApiCallError());
        }
    }

    public final boolean hasNextPage(final HttpHeaders responseHeaders) {
        final LinksPagination linksPagination = parseLinksPaginationHeader(responseHeaders);
        return linksPagination.getNextPage() != null;
    }

    // Mostly from:
    // https://github.com/eclipse/egit-github/blob/master/org.eclipse.egit.github.core/src/org/eclipse/egit/github/core/client/PageLinks.java#L43-75
    // Did not want to implement my own parser for pagination header, neither to add a library for 2 requests.
    private LinksPagination parseLinksPaginationHeader(final HttpHeaders responseHeaders) {
        final String linkHeader = responseHeaders.getFirst(LINK_HEADER_NAME);
        final LinksPagination result = new LinksPagination();

        if (linkHeader != null) {
            final String[] links = linkHeader.split(LINKS_DELIMITER);
            for (String link : links) {
                final String[] segments = link.split(LINKS_PARAM_DELIMITER);
                if (segments.length < 2)
                    continue;

                String linkPart = segments[0].trim();
                if (!linkPart.startsWith("<") || !linkPart.endsWith(">"))
                    continue;
                linkPart = linkPart.substring(1, linkPart.length() - 1);

                for (int i = 1; i < segments.length; i++) {
                    final String[] rel = segments[i].trim().split("=");
                    if (rel.length < 2 || !METADATA_REL.equals(rel[0]))
                        continue;

                    String relValue = rel[1];
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
