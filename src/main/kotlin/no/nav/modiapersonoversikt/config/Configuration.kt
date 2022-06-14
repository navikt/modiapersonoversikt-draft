package no.nav.modiapersonoversikt.config

import no.nav.modiapersonoversikt.infrastructure.Security
import no.nav.modiapersonoversikt.utils.getConfig
import no.nav.modiapersonoversikt.utils.getRequiredConfig

private val defaultValues = mapOf(
    "NAIS_CLUSTER_NAME" to "local",
    "DATABASE_JDBC_URL" to "jdbc:postgresql://localhost:5432/modiapersonoversikt-draft",
    "ISSO_JWKS_URL" to "",
    "ISSO_ISSUER" to "",
    "VAULT_MOUNTPATH" to ""
)

data class AuthProviderConfig(
    val name: String,
    val jwksUrl: String,
    val usesCookies: Boolean = false,
)
data class DatabaseConfig(
    val jdbcUrl: String = getRequiredConfig("DATABASE_JDBC_URL", defaultValues),
    val vaultMountpath: String = getRequiredConfig("VAULT_MOUNTPATH", defaultValues),
)

class Configuration(
    val clusterName: String = getRequiredConfig("NAIS_CLUSTER_NAME", defaultValues),
    val openam: AuthProviderConfig = AuthProviderConfig(
        name = Security.OpenAM,
        jwksUrl = getRequiredConfig("ISSO_JWKS_URL", defaultValues),
        usesCookies = true
    ),
    val azuread: AuthProviderConfig? = getConfig("AZURE_OPENID_CONFIG_JWKS_URI")?.let {
        AuthProviderConfig(
            name = Security.AzureAd,
            jwksUrl = it
        )
    },
    val authproviders: Array<String> = listOfNotNull(openam.name, azuread?.name).toTypedArray(),
    val database: DatabaseConfig = DatabaseConfig()
)
