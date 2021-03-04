package no.nav.modiapersonoversikt.draft

import kotliquery.TransactionalSession
import kotliquery.action.ResultQueryActionBuilder
import kotliquery.queryOf
import no.nav.modiapersonoversikt.log
import no.nav.modiapersonoversikt.utils.execute
import no.nav.modiapersonoversikt.utils.fromJson
import no.nav.modiapersonoversikt.utils.toJson
import no.nav.modiapersonoversikt.utils.transactional
import javax.sql.DataSource

private const val table = "draft"

class DraftDAOImpl(private val dataSource: DataSource) : DraftDAO {
    override suspend fun save(data: SaveDraft): Draft {
        return transactional(dataSource) { tx ->
            delete(tx, data.toDraftIdentificator())
            save(tx, data)
            get(tx, data.toDraftIdentificator(), true).first()
        }
    }

    override suspend fun get(data: DraftIdentificator, exact: Boolean): List<Draft> {
        return transactional(dataSource) { tx -> get(tx, data, exact) }
    }

    override suspend fun delete(data: DraftIdentificator) {
        return transactional(dataSource) { tx -> delete(tx, data) }
    }

    override suspend fun deleteOldDrafts() {
        return transactional(dataSource) { tx ->
            log.info("Deleting old drafts")
            val deletedLines = tx.run(queryOf("DELETE FROM $table WHERE created < now() - INTERVAL '4 HOUR'").asUpdate)
            log.info("Deleted old drafts: $deletedLines")
        }
    }
}

private fun get(tx: TransactionalSession, data: DraftIdentificator, exact: Boolean): List<Draft> {
    return getQuery(data, JSONBOperator.fromBoolean(exact))
        .asList
        .execute(tx)
}

private fun getQuery(data: DraftIdentificator, operator: JSONBOperator): ResultQueryActionBuilder<Draft> {
    return queryOf("SELECT * FROM $table WHERE owner = ? AND context ${operator.sql} ?::jsonb", data.owner, data.context.toJson())
        .map { row ->
            Draft(
                row.string("owner"),
                row.string("content"),
                row.string("context").fromJson(),
                row.localDateTime("created")
            )
        }
}

private fun save(tx: TransactionalSession, data: SaveDraft): Int {
    return queryOf("INSERT INTO $table (owner, content, context) VALUES (?, ?, ?::jsonb)", data.owner, data.content, data.context.toJson())
        .asUpdate
        .execute(tx)
}

private fun delete(tx: TransactionalSession, data: DraftIdentificator): Int {
    return queryOf("DELETE FROM $table WHERE owner = ? AND context = ?::jsonb", data.owner, data.context.toJson())
        .asUpdate
        .execute(tx)
}

private enum class JSONBOperator(val sql: String) {
    EQUALS("="), MATCHES("@>");

    companion object {
        fun fromBoolean(exact: Boolean): JSONBOperator = if (exact) EQUALS else MATCHES
    }
}
