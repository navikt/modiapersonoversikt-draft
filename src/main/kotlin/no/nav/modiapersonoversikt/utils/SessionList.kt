package no.nav.modiapersonoversikt.utils

import io.ktor.websocket.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.nav.personoversikt.common.ktor.utils.Metrics

class SessionList {
    private val lock = Semaphore(permits = 1)
    private val list = mutableListOf<WebSocketSession>()
    init {
        Metrics.Registry.gaugeCollectionSize("ws-sessions", emptyList(), list)
    }

    suspend fun <T> track(session: WebSocketSession, block: suspend () -> T): T {
        lock.withPermit { list.add(session) }
        val res = block()
        lock.withPermit { list.remove(session) }
        return res
    }

    suspend fun closeAll(reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, "")) {
        lock.withPermit {
            list.forEach { session ->
                session.close(reason)
            }
            list.clear()
        }
    }
}