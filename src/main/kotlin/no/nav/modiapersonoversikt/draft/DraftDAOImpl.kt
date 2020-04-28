package no.nav.modiapersonoversikt.draft

import kotliquery.TransactionalSession
import kotliquery.action.ResultQueryActionBuilder
import kotliquery.queryOf
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
            get(tx, data.toDraftIdentificator())!!
        }
    }

    override suspend fun get(data: DraftIdentificator): Draft? {
        return transactional(dataSource) { tx -> get(tx, data) }
    }

    override suspend fun delete(data: DraftIdentificator) {
        return transactional(dataSource) { tx -> delete(tx, data) }
    }

}

private fun get(tx: TransactionalSession, data: DraftIdentificator): Draft? {
    return getQuery(data)
            .asSingle
            .execute(tx)
}

private fun getQuery(data: DraftIdentificator): ResultQueryActionBuilder<Draft> {
    return queryOf("SELECT * FROM $table WHERE owner = ? AND context = ?::jsonb", data.owner, data.context.toJson())
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
