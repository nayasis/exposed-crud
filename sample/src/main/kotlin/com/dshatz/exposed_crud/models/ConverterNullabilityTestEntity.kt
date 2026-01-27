package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Convert
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.interfaces.AttributeConverter

data class TestValue(val value: String)

/**
 * Case 1: Entity nullable, DB nullable
 * AttributeConverter<TestValue?, String?>
 */
class NullableEntityNullableDbConverter : AttributeConverter<TestValue?, String?> {
    override fun convertToDatabaseColumn(entityData: TestValue?): String? {
        return entityData?.value
    }

    override fun convertToEntityAttribute(dbData: String?): TestValue? {
        return dbData?.let { TestValue(it) }
    }
}

/**
 * Case 2: Entity nullable, DB non-nullable
 * AttributeConverter<TestValue?, String>
 */
class NullableEntityNonNullableDbConverter : AttributeConverter<TestValue?, String> {
    override fun convertToDatabaseColumn(entityData: TestValue?): String {
        return entityData?.value ?: ""
    }

    override fun convertToEntityAttribute(dbData: String): TestValue? {
        return if (dbData.isEmpty()) null else TestValue(dbData)
    }
}

/**
 * Case 3: Entity non-nullable, DB nullable
 * AttributeConverter<TestValue, String?>
 */
class NonNullableEntityNullableDbConverter : AttributeConverter<TestValue, String?> {
    override fun convertToDatabaseColumn(entityData: TestValue): String {
        return entityData.value
    }

    override fun convertToEntityAttribute(dbData: String?): TestValue {
        return dbData?.let { TestValue(it) } ?: TestValue("default")
    }
}

/**
 * Case 4: Entity non-nullable, DB non-nullable
 * AttributeConverter<TestValue, String>
 */
class NonNullableEntityNonNullableDbConverter : AttributeConverter<TestValue, String> {
    override fun convertToDatabaseColumn(entityData: TestValue): String {
        return entityData.value
    }

    override fun convertToEntityAttribute(dbData: String): TestValue {
        return TestValue(dbData)
    }
}

/**
 * Entity for testing all 4 nullability combinations
 */
@Entity
data class ConverterNullabilityTestEntity(

    @Id(autoGenerate = true)
    @Column
    var id: Long = -1,

    // Case 1: Entity nullable, DB nullable
    @Column
    @Convert(NullableEntityNullableDbConverter::class)
    var case1NullableEntityNullableDb: TestValue? = null,

    // Case 2: Entity nullable, DB non-nullable
    @Column
    @Convert(NullableEntityNonNullableDbConverter::class)
    var case2NullableEntityNonNullableDb: TestValue? = null,

    // Case 3: Entity non-nullable, DB nullable
    @Column
    @Convert(NonNullableEntityNullableDbConverter::class)
    var case3NonNullableEntityNullableDb: TestValue = TestValue("default"),

    // Case 4: Entity non-nullable, DB non-nullable
    @Column
    @Convert(NonNullableEntityNonNullableDbConverter::class)
    var case4NonNullableEntityNonNullableDb: TestValue = TestValue("default"),

)
