package no.nav.modiapersonoversikt

import io.ktor.server.netty.*
import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.config.DataSourceConfiguration
import no.nav.modiapersonoversikt.config.DatabaseConfig
import no.nav.personoversikt.common.ktor.utils.KtorServer
import org.testcontainers.containers.PostgreSQLContainer

class SpecifiedPostgreSQLContainer : PostgreSQLContainer<SpecifiedPostgreSQLContainer>("postgres:14.3-alpine")

fun runLocally(useMock: Boolean) {
    val db = SpecifiedPostgreSQLContainer()
    db.start()

    val configuration = Configuration(
        database = DatabaseConfig(jdbcUrl = db.jdbcUrl)
    )
    val dbConfig = DataSourceConfiguration(configuration)

    dbConfig.runFlyway()

    KtorServer.create(Netty, 7070) {
        draftApp(
            configuration = configuration,
            dataSource = dbConfig.userDataSource(),
            useMock = useMock
        )
    }.start(wait = true)
}

fun main() {
    runLocally(false)
}
