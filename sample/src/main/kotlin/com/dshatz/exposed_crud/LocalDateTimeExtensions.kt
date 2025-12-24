package com.dshatz.exposed_crud

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun LocalDateTime.Companion.now(): LocalDateTime {
    return kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}
