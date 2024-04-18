package no.nav.modiapersonoversikt.config

import io.ktor.http.*
import no.nav.modiapersonoversikt.AzureAd
import no.nav.personoversikt.ktor.utils.Security
import no.nav.personoversikt.ktor.utils.Security.AuthProviderConfig
import no.nav.personoversikt.utils.EnvUtils.getRequiredConfig

private val defaultValues = mapOf(
    "NAIS_CLUSTER_NAME" to "local",
    "DATABASE_JDBC_URL" to "jdbc:postgresql://localhost:5432/modiapersonoversikt-draft",
    "VAULT_MOUNTPATH" to "",
    "AZURE_APP_WELL_KNOWN_URL" to "",
    "DATABASE_NAME" to "modiapersonoversikt-draft-pg15"
)

data class DatabaseConfig(
    val jdbcUrl: String = getRequiredConfig("DATABASE_JDBC_URL", defaultValues),
    val vaultMountpath: String = getRequiredConfig("VAULT_MOUNTPATH", defaultValues),
    val dbName: String = getRequiredConfig("DATABASE_NAME", defaultValues)
)

class DatabaseConfigGcp {
    private val appDB: String = getRequiredConfig("DATABASE_NAME")
    private val appDbString = "NAIS_DATABASE_MODIAPERSONOVERSIKT_DRAFT_MODIAPERSONOVERSIKT_DRAFT_DB"
    private val host = getRequiredConfig("${appDbString}_HOST")
    private val port = getRequiredConfig("${appDbString}_PORT").toInt()
    val jdbcUrl = "jdbc:postgresql://$host:$port/$appDB"
    val userName = getRequiredConfig("${appDbString}_USERNAME")
    val password = getRequiredConfig("${appDbString}_PASSWORD")
}

class Configuration {
    val clusterName: String = getRequiredConfig("NAIS_CLUSTER_NAME", defaultValues)
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
        )
    val database = database()

    private fun database(): Any {
        if(clusterName == "dev-gcp" || clusterName == "prod-gcp"){
            return DatabaseConfigGcp()
        }
        return DatabaseConfig()
    }
}
