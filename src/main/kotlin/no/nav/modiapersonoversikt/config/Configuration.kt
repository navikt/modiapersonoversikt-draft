package no.nav.modiapersonoversikt.config

import no.nav.modiapersonoversikt.AzureAd
import no.nav.modiapersonoversikt.OpenAM
import no.nav.personoversikt.ktor.utils.Security.AuthCookie
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
        jwksUrl = getRequiredConfig("ISSO_JWKS_URL", defaultValues),
        cookies = listOf(
            AuthCookie(name = "modia_ID_token"),
            AuthCookie(name = "ID_token"),
        )
    ),
    val azuread: AuthProviderConfig? = getConfig("AZURE_OPENID_CONFIG_JWKS_URI")?.let {
        AuthProviderConfig(
            name = AzureAd,
            jwksUrl = it
        )
    },
    val database: DatabaseConfig = DatabaseConfig()
)
