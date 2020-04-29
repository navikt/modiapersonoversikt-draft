package no.nav.modiapersonoversikt

import io.ktor.application.Application
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.config.DataSourceConfiguration
import no.nav.modiapersonoversikt.draft.Draft
import no.nav.modiapersonoversikt.infrastructure.ApplicationState
import no.nav.modiapersonoversikt.infrastructure.naisApplication
import org.junit.jupiter.api.*
import java.time.LocalDateTime
import java.time.ZoneId
import javax.sql.DataSource
import kotlin.math.abs

interface WithDatabase {
    companion object {
        private val postgreSQLContainer = SpecifiedPostgreSQLContainer().apply { start() }
        private val configuration = Configuration(jdbcUrl = postgreSQLContainer.jdbcUrl)
        private val dbConfig = DataSourceConfiguration(configuration)

        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            DataSourceConfiguration.migrateDb(configuration, dbConfig.adminDataSource())
        }
    }

    @BeforeEach
    fun clearDatabase() {
        dbConfig.adminDataSource().connection.prepareStatement("DELETE FROM draft").execute()
    }

    fun dataSource(): DataSource = dbConfig.userDataSource()
    fun connectionUrl(): String = postgreSQLContainer.jdbcUrl
}

fun <R> withTestApp(test: TestApplicationEngine.() -> R): R {
    val moduleFunction: Application.() -> Unit = {
        naisApplication("modiapersonoversikt-draft", ApplicationState()) {}
    }

    return withTestApplication(moduleFunction, test)
}

fun assertDraftMatches(expected: Draft, actual: Draft?, delta: Int = 100) {
    assertAll(
            "Drafts matches within a timedelta of $delta",
            { Assertions.assertEquals(expected.owner, actual?.owner, "Owner did not match") },
            { Assertions.assertEquals(expected.content, actual?.content, "Owner did not match") },
            { Assertions.assertEquals(expected.context, actual?.context, "Owner did not match") },
            {
                val timeDifference = abs(expected.created.toEpochMilli() - (actual?.created?.toEpochMilli() ?: 0))
                Assertions.assertTrue(timeDifference < delta, "Timedifference was greater than $delta, was: $timeDifference")
            }
    )
}

fun LocalDateTime.toEpochMilli() = this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
