package com.dailyback.shared.domain.health

interface DatabaseHealthChecker {
    fun isHealthy(): Boolean
}
