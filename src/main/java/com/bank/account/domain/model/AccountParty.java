package com.bank.account.domain.model;

import java.util.regex.Pattern;

public record AccountParty(DocumentType documentType, String documentNumber, String fullName) {

    private static final int MAX_NAME_LENGTH = 150;
    private static final Pattern DNI = Pattern.compile("^\\d{8}$");
    private static final Pattern CEX = Pattern.compile("^[A-Za-z0-9]{9,12}$");
    private static final Pattern PASSPORT = Pattern.compile("^[A-Za-z0-9]{6,12}$");
    private static final Pattern RUC = Pattern.compile("^\\d{11}$");

    public AccountParty {
        if (documentType == null) {
            throw new IllegalArgumentException("Document type must not be null");
        }
        if (documentNumber == null || !formatFor(documentType).matcher(documentNumber).matches()) {
            throw new IllegalArgumentException("Document number does not match the format for " + documentType);
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name must not be blank");
        }
        fullName = fullName.trim();
        if (fullName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Full name must be at most " + MAX_NAME_LENGTH + " characters");
        }
    }

    private static Pattern formatFor(DocumentType type) {
        return switch (type) {
            case DNI -> DNI;
            case CEX -> CEX;
            case PASSPORT -> PASSPORT;
            case RUC -> RUC;
        };
    }
}
