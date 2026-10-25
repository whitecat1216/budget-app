package com.example.budget.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExternalActualRecord(
        String externalId,
        Long categoryId,
        Long departmentId,
        Long projectId,
        LocalDate bookedOn,
        BigDecimal amount,
        String description,
        String payloadHash) { }
