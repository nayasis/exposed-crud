package com.dshatz.exposed_crud.typed

import org.jetbrains.exposed.v1.core.ColumnSet
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.mapLazy
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.IdGenerator
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import java.util.UUID
import java.sql.SQLException

data class CrudRepository<T, ID : Any, E : Any>(val table: T, val related: List<ColumnSet> = emptyList()) where T: IdTable<ID>, T: IEntityTable<E, ID> {

    private val autoGenerate = runCatching {
        table::class.memberProperties.find { it.name == "autoGenerate" }?.call(table) as Boolean
    }.getOrElse { false }

    val idGenerator = runCatching {
        val generatorClass = table::class.memberProperties.find { it.name == "idGenerator" }?.call(table) as? KClass<*>
        @Suppress("UNCHECKED_CAST")
        generatorClass?.createInstance() as? IdGenerator<E>
    }.getOrNull()

    private fun selectWithJoins(): Query {
        return if (related.isEmpty()) {
            table.selectAll()
        } else {
            var source = table.leftJoin(related.first())
            related.drop(1).forEach {
                source = source.leftJoin(it)
            }
            source.selectAll()
        }
    }

    /**
     * Lazy-select all entities.
     *
     * If this repo has related entities defined using [withRelated], corresponding Reference properties will be populated.
     */
    fun selectAllLazy(): SizedIterable<E> {
        return selectWithJoins().mapLazy(::toEntity)
    }

    /**
     * Select all entities. Use with caution.
     *
     * If this repo has related entities defined using [withRelated], corresponding Reference properties will be populated.
     */
    fun selectAll(): List<E> {
        return selectWithJoins().map(::toEntity)
    }

    /**
     * Insert a new entity into the database.
     *
     * @return inserted entity
     */
    fun create(data: E): E {
        return when {
            autoGenerate && idGenerator != null -> {

                @Suppress("UNCHECKED_CAST")
                val id = runCatching { idGenerator!!.generate(data) }.onFailure { e ->
                    throw IllegalStateException("Cannot generate id", e)
                }.getOrThrow() as ID

                table.setId(data, id)
                table.insert {
                    table.write(it, data)
                }
                data
            }
            autoGenerate && idGenerator == null -> {
                // For IdTable with autoGenerate=true and no custom generator, try insertAndGetId
                // This handles custom IdTable<Int>, IdTable<Long>, etc. with auto-increment columns
                runCatching {
                    val id = table.insertAndGetId {
                        table.writeExceptAutoIncrementing(it, data)
                    }.value
                    table.setId(data, id)
                    data
                }.getOrElse {
                    // Fallback: insert and retrieve by PK
                    table.insert {
                        table.writeExceptAutoIncrementing(it, data)
                    }
                    val pk = table.makePK(data).value
                    findById(pk) ?: error("failed to retrieve inserted entity with PK: $pk")
                }
            }
            else -> {
                table.insert {
                    table.writeExceptAutoIncrementing(it, data)
                }
                val pk = table.makePK(data).value
                findById(pk) ?: error("failed to retrieve inserted entity with PK: $pk")
            }
        }
    }

    /**
     * Updates rows of a table matching the given condition.
     *
     * @param where Condition that determines which rows to update.
     * @param body Lambda that sets the values to update. Receives the table as receiver and UpdateStatement as parameter.
     * @return Number of updated rows.
     */
    fun update(where: () -> Op<Boolean>, body: T.(UpdateStatement) -> Unit): Int {
        return table.update(where, null, body)
    }

    /**
     * Updates rows of a table with an optional limit.
     *
     * @param body Lambda that sets the values to update. Receives the table as receiver and UpdateStatement as parameter.
     * @param limit Maximum number of rows to update. If null, all matching rows are updated.
     * @return Number of updated rows.
     */
    fun update(body: T.(UpdateStatement) -> Unit, limit: Int? = null): Int {
        return table.update(limit, body)
    }

