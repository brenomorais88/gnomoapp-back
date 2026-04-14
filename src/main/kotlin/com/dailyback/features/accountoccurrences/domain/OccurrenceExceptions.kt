package com.dailyback.features.accountoccurrences.domain

import java.util.UUID

class OccurrenceNotFoundException(id: UUID) : RuntimeException("Occurrence not found: $id")

class InvalidOccurrenceAmountException : RuntimeException("Occurrence amount must be greater than or equal to zero")
