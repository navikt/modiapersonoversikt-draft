package no.nav.modiapersonoversikt.draft

import kotlinx.coroutines.runBlocking
import no.nav.modiapersonoversikt.WithDatabase
import no.nav.modiapersonoversikt.assertDraftMatches
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

open class DraftDAOTest : WithDatabase {
    val owner = "AB1231"
    val content = "This is some content"
    val updatedContent = "This is some updated content"

    @Nested
    inner class DraftGet {
        val dao = DraftDAOImpl(dataSource())
        var now = LocalDateTime.now()
        val context = mapOf("externalId" to "123", "metadata" to "meta")
        val context2 = mapOf("externalId" to "124", "metadata" to "meta2")

        @BeforeEach
        fun setup() {
            runBlocking {
                now = LocalDateTime.now()
                dao.save(SaveDraft(owner, content, context))
                dao.save(SaveDraft(owner, content, context2))
            }
        }

        @Test
        fun `empty context and exact=false should return all drafts`() = runBlocking {
            val drafts = dao.get(DraftIdentificator(owner, emptyMap()), false)

            assertTrue(drafts.size == 2)
            assertDraftMatches(Draft(owner, content, context, now), listOf(drafts[0]))
            assertDraftMatches(Draft(owner, content, context2, now), listOf(drafts[1]))
        }

        @Test
        fun `should returning drafts matching context if exact=false`() = runBlocking {
            val firstDraft = dao.get(DraftIdentificator(owner, mapOf("externalId" to "123")), false)
            val secondDraft = dao.get(DraftIdentificator(owner, mapOf("externalId" to "124")), false)

            assertDraftMatches(Draft(owner, content, context, now), firstDraft)
            assertDraftMatches(Draft(owner, content, context2, now), secondDraft)
        }

        @Test
        fun `should returning only fully-matching drafts if exact=true`() = runBlocking {
            val firstDraft = dao.get(DraftIdentificator(owner, mapOf("externalId" to "123")), true)
            val firstDraftFull = dao.get(DraftIdentificator(owner, context), true)
            val secondDraft = dao.get(DraftIdentificator(owner, mapOf("externalId" to "124")), true)
            val secondDraftFull = dao.get(DraftIdentificator(owner, context2), true)

            assertTrue(firstDraft.isEmpty())
            assertTrue(secondDraft.isEmpty())
            assertDraftMatches(Draft(owner, content, context, now), firstDraftFull)
            assertDraftMatches(Draft(owner, content, context2, now), secondDraftFull)
        }
    }

    @Test
    fun `draft lifecycle verification`() = runBlocking {
        val dao: DraftDAO = DraftDAOImpl(dataSource())
        val context: DraftContext = mapOf("documentId" to "1231456")

        val startResult = dao.get(DraftIdentificator(owner, context))

        val saveResult = dao.save(SaveDraft(owner, content, context))
        val retrievedResult = dao.get(DraftIdentificator(owner, context))

        val saveResult2 = dao.save(SaveDraft(owner, updatedContent, context))
        val retrievedResult2 = dao.get(DraftIdentificator(owner, context))

        dao.delete(DraftIdentificator(owner, context))
        val deletedResult = dao.get(DraftIdentificator(owner, context))

        assertTrue(startResult.isEmpty())
        assertDraftMatches(Draft(owner, content, context, LocalDateTime.now()), listOf(saveResult))
        assertDraftMatches(Draft(owner, content, context, LocalDateTime.now()), retrievedResult)
        assertDraftMatches(Draft(owner, updatedContent, context, LocalDateTime.now()), listOf(saveResult2))
        assertDraftMatches(Draft(owner, updatedContent, context, LocalDateTime.now()), retrievedResult2)
        assertTrue(deletedResult.isEmpty())
    }
}
