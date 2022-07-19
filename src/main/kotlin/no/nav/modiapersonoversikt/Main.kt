package no.nav.modiapersonoversikt

import io.ktor.server.netty.*
import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.config.DataSourceConfiguration
import no.nav.personoversikt.ktor.utils.KtorServer
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("modiapersonoversikt-draft.Application")

fun main() {
    val configuration = Configuration()
    val dbConfig = DataSourceConfiguration(configuration)

    DataSourceConfiguration.migrateDb(configuration, dbConfig.adminDataSource())

    KtorServer.create(Netty, 7070) {
        draftApp(
            configuration = configuration,
            dataSource = dbConfig.userDataSource(),
            useMock = false
        )
    }.start(wait = true)
}
