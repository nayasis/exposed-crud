package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.interfaces.AttributeConverter
import com.dshatz.exposed_crud.Convert
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id

data class Color(val red: Int, val green: Int, val blue: Int) {
    override fun toString(): String = "$red,$green,$blue"

    companion object {
        fun fromString(s: String): Color {
            val parts = s.split(",")
            return Color(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }
    }
}

class ColorConverter : AttributeConverter<Color, String?> {
    override fun convertToDatabaseColumn(entityData: Color): String {
        return entityData.toString()
    }
    override fun convertToEntityAttribute(dbData: String?): Color {
        return dbData?.let { Color.fromString(it) } ?: Color(0,0,0)
    }
}

@Entity
data class ConvertedEntity(
    @Id
    @Column
    val id: Int = 0,
    @Column(length = 50)
    @Convert(ColorConverter::class)
    val color: Color,
)