package com.dshatz.exposed_crud.interfaces

interface AttributeConverter<X, Y> {
    fun convertToDatabaseColumn(attribute: X): Y
    fun convertToEntityAttribute(dbData: Y): X
}