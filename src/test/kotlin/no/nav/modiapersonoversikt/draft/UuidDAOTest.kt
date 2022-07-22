package no.nav.modiapersonoversikt.draft

import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import no.nav.modiapersonoversikt.WithDatabase
import no.nav.modiapersonoversikt.utils.transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class UuidDAOTest : WithDatabase {
    val dao = UuidDAOImpl(dataSource())
    val testuuid = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() = runBlocking {
        insert(
            UuidDAO.TableRow(
                owner = "Z999999",
                uuid = testuuid,
                created = LocalDateTime.now()
            )
        )
    }

    @Test
    internal fun `should return null if no owner exists for uuid`() = runBlocking {
        assertNull(dao.getOwner(UUID.randomUUID()))
    }

    @Test
    internal fun `should return owner if uuid exists`() = runBlocking {
        assertEquals("Z999999", dao.getOwner(testuuid))
    }

    @Test
    internal fun `should generate new uuid if no uuid for owner exist`() = runBlocking {
        val uuid = dao.generateUid("Z888888")
        assertNotNull(uuid)
        assertNotEquals(testuuid.toString(), uuid.toString())
    }

    @Test
    internal fun `should generate new uuid if existing uuid is 1 hour old`() = runBlocking {
        insert(
            UuidDAO.TableRow(
                owner = "Z888888",
                uuid = testuuid,
                created = LocalDateTime.now().minusHours(1)
            )
        )
        val uuid = dao.generateUid("Z888888")
        assertNotNull(uuid)
        assertNotEquals(testuuid.toString(), uuid.toString())
    }

    @Test
    internal fun `should return existing uuid if it is under 1 hour old`() = runBlocking {
        insert(
            UuidDAO.TableRow(
                owner = "Z888888",
                uuid = testuuid,
                created = LocalDateTime.now().minusMinutes(58)
            )
        )
        val uuid = dao.generateUid("Z888888")
        assertNotNull(uuid)
        assertEquals(testuuid.toString(), uuid.toString())
    }

    @Test
    internal fun `should return newest existing uuid if it is under 1 hour old`() = runBlocking {
        insert(
            UuidDAO.TableRow(
                owner = "Z888888",
                uuid = UUID.randomUUID(),
                created = LocalDateTime.now().minusMinutes(40)
            )
        )
        insert(
            UuidDAO.TableRow(
                owner = "Z888888",
                uuid = testuuid,
                created = LocalDateTime.now().minusMinutes(30)
            )
        )
        val uuid = dao.generateUid("Z888888")
        assertNotNull(uuid)
        assertEquals(testuuid.toString(), uuid.toString())
    }

    @Test
    internal fun `should delete uuid after 4 hours`() = runBlocking {
        val tablerow = UuidDAO.TableRow(owner = "Z888888", uuid = testuuid, created = LocalDateTime.now())
        insert(tablerow.copy(uuid = UUID.randomUUID(), created = LocalDateTime.now().minusMinutes(120)))
        insert(tablerow.copy(uuid = UUID.randomUUID(), created = LocalDateTime.now().minusMinutes(230)))
        insert(tablerow.copy(uuid = UUID.randomUUID(), created = LocalDateTime.now().minusMinutes(250)))
        insert(tablerow.copy(uuid = UUID.randomUUID(), created = LocalDateTime.now().minusMinutes(360)))

        val deletedRows = dao.deleteExpired()
        assertEquals(2, deletedRows)
    }

    suspend fun insert(row: UuidDAO.TableRow) {
        transactional(dataSource()) { tx ->
            tx.run(
                queryOf("DELETE FROM owneruuid WHERE uuid = ?::uuid", row.uuid).asExecute
            )
            tx.run(
                queryOf(
                    "INSERT INTO owneruuid (owner, uuid, created) VALUES (?, ?, ?)",
                    row.owner,
                    row.uuid,
                    row.created
                ).asUpdate
            )
        }
    }
}