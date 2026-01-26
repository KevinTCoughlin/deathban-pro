package dev.coughlin.deathban.config

import java.time.Duration

interface OffenseConfig {
    val rollingWindowEnabled: Boolean
    val rollingWindowDuration: Duration
    val maxDeathsInWindow: Int
    val offenseResetEnabled: Boolean
    val offenseResetPeriod: Duration
}
