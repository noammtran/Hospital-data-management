package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.usth.hospital.dto.response.StatisticsResponse;
import com.usth.hospital.exception.InvalidStatisticsFilterException;
import com.usth.hospital.repository.StatisticsRepository;
import com.usth.hospital.repository.StatisticsRepository.PatientRow;
import com.usth.hospital.service.impl.StatisticsServiceImpl;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private StatisticsRepository statisticsRepository;

    @InjectMocks
    private StatisticsServiceImpl service;

    @Test
    void filterPatients_buildsClinicalAndDemographicSummary() {
        PatientRow admitted = patient("P26AA001", "Patient One", 1, 2, 70, 2, 3, 4);
        PatientRow discharged = patient("P26AA002", "Patient Two", 2, 3, 35, 1, 2, 1);
        PatientRow incomplete = patient("P26AA003", "Patient Three", null, 0, null, 0, 0, 0);
        when(statisticsRepository.filterPatients(null, null, null, null))
                .thenReturn(List.of(admitted, discharged, incomplete));

        StatisticsResponse result = service.filterPatients(null, null, "  ", null);

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.summary().admissions()).isEqualTo(3);
        assertThat(result.summary().diagnoses()).isEqualTo(5);
        assertThat(result.summary().prescriptions()).isEqualTo(5);
        assertThat(result.summary().patientStatuses())
                .extracting("key", "count")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("waiting", 1L),
                        org.assertj.core.groups.Tuple.tuple("examining", 0L),
                        org.assertj.core.groups.Tuple.tuple("admitted", 1L),
                        org.assertj.core.groups.Tuple.tuple("discharged", 1L)
                );
        assertThat(result.summary().genders())
                .extracting("key", "count")
                .contains(
                        org.assertj.core.groups.Tuple.tuple("male", 1L),
                        org.assertj.core.groups.Tuple.tuple("female", 1L),
                        org.assertj.core.groups.Tuple.tuple("not-recorded", 1L)
                );
        assertThat(result.summary().ageGroups())
                .extracting("key", "count")
                .contains(
                        org.assertj.core.groups.Tuple.tuple("18-39", 1L),
                        org.assertj.core.groups.Tuple.tuple("60-plus", 1L),
                        org.assertj.core.groups.Tuple.tuple("not-recorded", 1L)
                );
        assertThat(result.patients()).extracting("id", "age", "admissionCount")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("P26AA001", 70, 2L),
                        org.assertj.core.groups.Tuple.tuple("P26AA002", 35, 1L),
                        org.assertj.core.groups.Tuple.tuple("P26AA003", null, 0L)
                );
        verify(statisticsRepository).filterPatients(null, null, null, null);
    }

    @Test
    void filterPatients_fromWithoutTo_rejectsBeforeQuerying() {
        LocalDate from = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> service.filterPatients(from, null, null, null))
                .isInstanceOf(InvalidStatisticsFilterException.class)
                .hasMessage("is required when from is provided");
        verifyNoInteractions(statisticsRepository);
    }

    @Test
    void filterPatients_reversedRange_rejectsBeforeQuerying() {
        LocalDate from = LocalDate.of(2026, 8, 15);
        LocalDate to = LocalDate.of(2026, 8, 14);

        assertThatThrownBy(() -> service.filterPatients(from, to, null, null))
                .isInstanceOf(InvalidStatisticsFilterException.class)
                .hasMessage("must be on or after from");
        verifyNoInteractions(statisticsRepository);
    }

    private PatientRow patient(
            String id,
            String name,
            Integer gender,
            int status,
            Integer age,
            long admissions,
            long diagnoses,
            long prescriptions
    ) {
        PatientRow row = mock(PatientRow.class);
        when(row.getId()).thenReturn(id);
        when(row.getName()).thenReturn(name);
        when(row.getGender()).thenReturn(gender);
        when(row.getStatus()).thenReturn(status);
        when(row.getBirthDate()).thenReturn(age == null ? null : LocalDate.now().minusYears(age));
        when(row.getAdmissionCount()).thenReturn(admissions);
        when(row.getDiagnosisCount()).thenReturn(diagnoses);
        when(row.getPrescriptionCount()).thenReturn(prescriptions);
        return row;
    }
}
