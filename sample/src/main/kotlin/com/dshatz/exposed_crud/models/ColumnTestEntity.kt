package com.dshatz.exposed_crud.models

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id

/**
 * Entity for testing Column annotation with default value and blank handling
 */
@Entity("column_test")
data class ColumnTestEntity(
    @Id(autoGenerate = true)
    var id: Long = -1,
    @Column()  // 기본값 사용 (blank) - 프로퍼티 이름 기반으로 컬럼명 생성되어야 함
    var defaultColumn: String = "",
    @Column("")  // 명시적으로 빈 문자열 - 프로퍼티 이름 기반으로 컬럼명 생성되어야 함
    var blankColumn: String = "",
    @Column("custom_name")  // 정상적인 이름 지정
    var customColumn: String = "",
    var normalColumn: String = ""  // 어노테이션 없음 - 프로퍼티 이름 기반으로 컬럼명 생성
)

