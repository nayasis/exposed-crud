package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.Ignore

/**
 * Entity for testing column selection rules.
 */
@Entity("ignore_entities")
data class IgnoreEntity(
    @Id(autoGenerate = true)
    @Column
    var id: Long = -1,
    @Column
    var name: String = "",
    @Ignore
    @Column
    var ignoredField: String? = null,
    @Column
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

