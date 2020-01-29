package com.gosiewski.contributorsjava.controller;

import com.gosiewski.contributorsjava.dto.outgoing.ContributorDto;
import com.gosiewski.contributorsjava.dto.outgoing.ErrorDto;
import com.gosiewski.contributorsjava.error.ApiCallError;
import com.gosiewski.contributorsjava.error.DomainError;
import com.gosiewski.contributorsjava.error.BlankOrganisationNameError;
import com.gosiewski.contributorsjava.error.NotFoundError;
import com.gosiewski.contributorsjava.service.ContributorService;
import io.vavr.collection.Seq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ContributorController {

    private final ContributorService service;

    @GetMapping("/org/{organizationName}/contributors")
    public final ResponseEntity<Seq<ContributorDto>> getContributorsByOrganization(
            @PathVariable final String organizationName) {
        final var result = service.getContributorsByOrganization(organizationName);

        return new ResponseEntity<>(result.getOrElseThrow(result::getLeft), HttpStatus.OK);
    }

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ErrorDto> handleErrors(final HttpServletRequest req, final Exception ex) {
        if (ex instanceof ApiCallError) {
            return new ResponseEntity<>(new ErrorDto("Sorry, we have troubles fetching repositories. Please, try again later"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (ex instanceof BlankOrganisationNameError) {
            return new ResponseEntity<>(new ErrorDto("Organisation name cannot be blank"), HttpStatus.BAD_REQUEST);
        } else if (ex instanceof NotFoundError) {
            return new ResponseEntity<>(new ErrorDto("Organization not found"), HttpStatus.NOT_FOUND);
        } else if (ex instanceof DomainError) {
            log.error("Domain error not mapped in controller:", ex);
            return new ResponseEntity<>(new ErrorDto("Sorry, we have troubles fetching repositories. Please, try again later"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            log.error("Non-domain exception not mapped in controller:", ex);
            return new ResponseEntity<>(new ErrorDto("Sorry, we have troubles fetching repositories. Please, try again later"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
