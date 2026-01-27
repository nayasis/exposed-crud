package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
data class JavaTimeEntity(
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
    val birthday: LocalDate = LocalDate.of(2000, 1, 1),
    @Column
    val anniversary: LocalDate? = null,
    @Column
    val wakeup: LocalTime = LocalTime.of(7, 0),
)

