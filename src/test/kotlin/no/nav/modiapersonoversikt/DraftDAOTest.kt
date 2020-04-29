package no.nav.modiapersonoversikt

import kotlinx.coroutines.runBlocking
import no.nav.modiapersonoversikt.draft.*
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
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

        assertNull(startResult)
        assertDraftMatches(Draft(owner, content, context, LocalDateTime.now()), saveResult)
        assertDraftMatches(Draft(owner, content, context, LocalDateTime.now()), retrievedResult)
        assertDraftMatches(Draft(owner, updatedContent, context, LocalDateTime.now()), saveResult2)
        assertDraftMatches(Draft(owner, updatedContent, context, LocalDateTime.now()), retrievedResult2)
        assertNull(deletedResult)
    }
}
