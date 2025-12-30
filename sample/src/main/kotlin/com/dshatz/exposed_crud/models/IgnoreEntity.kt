package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.Ignore

/**
 * Entity for testing @Ignore annotation
 */
@Entity("ignore_entities")
data class IgnoreEntity(
    @Id(autoGenerate = true)
    var id: Long = -1,
    var name: String = "",
    @Ignore
    var ignoredField: String? = null,
    var active: Boolean = true
) {
    @Ignore
    var computedProperty: String
        get() = ignoredField ?: ""
        set(value) {
            ignoredField = value
        }
    
    @Ignore
    var anotherIgnored: Int = 0
}

