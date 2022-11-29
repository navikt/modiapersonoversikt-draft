package no.nav.modiapersonoversikt.config

import io.ktor.http.*
import no.nav.modiapersonoversikt.AzureAd
import no.nav.personoversikt.ktor.utils.Security
import no.nav.personoversikt.ktor.utils.Security.AuthProviderConfig
import no.nav.personoversikt.utils.EnvUtils.getConfig
import no.nav.personoversikt.utils.EnvUtils.getRequiredConfig

private val defaultValues = mapOf(
    "NAIS_CLUSTER_NAME" to "local",
    "DATABASE_JDBC_URL" to "jdbc:postgresql://localhost:5432/modiapersonoversikt-draft",
    "VAULT_MOUNTPATH" to "",
    "AZURE_APP_WELL_KNOWN_URL" to ""
)

data class DatabaseConfig(
    val jdbcUrl: String = getRequiredConfig("DATABASE_JDBC_URL", defaultValues),
    val vaultMountpath: String = getRequiredConfig("VAULT_MOUNTPATH", defaultValues)
)

class Configuration(
    val clusterName: String = getRequiredConfig("NAIS_CLUSTER_NAME", defaultValues),
    val azuread: AuthProviderConfig =
        AuthProviderConfig(
            name = AzureAd,
            jwksConfig = Security.JwksConfig.OidcWellKnownUrl(getRequiredConfig("AZURE_APP_WELL_KNOWN_URL", defaultValues)),
            tokenLocations = listOf(
                Security.TokenLocation.Header(HttpHeaders.Authorization)
            )
        ),
    val database: DatabaseConfig = DatabaseConfig()
)
