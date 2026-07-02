package com.usth.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PatientIdGeneratorTest {

    private final PatientIdGenerator generator = new PatientIdGenerator();

    @Test
    void generate_producesExpectedCheckLetters() {
        assertThat(generator.generate(2026, 1)).isEqualTo("P26MB001");
        assertThat(generator.generate(2026, 8)).isEqualTo("P26MI008");
        assertThat(generator.generate(2026, 50)).isEqualTo("P26NY050");
    }

    @Test
    void generate_alwaysMatchesIdFormat() {
        for (int seq = 1; seq <= 999; seq++) {
            assertThat(generator.generate(2026, seq))
                    .matches("^P[0-9]{2}[A-Z]{2}[0-9]{3}$");
        }
    }

    @Test
    void generate_zeroPadsTheSequence() {
        assertThat(generator.generate(2026, 7)).endsWith("007");
        assertThat(generator.generate(2026, 70)).endsWith("070");
    }

    @Test
    void generate_differentSequencesGiveDifferentCheckLetters() {
        String first = generator.generate(2026, 1).substring(3, 5);
        String second = generator.generate(2026, 2).substring(3, 5);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void extractSequence_returnsTrailingNumber() {
        assertThat(generator.extractSequence("P26MB001")).isEqualTo(1);
        assertThat(generator.extractSequence("P26NY050")).isEqualTo(50);
    }

    @Test
    void extractSequence_isInverseOfGenerate() {
        for (int seq = 1; seq <= 999; seq++) {
            assertThat(generator.extractSequence(generator.generate(2026, seq))).isEqualTo(seq);
        }
    }
}
