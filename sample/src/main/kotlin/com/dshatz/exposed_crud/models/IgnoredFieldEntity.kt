package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import kotlin.jvm.Transient

@Entity
data class IgnoredFieldEntity(
    @Id val id: Int = 0,
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

