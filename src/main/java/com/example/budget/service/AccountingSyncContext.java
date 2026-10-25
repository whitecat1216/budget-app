package com.example.budget.service;

public record AccountingSyncContext(
        long runId,
        long tenantId,
        long connectionId,
        String provider,
        String cursor) { }
