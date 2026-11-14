package com.example.clinic.common.infrastructure;

/**
 * Transactional lifecycle event emitted just before {@link DemoDataResetService} commits a reset.
 * Enables verification of atomic database rollback within the same active transaction.
 */
public record DemoDataResetCompletedEvent(int deletedRecordsCount) {
}
