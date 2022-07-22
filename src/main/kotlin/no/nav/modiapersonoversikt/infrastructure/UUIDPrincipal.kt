package no.nav.modiapersonoversikt.infrastructure

import io.ktor.server.auth.*
import java.util.*

data class UUIDPrincipal(val uuid: UUID) : Principal