    /**
     * Perform an `UPDATE` based on the primary key.
     * @return updated entity (the same object passed in, with timestamp fields updated).
     */
    fun update(data: E): E {
        with (table) {
            update({
                table.id eq table.makePK(data)
            }) {
                table.write(it, data)
            }
        }
        return data
    }

    /**
     * Save an entity. If the ID is empty, creates a new entity. Otherwise, updates the existing entity.
     * @param data Entity to save
     * @return saved entity
     */
    fun save(data: E): E {
        val id = table.makePK(data).value
        return if (id is CompositeID) {
            runCatching { create(data) }.getOrElse { error ->
                if (isDuplicateKeyException(error)) {
                    update(data)
                } else {
                    throw error
                }
            }
        } else if (isIdEmpty(id)) {
            create(data)
        } else {
            update(data)
        }
    }

    /**
     * save entities.
     *
     * @param items entities to save
     */
    fun saveAll(items: Collection<E>) {
        items.forEach { save(it) }
    }

    private fun isIdEmpty(id: ID): Boolean {
        return when (id) {
            is Int    -> id <= 0
            is Long   -> id <= 0L
            is UInt   -> id <= 0u
            is ULong  -> id <= 0uL
            is Number -> id.toLong() <= 0L
            is String -> id.isEmpty()
            is UUID   -> id == Id.UUID_EMPTY
            else      -> false
        }
    }

    private fun isDuplicateKeyException(error: Throwable): Boolean {
        val sqlException = generateSequence(error) { it.cause }
            .filterIsInstance<SQLException>()
            .firstOrNull()
        val sqlState = sqlException?.sqlState
        if (sqlState == "23505" || sqlState == "23000") {
            return true
        }
        val exposedError = generateSequence(error) { it.cause }
            .filterIsInstance<ExposedSQLException>()
            .firstOrNull()
        return exposedError?.message
            ?.contains("duplicate", ignoreCase = true)
            ?: false
    }

    /**
     * Start a `SELECT`.
     */
    fun select(): TypedSelect<T, E, ID> {
        return TypedSelect(table, selectWithJoins())
    }

    /**
     * Creates a new CrudRepository that will include given related entities in queries.
     *
     * When doing a select, corresponding @References and @BackReferences annotated columns will be populated if present.
     * Example:
     *
     * ```kotlin
     * data class Movie(
     *  val title: String,
     *
     *  @ForeignKey(Director::class)
     *  val directorId: Long,
     *
     *  @References(Director::class, "directorId")
     *  val director: Director? = null
     * )
     * ```
     * Assuming we have inserted one director and one movie having `Movie.directorId == Director.id`,
     *
     * ```kotlin
     * val movie = Movie::class.repo.withRelated(Director::class).selectAll().first()
     * assertNotNull(movie?.director)
     * assertIs<Director>(movie?.director)
     * // Movie.director is of type Director.
     * ```
     *
     */
    fun withRelated(table: ColumnSet) = copy(related = related + table)

    fun withRelated(vararg tables: ColumnSet) = copy(related = related + tables)

    fun findById(id: ID): E? {
        val eid = EntityID(id, table)
        return selectWithJoins().where({
            table.id eq eid
        }).limit(1).firstOrNull()?.let(::toEntity)
    }

    /**
     * Check if an entity exists by its primary key.
     *
     * @param id Primary key value
     * @return true if entity exists, false otherwise
     */
    fun existsById(id: ID): Boolean {
        val eid = EntityID(id, table)
        return selectWithJoins().where({
            table.id eq eid
        }).limit(1).any()
    }

    private fun toEntity(resultRow: ResultRow): E {
        return table.toEntity(resultRow, related)
    }

    fun findOne(where: () -> Op<Boolean>): E? {
        return select().where(where).limit(1).firstOrNull()
    }

    /**
     * Deletes given entity by primary key.
     */
    fun delete(data: E, limit: Int? = null): Int {
        return with (table) {
            deleteWhere(limit = limit) {
                table.id eq table.makePK(data)
            }
        }
    }
}