package no.nav.modiapersonoversikt

import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.config.DataSourceConfiguration
import org.testcontainers.containers.PostgreSQLContainer

internal class SpecifiedPostgreSQLContainer : PostgreSQLContainer<SpecifiedPostgreSQLContainer>()

fun runLocally(useMock: Boolean) {
    val db = SpecifiedPostgreSQLContainer()
    db.start()

    val configuration = Configuration(jdbcUrl = db.jdbcUrl)
    val dbConfig = DataSourceConfiguration(configuration)
    val applicationState = ApplicationState()

    DataSourceConfiguration.migrateDb(configuration, dbConfig.adminDataSource())

    val applicationServer = createHttpServer(
            applicationState = applicationState,
            port = 7070,
            configuration = Configuration(),
            dataSource = dbConfig.userDataSource(),
            useMock = useMock
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutdown hook called, shutting down gracefully")
        db.stop()
        applicationState.initialized = false
        applicationServer.stop(1000, 1000)
    })

    applicationServer.start(wait = true)
}

fun main() {
    runLocally(false)
}
