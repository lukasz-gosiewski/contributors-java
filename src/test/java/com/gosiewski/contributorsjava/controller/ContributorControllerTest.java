package com.gosiewski.contributorsjava.controller;

import com.gosiewski.contributorsjava.error.ApiCallError;
import com.gosiewski.contributorsjava.error.BlankOrganisationNameError;
import com.gosiewski.contributorsjava.error.NotFoundError;
import com.gosiewski.contributorsjava.service.ContributorService;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.inject.Inject;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
public class ContributorControllerTest {

    @MockBean(answer= Answers.RETURNS_SMART_NULLS)
    private ContributorService contributorService;

    @Inject
    private MockMvc mockMvc;

    @Test
    void shouldReturn404WhenNoContributors() throws Exception {
        // given
        final var organizationName = "exampleOrganization";
        final var url = String.format("/org/%1$s/contributors", organizationName);

        // when
        when(contributorService.getContributorsByOrganization(organizationName))
                .thenReturn(Either.left(new NotFoundError()));

        // then
        mockMvc.perform(get(url))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn500WhenInternalError() throws Exception {
        // given
        final var organizationName = "exampleOrganization";
        final var url = String.format("/org/%1$s/contributors", organizationName);

        // when
        when(contributorService.getContributorsByOrganization(organizationName))
                .thenReturn(Either.left(new ApiCallError()));

        // then
        mockMvc.perform(get(url))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldReturn400WhenNameBlank() throws Exception {
        // given
        final var organizationName = "exampleOrganization";
        final var url = String.format("/org/%1$s/contributors", organizationName);

        // when
        when(contributorService.getContributorsByOrganization(organizationName))
                .thenReturn(Either.left(new BlankOrganisationNameError("sample")));

        // then
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest());
    }
}
