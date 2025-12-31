package com.dshatz.exposed_crud

import java.io.Serializable

/**
 * Interface for custom ID generators.
 * 
 * Implement this interface to create custom ID generation logic.
 * 
 * Example:
 * ```kotlin
 * class SampleIdGenerator: IdGenerator<Sample> {
 *     companion object {
 *         var seq = 0
 *     }
 *     
 *     override fun generate(entity: Sample): Serializable {
 *         return entity.name + "${seq++}"
 *     }
 * }
 * ```
 */
interface IdGenerator<E : Any> {
    /**
     * Generates an ID for the given entity.
     * 
     * @param entity The entity instance for which to generate an ID
     * @return The generated ID value
     */
    fun generate(entity: E): Serializable
}

