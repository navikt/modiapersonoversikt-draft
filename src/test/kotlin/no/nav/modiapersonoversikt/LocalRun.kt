package no.nav.modiapersonoversikt

import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.config.DataSourceConfiguration
import no.nav.modiapersonoversikt.infrastructure.HttpServer
import org.testcontainers.containers.PostgreSQLContainer

class SpecifiedPostgreSQLContainer : PostgreSQLContainer<SpecifiedPostgreSQLContainer>()

fun runLocally(useMock: Boolean) {
    val db = SpecifiedPostgreSQLContainer()
    db.start()

    val configuration = Configuration(jdbcUrl = db.jdbcUrl)
    val dbConfig = DataSourceConfiguration(configuration)

    DataSourceConfiguration.migrateDb(configuration, dbConfig.adminDataSource())

    HttpServer.create("modiapersonoversikt-draft", 7070) {
        draftApp(
                configuration = Configuration(),
                dataSource = dbConfig.userDataSource(),
                useMock = useMock
        )
    }.start(wait = true)
}

fun main() {
    runLocally(false)
}
