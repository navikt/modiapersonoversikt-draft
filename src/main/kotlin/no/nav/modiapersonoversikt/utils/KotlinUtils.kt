package no.nav.modiapersonoversikt.utils

import java.util.*
import kotlin.concurrent.timerTask
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
inline fun Timer.schedule(delay: Duration, period: Duration, crossinline action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    schedule(task, delay.toLongMilliseconds(), period.toLongMilliseconds())
    return task
}
