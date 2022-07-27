package no.nav.modiapersonoversikt.config

import io.ktor.http.*
import no.nav.modiapersonoversikt.AzureAd
import no.nav.modiapersonoversikt.OpenAM
import no.nav.personoversikt.ktor.utils.Security
import no.nav.personoversikt.ktor.utils.Security.AuthProviderConfig
import no.nav.personoversikt.utils.EnvUtils.getConfig
import no.nav.personoversikt.utils.EnvUtils.getRequiredConfig

private val defaultValues = mapOf(
    "NAIS_CLUSTER_NAME" to "local",
    "DATABASE_JDBC_URL" to "jdbc:postgresql://localhost:5432/modiapersonoversikt-draft",
    "ISSO_JWKS_URL" to "",
    "ISSO_ISSUER" to "",
    "VAULT_MOUNTPATH" to ""
)

data class DatabaseConfig(
    val jdbcUrl: String = getRequiredConfig("DATABASE_JDBC_URL", defaultValues),
    val vaultMountpath: String = getRequiredConfig("VAULT_MOUNTPATH", defaultValues),
)

class Configuration(
    val clusterName: String = getRequiredConfig("NAIS_CLUSTER_NAME", defaultValues),
    val openam: AuthProviderConfig = AuthProviderConfig(
        name = OpenAM,
        jwksConfig = Security.JwksConfig.JwksUrl(
            getRequiredConfig("ISSO_JWKS_URL", defaultValues),
            getRequiredConfig("ISSO_ISSUER", defaultValues)
        ),
        tokenLocations = listOf(
            Security.TokenLocation.Cookie(name = "modia_ID_token"),
            Security.TokenLocation.Cookie(name = "ID_token"),
        )
    ),
    val azuread: AuthProviderConfig? = getConfig("AZURE_APP_WELL_KNOWN_URL")?.let { jwksurl ->
        AuthProviderConfig(
            name = AzureAd,
            jwksConfig = Security.JwksConfig.OidcWellKnownUrl(jwksurl),
            tokenLocations = listOf(
                Security.TokenLocation.Header(HttpHeaders.Authorization)
            )
        )
    },
    val database: DatabaseConfig = DatabaseConfig()
)
