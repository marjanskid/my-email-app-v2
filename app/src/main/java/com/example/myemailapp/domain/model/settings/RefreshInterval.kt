package com.example.myemailapp.domain.model.settings

enum class RefreshInterval(val label: String, val millis: Long) {
    MANUAL("Manual only", 0L),
    ONE_MINUTE("1 minute", 60_000L),
    FIVE_MINUTES("5 minutes", 300_000L),
    FIFTEEN_MINUTES("15 minutes", 900_000L),
    THIRTY_MINUTES("30 minutes", 1_800_000L),
    ONE_HOUR("1 hour", 3_600_000L)
}
