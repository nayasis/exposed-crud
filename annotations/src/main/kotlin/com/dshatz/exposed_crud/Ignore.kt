package com.dshatz.exposed_crud

/**
 * Marks a property to be ignored during table generation.
 * Can be applied to the property itself, its getter, or its setter.
 */
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.SOURCE)
annotation class Ignore

