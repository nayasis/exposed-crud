package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import kotlin.jvm.Transient

@Entity
data class IgnoredFieldEntity(
    @Id
    @Column
    val id: Int = 0,
    @Column
    val name: String,
    @Transient
    var ignoredField: String? = null,
) {

    var ignoredFlag: String
        get() = ignoredField ?: ""
        set(value) {
            ignoredField = value
        }


    @Transient
    var age = 0

}

