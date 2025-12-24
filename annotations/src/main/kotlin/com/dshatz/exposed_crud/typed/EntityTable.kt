package com.dshatz.exposed_crud.typed

import org.jetbrains.exposed.v1.core.ColumnSet
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Query

interface IEntityTable<T, ID: Any> {
    fun toEntity(row: ResultRow, related: List<ColumnSet> = emptyList()): T {
        error("Not implemented")
    }

    fun Query.toEntityList(): List<T> = map(::toEntity)

    abstract fun write(update: UpdateBuilder<Number>, data: T)
    abstract fun writeExceptAutoIncrementing(update: UpdateBuilder<Number>, data: T)
    abstract fun makePK(data: T): EntityID<ID>
    abstract fun setId(data: T, id: ID): T
}