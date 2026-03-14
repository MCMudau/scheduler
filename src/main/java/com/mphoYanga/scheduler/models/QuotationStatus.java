package com.mphoYanga.scheduler.models;

public enum QuotationStatus {
    DRAFT("Draft"),
    SENT("Sent to Client"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    EXPIRED("Expired"),
    ARCHIVED("Archived");

    private final String displayName;

    QuotationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
