package no.nav.modiapersonoversikt.utils

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.concurrent.timerTask

inline fun Timer.schedule(delay: Duration, period: Duration, crossinline action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    schedule(task, delay.toMillis(), period.toMillis())
    return task
}

val Int.minutes get() = Duration.of(this.toLong(), ChronoUnit.MINUTES)

fun getConfig(name: String, defaultValues: Map<String, String?> = emptyMap()): String? {
    return System.getProperty(name) ?: System.getenv(name) ?: defaultValues[name]
}

fun getRequiredConfig(name: String, defaultValues: Map<String, String?> = emptyMap()): String =
    requireNotNull(getConfig(name, defaultValues)) {
        "Could not find property/env for '$name'"
    }
