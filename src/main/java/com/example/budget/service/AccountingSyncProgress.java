package com.example.budget.service;

import com.example.budget.model.ExternalActualRecord;

import java.util.List;

@FunctionalInterface
public interface AccountingSyncProgress {
    /** Persist one fully fetched page and its cursor as a single transaction. */
    void recordPage(int fetchedCount, List<ExternalActualRecord> importableRecords, String nextCursor);
}
