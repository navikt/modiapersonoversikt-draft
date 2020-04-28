package no.nav.modiapersonoversikt

import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.config.DataSourceConfiguration
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("modiapersonoversikt-draft.Application")
data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

fun main() {
    val configuration = Configuration()
    val dbConfig = DataSourceConfiguration(configuration)
    val applicationState = ApplicationState()

    DataSourceConfiguration.migrateDb(configuration, dbConfig.adminDataSource())

    val applicationServer = createHttpServer(
            applicationState = applicationState,
            configuration = configuration,
            dataSource = dbConfig.userDataSource(),
            useMock = false
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutdown hook called, shutting down gracefully")
        applicationState.initialized = false
        applicationServer.stop(5000, 5000)
    })

    applicationServer.start(wait = true)
}
