package no.nav.modiapersonoversikt

import io.ktor.client.plugins.defaultRequest
import io.ktor.server.application.Application
import io.ktor.server.testing.*
import io.ktor.server.util.url
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.config.DataSourceConfiguration
import no.nav.modiapersonoversikt.config.DatabaseConfig
import no.nav.modiapersonoversikt.draft.Draft
import no.nav.modiapersonoversikt.draft.DraftDTO
import no.nav.modiapersonoversikt.draft.toDTO
import no.nav.modiapersonoversikt.utils.transactional
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertAll
import java.time.LocalDateTime
import java.time.ZoneId
import javax.sql.DataSource
import kotlin.math.abs

interface WithDatabase {
    companion object {
        private val postgreSQLContainer = SpecifiedPostgreSQLContainer().apply { start() }
        private val configuration = Configuration(database = DatabaseConfig(jdbcUrl = postgreSQLContainer.jdbcUrl))
        private val dbConfig = DataSourceConfiguration(configuration)

        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            dbConfig.runFlyway()
        }
    }

    @AfterEach
    fun clearDatabase() {
        runBlocking {
            transactional(dbConfig.adminDataSource()) { tx ->
                tx.run(queryOf("DELETE FROM draft").asExecute)
                tx.run(queryOf("DELETE FROM owneruuid").asExecute)
            }
        }
    }

    fun dataSource(): DataSource = dbConfig.userDataSource()
    fun connectionUrl(): String = postgreSQLContainer.jdbcUrl
}

fun <R> withTestApp(jdbcUrl: String? = null, test: suspend ApplicationTestBuilder.() -> R) {
    val dataAwareApp = fun Application.() {
        if (jdbcUrl != null) {
            val config = Configuration(database = DatabaseConfig(jdbcUrl = jdbcUrl))
            val dbConfig = DataSourceConfiguration(config)
            draftApp(config, dbConfig.userDataSource(), true)
        }
    }

    val moduleFunction: Application.() -> Unit = {
        dataAwareApp()
    }
    testApplication {
        application(moduleFunction)
        test()
    }
}

fun assertDraftMatches(expected: Draft, actuals: List<Draft>, delta: Int = 1000) {
    assertDraftDTOMatches(expected.toDTO(), actuals.map { it.toDTO() }, delta)
}

fun assertDraftDTOMatches(expected: DraftDTO, actuals: List<DraftDTO>, delta: Int = 1000) {
    assertDraftDTOMatches(listOf(expected), actuals, delta)
}

fun assertDraftDTOMatches(expecteds: List<DraftDTO>, actuals: List<DraftDTO>, delta: Int = 1000) {
    assertTrue(actuals.isNotEmpty(), "Expected at least one Draft")
    assertTrue(expecteds.size == actuals.size, "Lengt of lists should be equal")

    val assertions = expecteds
        .zip(actuals)
        .flatMap { (expected, actual) ->
            listOf(
                { Assertions.assertEquals(expected.owner, actual.owner, "Owner did not match") },
                { Assertions.assertEquals(expected.content, actual.content, "Owner did not match") },
                { Assertions.assertEquals(expected.context, actual.context, "Owner did not match") },
                {
                    val timeDifference = abs(expected.created.toEpochMilli() - actual.created.toEpochMilli())
                    assertTrue(timeDifference < delta, "Timedifference was greater than $delta, was: $timeDifference")
                }
            )
        }

    assertAll("Drafts matches within a timedelta of $delta", assertions)
}

fun LocalDateTime.toEpochMilli() = this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
