package no.nav.modiapersonoversikt.draft

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.modiapersonoversikt.draft.UuidDAO.OwnerUUID
import no.nav.modiapersonoversikt.log
import no.nav.modiapersonoversikt.utils.execute
import no.nav.modiapersonoversikt.utils.transactional
import java.util.UUID
import javax.sql.DataSource

private const val table = "owneruuid"

class UuidDAOImpl(private val dataSource: DataSource) : UuidDAO {
    override suspend fun generateUid(owner: String): OwnerUUID {
        return transactional(dataSource) { tx -> generateUid(tx, owner) }
    }

    override suspend fun getOwner(uuid: UUID): OwnerUUID? {
        return transactional(dataSource) { tx -> getByUUID(tx, uuid) }
    }

    override suspend fun deleteExpired(): Int {
        return transactional(dataSource) { tx ->
            log.info("Deleting old uuid's")
            tx
                .run(queryOf("DELETE FROM $table WHERE created < now() - INTERVAL '4 HOUR'").asUpdate)
                .also { log.info("Deleted old uuids: $it") }
        }
    }
}

private fun generateUid(tx: TransactionalSession, owner: String): OwnerUUID {
    val existingUUID = getByOwner(tx, owner)
    return if (existingUUID == null) {
        val uuid = UUID.randomUUID()
        saveOwnerUUID(tx, owner, uuid)
    } else if (existingUUID.shouldBeRefreshed) {
        /**
         * Existing uuid will be removed by scheduled job (removed 4hour old uuid's).
         * But we're not removing it yet in case a browser-tab is open, and using it.
         */
        val uuid = UUID.randomUUID()
        saveOwnerUUID(tx, owner, uuid)
    } else {
        existingUUID
    }
}

private fun getByUUID(tx: TransactionalSession, uuid: UUID): OwnerUUID? {
    return queryOf("SELECT * FROM $table WHERE uuid = ?::uuid", uuid.toString())
        .map(rowMapper)
        .asSingle
        .execute(tx)
}

private fun getByOwner(tx: TransactionalSession, owner: String): OwnerUUID? {
    return queryOf("SELECT * FROM $table WHERE owner = ? ORDER by created DESC", owner)
        .map(rowMapper)
        .asSingle
        .execute(tx)
}

private fun saveOwnerUUID(tx: TransactionalSession, owner: String, uuid: UUID): OwnerUUID {
    return queryOf("INSERT INTO $table (owner, uuid) VALUES (?, ?) RETURNING owner, uuid, created", owner, uuid)
        .map(rowMapper)
        .asSingle
        .execute(tx)!!
}

val rowMapper: (Row) -> OwnerUUID = { row ->
    OwnerUUID(
        owner = row.string("owner"),
        uuid = row.uuid("uuid"),
        created = row.localDateTime("created")
    )
}