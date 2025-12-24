package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import java.time.LocalDateTime

/**
 * Base entity with common timestamp fields
 */
open class DefaultEntity(
    @Column("created_at")
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    @Column("updated_at")
    var updatedAt: LocalDateTime? = null
)

/**
 * Abstract person entity extending DefaultEntity
 */
abstract class AbstractEmployee(
    @Id(autoGenerate = true)
    var id: Long = -1,
    var name: String = "",
    var age: Int = 0,
) : DefaultEntity()

/**
 * Employee entity for Japan - extends AbstractEmployee
 */
@Entity("TB_EMPLOYEE_JAPAN")
class EmployeeJapan: AbstractEmployee()

/**
 * Employee entity for India - extends AbstractEmployee
 */
@Entity("TB_EMPLOYEE_INDIA")
class EmployeeIndia : AbstractEmployee()

