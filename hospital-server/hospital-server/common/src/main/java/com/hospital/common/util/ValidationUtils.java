package com.hospital.common.util;

import com.hospital.common.exception.InvalidArgumentException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{8,15}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtils() {}

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidArgumentException(fieldName + " est obligatoire");
        }
    }

    public static void requirePositive(double value, String fieldName) {
        if (value <= 0) {
            throw new InvalidArgumentException(fieldName + " doit être supérieur à zéro");
        }
    }

    public static void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new InvalidArgumentException(fieldName + " doit être supérieur à zéro");
        }
    }

    public static LocalDate parseDate(String dateStr, String fieldName) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new InvalidArgumentException(fieldName + " est obligatoire");
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new InvalidArgumentException(fieldName + " doit être au format yyyy-MM-dd");
        }
    }

    public static void validatePhone(String phone) {
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new InvalidArgumentException("Numéro de téléphone invalide");
        }
    }

    public static void validateEmail(String email) {
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidArgumentException("Adresse email invalide");
        }
    }

    public static void validateSexe(String sexe) {
        if (sexe == null || (!sexe.equalsIgnoreCase("M") && !sexe.equalsIgnoreCase("F")
                && !sexe.equalsIgnoreCase("MASCULIN") && !sexe.equalsIgnoreCase("FEMININ"))) {
            throw new InvalidArgumentException("Sexe doit être M/F ou MASCULIN/FEMININ");
        }
    }
}
