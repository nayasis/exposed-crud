package com.dshatz.exposed_crud.helper

import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

object TestHelper {

    fun prepareDatabase(
        tables: List<Table>,
        url: String = "jdbc:h2:mem:test_create_returning_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MYSQL",
    ): Database {
        val db = Database.connect(url)
        transaction(db) {
            addLogger(StdOutSqlLogger)
            tables.forEach {
                SchemaUtils.drop(it)
                SchemaUtils.create(it)
            }
        }
        return db
    }

}