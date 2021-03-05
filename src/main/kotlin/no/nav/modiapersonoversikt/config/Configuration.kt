package no.nav.modiapersonoversikt.config

import com.natpryce.konfig.*

private val defaultProperties = ConfigurationMap(
    mapOf(
        "NAIS_CLUSTER_NAME" to "local",
        "DATABASE_JDBC_URL" to "jdbc:postgresql://localhost:5432/modiapersonoversikt-draft",
        "ISSO_JWKS_URL" to "",
        "ISSO_ISSUER" to "",
        "VAULT_MOUNTPATH" to ""
    )
)

data class Configuration(
    val clusterName: String = config()[Key("NAIS_CLUSTER_NAME", stringType)],
    val jwksUrl: String = config()[Key("ISSO_JWKS_URL", stringType)],
    val jwtIssuer: String = config()[Key("ISSO_ISSUER", stringType)],
    val jdbcUrl: String = config()[Key("DATABASE_JDBC_URL", stringType)],
    val vaultMountpath: String = config()[Key("VAULT_MOUNTPATH", stringType)]
)

private fun config() = ConfigurationProperties.systemProperties() overriding
    EnvironmentVariables overriding
    defaultProperties
