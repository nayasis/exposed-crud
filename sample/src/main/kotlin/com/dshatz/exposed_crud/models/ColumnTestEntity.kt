package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id

/**
 * Entity for testing Column annotation with default value and blank handling
 */
@Entity("column_test")
data class ColumnTestEntity(
    @Id(autoGenerate = true)
    @Column
    var id: Long = -1,
    @Column()  // Uses default (blank) -> column name from property (snake_case)
    var defaultColumn: String = "",
    @Column("")  // Explicit blank -> column name from property (snake_case)
    var blankColumn: String = "",
    @Column("custom_name")  // Custom name
    var customColumn: String = "",
    var normalColumn: String = ""  // No annotation -> not generated as a column
)


