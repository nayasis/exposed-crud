package com.dshatz.exposed_crud

/**
 * Marks a DateTime field to be automatically set to the current time when updating a record.
 * Can only be applied to DateTime types: Date, LocalDate, LocalDateTime, or Kotlin's LocalDate, LocalDateTime.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class UpdateTimestamp




