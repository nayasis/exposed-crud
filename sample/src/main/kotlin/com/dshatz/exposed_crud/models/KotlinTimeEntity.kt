package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.now
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

@Entity
data class KotlinTimeEntity(
    @Id
    val id: Int = 0,
    val name: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = null,
    val birthday: LocalDate = LocalDate(1990, 1, 1),
    val anniversary: LocalDate? = null,
    val wakeup: LocalTime = LocalTime(7, 0, 0),
)

