package no.nav.modiapersonoversikt.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.modiapersonoversikt.log
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class DataSourceConfiguration(private val env: Configuration) {
    private var userDataSource = createDatasource("user")
    private var adminDataSource = createDatasource("admin")

    fun userDataSource() = userDataSource
    fun adminDataSource() = adminDataSource

    private fun createDatasource(user: String): DataSource {
        val config = HikariConfig()
        if (env.clusterName == "dev-gcp" || env.clusterName == "prod-gcp") {
            val database: DatabaseConfigGcp = env.database as DatabaseConfigGcp
            config.jdbcUrl = database.jdbcUrl
            config.minimumIdle = 2
            config.maximumPoolSize = 10
            config.connectionTimeout = 1000
            config.maxLifetime = 30_000
            config.username = database.userName
            config.password = database.password
            return HikariDataSource(config)
        }

        val database: DatabaseConfig = env.database as DatabaseConfig
        val mountPath = database.vaultMountpath
        config.jdbcUrl = database.jdbcUrl
        config.minimumIdle = 0
        config.maximumPoolSize = 4
        config.connectionTimeout = 5000
        config.maxLifetime = 30000
        config.isAutoCommit = false

        log.info("Creating DataSource to: ${database.jdbcUrl}")

        if (env.clusterName == "local") {
            config.username = "test"
            config.password = "test"
            return HikariDataSource(config)
        }

        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            config,
            mountPath,
            dbRole(env.database.dbName, user),
        )
    }

    fun runFlyway() {
        Flyway
            .configure()
            .dataSource(adminDataSource)
            .load()
            .migrate()
    }

    private fun dbRole(dbName: String, user: String): String = "$dbName-$user"
}


