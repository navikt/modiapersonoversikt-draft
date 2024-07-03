package no.nav.modiapersonoversikt.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.modiapersonoversikt.log
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class DataSourceConfiguration(private val env: Configuration) {
    private var userDataSource = createDatasource()
    private var adminDataSource = createDatasource()

    fun userDataSource() = userDataSource
    fun adminDataSource() = adminDataSource

    fun runFlyway() {
        Flyway
            .configure()
            .dataSource(adminDataSource)
            .load()
            .migrate()
    }

    private fun createDatasource(): DataSource {
        val config = HikariConfig()
        config.jdbcUrl = env.database.jdbcUrl
        config.minimumIdle = 0
        config.maximumPoolSize = 4
        config.connectionTimeout = 5000
        config.maxLifetime = 30000
        config.isAutoCommit = false

        log.info("Creating DataSource to: ${env.database.jdbcUrl}")

        if (env.clusterName == "local") {
            config.username = "test"
            config.password = "test"
            return HikariDataSource(config)
        }

        return HikariDataSource(config)
    }

    private fun dbRole(dbName: String, user: String): String = "$dbName-$user"
}
