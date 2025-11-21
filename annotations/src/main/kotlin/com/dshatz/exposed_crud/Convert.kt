package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.interfaces.AttributeConverter
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Convert(
    val converter: KClass<out AttributeConverter<*, *>>
)
