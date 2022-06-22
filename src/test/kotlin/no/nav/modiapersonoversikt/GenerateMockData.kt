package no.nav.modiapersonoversikt

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.config.DataSourceConfiguration
import no.nav.modiapersonoversikt.config.DatabaseConfig
import no.nav.modiapersonoversikt.draft.DraftDAOImpl
import no.nav.modiapersonoversikt.draft.SaveDraft
import java.util.*

const val NOF_OWNERS = 500
const val NOF_CONTEXT = 30

fun main() {
    val jdbcUrl = "jdbc:postgresql://localhost:32834/test"
    val configuration = Configuration(database = DatabaseConfig(jdbcUrl = jdbcUrl))
    val dbConfig = DataSourceConfiguration(configuration)
    val dao = DraftDAOImpl(dbConfig.userDataSource())

    runBlocking {
        repeat(NOF_OWNERS) { userID ->
            val owner = "Z998${userID.toString().padStart(3, '0')}"
            async {
                println("Starting insertion for $owner")
                repeat(NOF_CONTEXT) {
                    val context = mutableMapOf("contextId" to it.toString(), "rnd" to UUID.randomUUID().toString())
                    if (it % 3 == 0) {
                        context["tripple"] = (it * 3).toString()
                    }

                    dao.save(SaveDraft(owner, UUID.randomUUID().toString(), context))
                }
                println("Completed insertion for $owner")
            }
        }
    }
}
