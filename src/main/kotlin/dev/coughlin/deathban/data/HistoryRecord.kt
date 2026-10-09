package dev.coughlin.deathban.data

import java.time.Instant

data class HistoryRecord(
    val timestamp: Instant,
    val action: String,
    val ban: BanRecord? = null,
    val actor: String? = null,
)
