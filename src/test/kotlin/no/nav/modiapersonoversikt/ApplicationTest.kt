package no.nav.modiapersonoversikt

import io.ktor.http.HttpMethod
import io.ktor.server.testing.*
import no.nav.modiapersonoversikt.draft.DraftContext
import no.nav.modiapersonoversikt.draft.DraftDTO
import no.nav.modiapersonoversikt.draft.SaveDraftDTO
import no.nav.modiapersonoversikt.utils.fromJson
import no.nav.modiapersonoversikt.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplicationTest : WithDatabase {
    @Test
    fun `should with empty array if no drafts exists`() {
        withTestApp(connectionUrl()) {
            val response = getDrafts()
            assertEquals(response.status, 200)
            assertEquals(response.data, emptyList<DraftDTO>())
        }
    }

    @Test
    fun `should just return drafts exactly matching the given context by default`() {
        withTestApp(connectionUrl()) {
            saveDraft(SaveDraftDTO("This is some content 1", mapOf("externalId" to "1231")))
            val savedDraft = saveDraft(SaveDraftDTO("This is some content 2", mapOf("externalId" to "1232")))
            saveDraft(SaveDraftDTO("This is some content 3", mapOf("externalId" to "1233")))

            val noContext = getDrafts()
            assertEquals(noContext.status, 200)
            assertEquals(noContext.data, emptyList<DraftDTO>())

            val withContext = getDrafts(context = mapOf("externalId" to "1232"))
            assertEquals(withContext.status, 200)
            assertDraftDTOMatches(savedDraft.data, withContext.data)
        }
    }

    @Test
    fun `should just return drafts partially matching the given context if exact is false`() {
        withTestApp(connectionUrl()) {
            val firstSavedDraft = saveDraft(SaveDraftDTO("This is some content 1", mapOf("externalId" to "1231", "submatch" to "id1")))
            val secondSavedDraft = saveDraft(SaveDraftDTO("This is some content 2", mapOf("externalId" to "1232", "submatch" to "id1")))
            val thirdSavedDraft = saveDraft(SaveDraftDTO("This is some content 3", mapOf("externalId" to "1233", "submatch" to "id1")))
            val responses = listOf(firstSavedDraft.data, secondSavedDraft.data, thirdSavedDraft.data)

            val withExact = getDrafts(exact = true, context = mapOf("submatch" to "id1"))
            assertEquals(withExact.status, 200)
            assertEquals(withExact.data, emptyList<DraftDTO>())

            val noExact = getDrafts(exact = false, context = mapOf("submatch" to "id1"))
            assertEquals(noExact.status, 200)
            assertDraftDTOMatches(responses, noExact.data)
        }
    }

    @Test
    fun `should return all drafts if context is empty and exact is false`() {
        withTestApp(connectionUrl()) {
            val firstSavedDraft = saveDraft(SaveDraftDTO("This is some content", mapOf("externalId" to "1231")))
            val secondSavedDraft = saveDraft(SaveDraftDTO("This is some content", mapOf("externalId" to "1232")))
            val responses = listOf(firstSavedDraft.data, secondSavedDraft.data)

            val getAllNonExact = getDrafts(exact = false)
            assertEquals(getAllNonExact.status, 200)
            assertDraftDTOMatches(responses, getAllNonExact.data)
        }
    }
}

class JsonResponse<T>(
    call: TestApplicationCall,
    val data: T
) {
    val status = call.response.status()?.value ?: -1
}

fun TestApplicationEngine.saveDraft(draft: SaveDraftDTO): JsonResponse<DraftDTO> {
    val call = handleRequest(HttpMethod.Post, "/modiapersonoversikt-draft/api/draft") {
        addHeader("Content-Type", "application/json")
        setBody(draft.toJson())
    }
    val data = call.response.content!!.fromJson<DraftDTO>()

    return JsonResponse(call, data)
}

fun TestApplicationEngine.getDrafts(exact: Boolean? = null, context: DraftContext = emptyMap()): JsonResponse<List<DraftDTO>> {
    val params = context.toMutableMap()
    if (exact != null) {
        params["exact"] = exact.toString()
    }

    val queryParams = params
        .map { entry -> "${entry.key}=${entry.value}" }
        .joinToString("&")
        .let { if (it.isNotEmpty()) "?$it" else "" }

    val call = handleRequest(HttpMethod.Get, "/modiapersonoversikt-draft/api/draft$queryParams")
    val data = call.response.content!!.fromJson<List<DraftDTO>>()

    return JsonResponse(call, data)
}
