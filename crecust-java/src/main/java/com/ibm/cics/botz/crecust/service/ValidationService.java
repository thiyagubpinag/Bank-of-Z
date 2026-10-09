package com.ibm.cics.botz.crecust.service;

import com.ibm.cics.botz.crecust.exception.CrecustException;
import com.ibm.cics.botz.crecust.model.CrecustCommarea;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates customer input fields from the CRECUST commarea.
 *
 * @see "CRECUST.cbl P010 title EVALUATE (lines 415–451) and DATE-OF-BIRTH-CHECK section"
 */
public class ValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationService.class);

    private static final Set<String> ACCEPTED_TITLES = Set.of(
            "Professor ",
            "Mr        ",
            "Mrs       ",
            "Miss      ",
            "Ms        ",
            "Dr        ",
            "Drs       ",
            "Lord      ",
            "Sir       ",
            "Lady      ",
            "          "
    );

    private static final int MIN_DOB_YEAR = 1601;
    private static final int MAX_CUSTOMER_AGE = 150;
    private static final long LILIAN_EPOCH_OFFSET = 141427L;

    /**
     * Validates the customer title against the 11 accepted titles.
     *
     * <p>Sets {@code commArea.commSuccess='N'} and {@code commArea.commFailCode='T'} on failure.
     *
     * @param commArea the commarea containing {@code commTitle} (input) and
     *                 {@code commSuccess}/{@code commFailCode} (output)
     */
    public void validateTitle(CrecustCommarea commArea) {
        if (commArea == null || commArea.getCommTitle() == null || !ACCEPTED_TITLES.contains(commArea.getCommTitle())) {
            LOGGER.debug("Title validation failed");
            if (commArea != null) {
                commArea.setCommSuccess("N");
                commArea.setCommFailCode(CrecustException.FAIL_CODE_INVALID_TITLE);
            }
        }
    }

    /**
     * Validates the customer date of birth using Java-native date arithmetic.
     *
     * <p>Sets {@code commArea.commSuccess='N'} and {@code commArea.commFailCode} to one of
     * {@code 'O'}/{@code 'Z'}/{@code 'Y'} on failure.
     *
     * @param commarea the commarea containing {@code commDobDay}/{@code commDobMonth}/
     *                 {@code commDobYear} (input) and {@code commSuccess}/{@code commFailCode}
     *                 (output)
     */
    public void validateDateOfBirth(CrecustCommarea commarea) {
        if (commarea == null) {
            return;
        }

        int year;
        int month;
        int day;
        try {
            year = Integer.parseInt(commarea.getCommDobYear() != null ? commarea.getCommDobYear().trim() : "");
            month = Integer.parseInt(commarea.getCommDobMonth() != null ? commarea.getCommDobMonth().trim() : "");
            day = Integer.parseInt(commarea.getCommDobDay() != null ? commarea.getCommDobDay().trim() : "");
        } catch (NumberFormatException e) {
            LOGGER.debug("DOB parsing failed: invalid numeric format");
            commarea.setCommSuccess("N");
            commarea.setCommFailCode(CrecustException.FAIL_CODE_CEEDAYS_FAIL);
            return;
        }

        if (year < MIN_DOB_YEAR) {
            LOGGER.debug("DOB validation failed: year earlier than minimum");
            commarea.setCommSuccess("N");
            commarea.setCommFailCode(CrecustException.FAIL_CODE_DOB_RANGE);
            return;
        }

        LocalDate dob;
        try {
            dob = LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            LOGGER.debug("DOB validation failed: invalid calendar date");
            commarea.setCommSuccess("N");
            commarea.setCommFailCode(CrecustException.FAIL_CODE_CEEDAYS_FAIL);
            return;
        }

        LocalDate today = LocalDate.now();
        if (today.getYear() - year > MAX_CUSTOMER_AGE) {
            LOGGER.debug("DOB validation failed: customer age exceeds maximum");
            commarea.setCommSuccess("N");
            commarea.setCommFailCode(CrecustException.FAIL_CODE_DOB_RANGE);
            return;
        }

        long dobLilian = dob.toEpochDay() + LILIAN_EPOCH_OFFSET;
        long todayLilian = today.toEpochDay() + LILIAN_EPOCH_OFFSET;
        if (dobLilian > todayLilian) {
            LOGGER.debug("DOB validation failed: date is in the future");
            commarea.setCommSuccess("N");
            commarea.setCommFailCode(CrecustException.FAIL_CODE_DOB_FUTURE);
        }
    }
}
