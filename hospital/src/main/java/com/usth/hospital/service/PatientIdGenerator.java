package com.usth.hospital.service;

import org.springframework.stereotype.Component;

/**
 * Builds the human-readable patient identifier of the form {@code P + YY + LL + NNN},
 * for example {@code P26MB001}.
 *
 * <ul>
 *   <li>{@code YY}  - last two digits of the year the record is created (2026 -&gt; 26).</li>
 *   <li>{@code NNN} - three-digit sequence number, reset every year (001, 002, ...).</li>
 *   <li>{@code LL}  - two check letters derived from {@code YY} and {@code NNN}. They give the
 *       otherwise plain identifier an error-detecting role: a single mistyped digit produces
 *       different check letters, so a wrong identifier can be rejected before any lookup.</li>
 * </ul>
 *
 * The check letters are {@code value = (YYNNN) mod 676} split into two base-26 letters, where
 * 676 = 26 x 26 is the number of distinct letter pairs.
 */
@Component
public class PatientIdGenerator {

    private static final int ALPHABET_SIZE = 26;
    private static final int LETTER_PAIRS = ALPHABET_SIZE * ALPHABET_SIZE;

    public String generate(int year, int sequence) {
        String yy = String.format("%02d", year % 100);
        String nnn = String.format("%03d", sequence);
        int check = Integer.parseInt(yy + nnn) % LETTER_PAIRS;
        char first = (char) ('A' + check / ALPHABET_SIZE);
        char second = (char) ('A' + check % ALPHABET_SIZE);
        return "P" + yy + first + second + nnn;
    }

    public int extractSequence(String id) {
        return Integer.parseInt(id.substring(id.length() - 3));
    }
}
