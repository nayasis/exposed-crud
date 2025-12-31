package com.dshatz.exposed_crud.typed

import org.jetbrains.exposed.v1.core.ColumnSet
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable
import org.jetbrains.exposed.v1.core.dao.id.ULongIdTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
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
import com.dshatz.exposed_crud.IdGenerator
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties

data class CrudRepository<T, ID : Any, E : Any>(val table: T, val related: List<ColumnSet> = emptyList()) where T: IdTable<ID>, T: IEntityTable<E, ID> {

    private val isAutoIncrementingTable =
        table is IntIdTable   ||
        table is UIntIdTable  ||
        table is LongIdTable  ||
        table is ULongIdTable ||
        table is UUIDTable

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
            autoGenerate && isAutoIncrementingTable -> {
                val id = table.insertAndGetId {
                    table.writeExceptAutoIncrementing(it, data)
                }.value
                table.setId(data, id)
                data
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