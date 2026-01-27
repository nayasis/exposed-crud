package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Convert
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.LargeText
import com.dshatz.exposed_crud.MediumText
import com.dshatz.exposed_crud.Text
import com.dshatz.exposed_crud.Varchar
import com.dshatz.exposed_crud.interfaces.AttributeConverter

data class TestData(val value: String)

class TestDataConverter : AttributeConverter<TestData?, String?> {
    override fun convertToDatabaseColumn(entityData: TestData?): String? {
        return entityData?.value
    }
    
    override fun convertToEntityAttribute(dbData: String?): TestData? {
        return dbData?.let { TestData(it) }
    }
}

@Entity
data class TextColumnWithConverterEntity(
    @Id(autoGenerate = true)
    @Column
    var id: Long = -1,
    
    @Column
    @Convert(TestDataConverter::class)
    @LargeText
    var largeTextField: TestData?,
    
    @Column
    @Convert(TestDataConverter::class)
    @MediumText
    var mediumTextField: TestData?,
    
    @Column
    @Convert(TestDataConverter::class)
    @Text
    var textField: TestData?,
    
    @Column
    @Convert(TestDataConverter::class)
    @Varchar(50)
    var varcharField: TestData?,
)

