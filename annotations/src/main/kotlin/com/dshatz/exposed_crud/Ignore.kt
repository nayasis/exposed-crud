package com.dshatz.exposed_crud

/**
 * Marks a property to be ignored during table generation.
 * Can be applied to the property itself, its getter, or its setter.
 * 
 * Example:
 * ```
 * data class Sample(
 *     @Ignore
 *     var name: String
 * )
 * 
 * class Sample {
 *     @Ignore
 *     val name: String
 *         get() { ... }
 *         set(value) { ... }
 * }
 * ```
 */
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.SOURCE)
annotation class Ignore

