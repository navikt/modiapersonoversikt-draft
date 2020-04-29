package no.nav.modiapersonoversikt

import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.config.DataSourceConfiguration
import no.nav.modiapersonoversikt.infrastructure.HttpServer
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("modiapersonoversikt-draft.Application")

fun main() {
    val configuration = Configuration()
    val dbConfig = DataSourceConfiguration(configuration)

    DataSourceConfiguration.migrateDb(configuration, dbConfig.adminDataSource())

    HttpServer.create("modiapersonoversikt-draft", 7070) {
        draftApp(
                configuration = configuration,
                dataSource = dbConfig.userDataSource(),
                useMock = false
        )
    }.start(wait = true)
}
