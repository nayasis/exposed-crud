package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.now
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

@Entity
data class KotlinTimeEntity(
    @Id
    @Column
    val id: Int = 0,
    @Column
    val name: String,
    @Column
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column
    val updatedAt: LocalDateTime? = null,
    @Column
    val birthday: LocalDate = LocalDate(1990, 1, 1),
    @Column
    val anniversary: LocalDate? = null,
    @Column
    val wakeup: LocalTime = LocalTime(7, 0, 0),
)

