package com.dshatz.exposed_crud.interfaces

interface AttributeConverter<Entity, Db> {
    fun convertToDatabaseColumn(entityData: Entity): Db
    fun convertToEntityAttribute(dbData: Db): Entity
}