package com.dshatz.exposed_crud

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.Index
import com.dshatz.exposed_crud.Table

/**
 * This file should cause compilation error because columnList is empty
 */
@Table(
    name = "invalid_table",
    indexes = [
        Index(columnList = "")  // This should cause an error
    ]
)
@Entity("invalid_table")
class InvalidEntity(
    @Id(autoGenerate = true)
    var id: Long = -1,
    var name: String = ""
)

