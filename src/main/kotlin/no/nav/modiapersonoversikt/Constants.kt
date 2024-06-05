package no.nav.modiapersonoversikt

import no.nav.personoversikt.common.utils.EnvUtils

const val appName = "modiapersonoversikt-draft"
const val AzureAd = "azuread"

val appImage = EnvUtils.getConfig("NAIS_APP_IMAGE") ?: "N/A"
