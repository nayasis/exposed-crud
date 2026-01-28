package com.dshatz.exposed_crud

import kotlinx.serialization.json.Json

/**
 * Default JSON format configurations for JSON/JSONB columns.
 */
object DefaultJsonFormats {
    /**
     * Default JSON format used when no custom format is specified.
     */
    val default: Json by lazy {
        Json {
            prettyPrint = true
        }
    }
}
