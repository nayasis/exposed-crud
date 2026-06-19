package com.dshatz.exposed_crud.typed

import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.util.logging.Logger

data class TypedSelect<T, E, ID: Any>(
    val table: T,
    private val query: Query,
    private val related: List<ColumnSet> = emptyList(),
): Iterable<E> where T: IdTable<ID>, T: IEntityTable<E, ID> {

    fun where(predicate: () -> Op<Boolean>) = copy(query = query.where(predicate))
    fun where(predicate: Op<Boolean>) = copy(query = query.where(predicate))
    fun orWhere(orPart: () -> Op<Boolean>) = copy(query = query.orWhere(orPart))
    fun andWhere(andPart: () -> Op<Boolean>) = copy(query = query.andWhere(andPart))

    fun limit(limit: Int) = copy(query = query.limit(limit))

    fun orderBy(column: Column<*>, order: SortOrder = SortOrder.ASC) = copy(query = query.orderBy(column, order))

    fun orderBy(vararg order: Pair<Column<*>, SortOrder>) = copy(query = query.orderBy(*order))

    fun withDistinctOn(vararg columns: Column<*>) = copy(query = query.withDistinctOn(columns = columns))
    fun withDistinctOn(vararg columns: Pair<Column<*>, SortOrder>) = copy(query = query.withDistinctOn(columns = columns))
    fun withDistinctOn(value: Boolean = true) = copy(query = query.withDistinct(value))

    override fun iterator(): Iterator<E> {
        val tracedQuery = query.copy()
        if(tracedQuery.fetchSize == null) {
            tracedQuery.fetchSize(defaultFetchSize())
        }
        return TimedIterator(table, tracedQuery, related)
    }

    private class TimedIterator<T, E, ID: Any>(
        private val table: T,
        private val query: Query,
        private val related: List<ColumnSet>,
    ): Iterator<E> where T: IdTable<ID>, T: IEntityTable<E, ID> {

        private val slowRowMs = System.getProperty(SLOW_ROW_MS_PROPERTY)?.toLongOrNull() ?: DEFAULT_SLOW_ROW_MS
        private val traceRows = System.getProperty(TRACE_ROWS_PROPERTY).toBoolean()
        private val rowIterator = openIterator()
        private var offset = 0L
        private var pendingAdvanceElapsedMs = 0L
        private var lastId: ID? = null

        override fun hasNext(): Boolean {
            val startedAt = System.nanoTime()
            return try {
                val hasNext = rowIterator.hasNext()
                val elapsedMs = elapsedMs(startedAt)
                if(hasNext) {
                    pendingAdvanceElapsedMs += elapsedMs
                } else if(elapsedMs >= slowRowMs) {
                    LOGGER.warning(
                        "slow typed select end: table=${table.tableName}, rows=$offset, " +
                            "lastId=$lastId, advance=${elapsedMs}ms",
                    )
                }
                hasNext
            } catch(e: Throwable) {
                logFailure("advance", startedAt, e)
                throw e
            }
        }

        override fun next(): E {
            val rowOffset = offset
            val readStartedAt = System.nanoTime()
            val row = try {
                rowIterator.next()
            } catch(e: Throwable) {
                logFailure("read", readStartedAt, e)
                throw e
            }
            val readElapsedMs = pendingAdvanceElapsedMs + elapsedMs(readStartedAt)
            pendingAdvanceElapsedMs = 0L
            val id = row[table.id].value
            lastId = id

            val mapStartedAt = System.nanoTime()
            val entity = table.toEntity(row, related)
            val mapElapsedMs = elapsedMs(mapStartedAt)
            if(traceRows || readElapsedMs >= slowRowMs || mapElapsedMs >= slowRowMs) {
                logRow(id, rowOffset, readElapsedMs, mapElapsedMs)
            }
            offset++
            return entity
        }

        private fun openIterator(): Iterator<ResultRow> {
            val startedAt = System.nanoTime()
            return try {
                query.iterator().also {
                    val elapsedMs = elapsedMs(startedAt)
                    if(elapsedMs >= slowRowMs) {
                        LOGGER.warning(
                            "slow typed select open: table=${table.tableName}, " +
                                "fetchSize=${query.fetchSize}, elapsed=${elapsedMs}ms",
                        )
                    }
                }
            } catch(e: Throwable) {
                logFailure("open", startedAt, e)
                throw e
            }
        }

        private fun logRow(id: ID, rowOffset: Long, readElapsedMs: Long, mapElapsedMs: Long) {
            LOGGER.warning(
                "slow typed select row: table=${table.tableName}, offset=$rowOffset, id=$id, " +
                    "fetchSize=${query.fetchSize}, rowRead=${readElapsedMs}ms, rowMap=${mapElapsedMs}ms",
            )
        }

        private fun logFailure(phase: String, startedAt: Long, error: Throwable) {
            LOGGER.warning(
                "failed typed select $phase: table=${table.tableName}, offset=$offset, lastId=$lastId, " +
                    "fetchSize=${query.fetchSize}, elapsed=${elapsedMs(startedAt)}ms, error=${error.message}",
            )
        }
    }

    companion object {
        private const val FETCH_SIZE_PROPERTY = "exposedCrud.fetchSize"
        private const val SLOW_ROW_MS_PROPERTY = "exposedCrud.slowRowMs"
        private const val TRACE_ROWS_PROPERTY = "exposedCrud.traceRows"
        private const val DEFAULT_FETCH_SIZE = 1
        private const val DEFAULT_SLOW_ROW_MS = 1_000L
        private val LOGGER = Logger.getLogger(TypedSelect::class.java.name)

        private fun defaultFetchSize(): Int {
            return System.getProperty(FETCH_SIZE_PROPERTY)?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_FETCH_SIZE
        }

        private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000
    }

}
