package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.interfaces.AttributeConverter
import com.dshatz.exposed_crud.Convert
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.Varchar

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
    override fun convertToDatabaseColumn(attribute: Color): String {
        return attribute.toString()
    }
    override fun convertToEntityAttribute(dbData: String?): Color {
        return dbData?.let { Color.fromString(it) } ?: Color(0,0,0)
    }
}

class NullableColorConverter : AttributeConverter<Color?, String?> {
    override fun convertToDatabaseColumn(attribute: Color?): String? {
        return attribute?.toString()
    }
    override fun convertToEntityAttribute(dbData: String?): Color? {
        return dbData?.let { Color.fromString(it) }
    }
}

@Entity
data class ConvertedEntity(
    @Id val id: Int = 0,
    @Convert(ColorConverter::class)
    @Varchar(50)
    val color: Color,
    @Convert(NullableColorConverter::class)
    val nullableColor: Color?,
)