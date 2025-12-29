package com.dshatz.exposed_crud.interfaces

interface AttributeConverter<X, Y> {
    fun convertToDatabaseColumn(entityData: X): Y
    fun convertToEntityAttribute(dbData: Y): X
}