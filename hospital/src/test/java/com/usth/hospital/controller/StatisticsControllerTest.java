package com.usth.hospital.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usth.hospital.config.SecurityConfig;
import com.usth.hospital.dto.response.StatisticsSummaryResponse;
import com.usth.hospital.exception.InvalidStatisticsFilterException;
import com.usth.hospital.dto.response.StatisticsResponse;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.service.StatisticsService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StatisticsController.class)
@Import(SecurityConfig.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticsService statisticsService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = "VIEW_STATISTICS")
    void filter_withPermission_returnsStatistics() throws Exception {
        when(statisticsService.filterPatients(any(), any(), any(), any()))
                .thenReturn(new StatisticsResponse(
                        0,
                        List.of(),
                        new StatisticsSummaryResponse(0, 0, 0, List.of(), List.of(), List.of())
                ));

        mockMvc.perform(get("/api/v1/statistics/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.summary.admissions").value(0));
    }

    @Test
    @WithMockUser(authorities = "VIEW_PATIENT")
    void filter_withoutPermission_returns403Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/patients"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403));

        verify(statisticsService, never()).filterPatients(any(), any(), any(), any());
    }

    @Test
    void filter_anonymous_returns401Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401));
    }

    @Test
    @WithMockUser(authorities = "VIEW_STATISTICS")
    void filter_invalidDateRange_returnsFieldValidationError() throws Exception {
        when(statisticsService.filterPatients(any(), any(), any(), any()))
                .thenThrow(new InvalidStatisticsFilterException("to", "must be on or after from"));

        mockMvc.perform(get("/api/v1/statistics/patients")
                        .param("from", "2026-08-15")
                        .param("to", "2026-08-14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid data"))
                .andExpect(jsonPath("$.errors.to").value("must be on or after from"));
    }
}
