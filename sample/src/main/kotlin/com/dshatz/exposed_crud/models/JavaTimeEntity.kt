package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
data class JavaTimeEntity(
    @Id
    val id: Int = 0,
    val name: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = null,
    val birthday: LocalDate = LocalDate.of(2000, 1, 1),
    val anniversary: LocalDate? = null,
    val wakeup: LocalTime = LocalTime.of(7, 0),
)

