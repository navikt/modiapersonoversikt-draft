package no.nav.modiapersonoversikt.draft

import kotlinx.coroutines.runBlocking
import no.nav.modiapersonoversikt.WithDatabase
import no.nav.modiapersonoversikt.assertDraftMatches
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDateTime

class DraftDAOTest : WithDatabase {
    @Test
    fun `draft lifecycle verification`() = runBlocking {
        val dao: DraftDAO = DraftDAOImpl(dataSource())
        val owner = "AB1231"
        val content = "This is some content"
        val updatedContent = "This is some updated content"
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
