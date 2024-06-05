package no.nav.modiapersonoversikt.config

import io.ktor.http.*
import no.nav.modiapersonoversikt.AzureAd
import no.nav.personoversikt.common.ktor.utils.Security
import no.nav.personoversikt.common.ktor.utils.Security.AuthProviderConfig
import no.nav.personoversikt.common.utils.EnvUtils.getRequiredConfig

private val defaultValues = mapOf(
    "NAIS_CLUSTER_NAME" to "local",
    "DATABASE_JDBC_URL" to "jdbc:postgresql://localhost:5432/modiapersonoversikt-draft",
    "VAULT_MOUNTPATH" to "",
    "AZURE_APP_WELL_KNOWN_URL" to "",
    "DATABASE_NAME" to "modiapersonoversikt-draft-pg15"
)

data class DatabaseConfig(
    val dbName: String = getRequiredConfig("DATABASE_NAME", defaultValues),
    val jdbcUrl: String,
    val vaultMountpath: String? = null,
)

class Configuration(
    val clusterName: String = getRequiredConfig("NAIS_CLUSTER_NAME", defaultValues),
    val appContextpath: String = if (listOf("prod-gcp", "dev-gcp").contains(clusterName)) "" else "modiapersonoversikt-draft" ,
    val azuread: AuthProviderConfig =
        AuthProviderConfig(
            name = AzureAd,
            jwksConfig = Security.JwksConfig.OidcWellKnownUrl(
                getRequiredConfig(
                    "AZURE_APP_WELL_KNOWN_URL",
                    defaultValues
                )
            ),
            tokenLocations = listOf(
                Security.TokenLocation.Header(HttpHeaders.Authorization)
            )
        ),
    val database: DatabaseConfig = if (clusterName == "dev-gcp" || clusterName == "prod-gcp") {
        DatabaseConfig(
            jdbcUrl = getRequiredConfig(
                "NAIS_DATABASE_MODIAPERSONOVERSIKT_DRAFT_MODIAPERSONOVERSIKT_DRAFT_DB_JDBC_URL",
                defaultValues
            ),
        )

    } else {
        DatabaseConfig(
            jdbcUrl = getRequiredConfig("DATABASE_JDBC_URL", defaultValues),
            vaultMountpath = getRequiredConfig("VAULT_MOUNTPATH", defaultValues),
        )
    }
)
