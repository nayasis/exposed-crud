package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.Index
import com.dshatz.exposed_crud.Table

/**
 * Game entity for testing index functionality
 */
@Table(
    name = "games",
    indexes = [
        Index(name = "specificIndex", columnList = "console_type,cover_type,game_id"),
        Index(name = "uniqueGameIndex", columnList = "game_id", unique = true)
    ]
)
@Entity("games")
class Game(
    @Id(autoGenerate = true)
    var id: Long = -1,
    @Column("console_type")
    var consoleType: String = "",
    @Column("cover_type")
    var coverType: String = "",
    @Column("game_id")
    var gameId: Long = 0,
    var title: String = ""
)

