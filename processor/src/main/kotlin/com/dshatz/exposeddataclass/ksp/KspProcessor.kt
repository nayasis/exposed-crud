package com.dshatz.exposeddataclass.ksp

import com.dshatz.exposed_crud.BackReference
import com.dshatz.exposed_crud.Collate
import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Convert
import com.dshatz.exposed_crud.CreationTimestamp
import com.dshatz.exposed_crud.Default
import com.dshatz.exposed_crud.DefaultText
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.ForeignKey
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.Json
import com.dshatz.exposed_crud.JsonFormat
import com.dshatz.exposed_crud.Jsonb
import com.dshatz.exposed_crud.LargeText
import com.dshatz.exposed_crud.MediumText
import com.dshatz.exposed_crud.References
import com.dshatz.exposed_crud.Table
import com.dshatz.exposed_crud.Text
import com.dshatz.exposed_crud.UpdateTimestamp
import com.dshatz.exposed_crud.interfaces.AttributeConverter
import com.dshatz.exposeddataclass.ColumnModel
import com.dshatz.exposeddataclass.ConverterInfo
import com.dshatz.exposeddataclass.EntityModel
import com.dshatz.exposeddataclass.FKInfo
import com.dshatz.exposeddataclass.FieldAttrs
import com.dshatz.exposeddataclass.Generator
import com.dshatz.exposeddataclass.IndexInfo
import com.dshatz.exposeddataclass.JsonFormatModel
import com.dshatz.exposeddataclass.PrimaryKey
import com.dshatz.exposeddataclass.ProcessorException
import com.dshatz.exposeddataclass.ReferenceInfo
import com.dshatz.exposeddataclass.decapitate
import com.dshatz.exposeddataclass.findIdProperties
import com.dshatz.exposeddataclass.getAnnotation
import com.dshatz.exposeddataclass.getArgumentAs
import com.dshatz.exposeddataclass.getPropName
import com.dshatz.exposeddataclass.hasAnnotation
import com.dshatz.exposeddataclass.messageWithSymbolContext
import com.dshatz.exposeddataclass.notNull
import com.dshatz.exposeddataclass.parse
import com.dshatz.exposeddataclass.valueByKey
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.math.BigInteger
import java.security.MessageDigest

class KspProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
    private val basePackage: String = "com.exposeddataclass"
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.getSymbolsWithAnnotation(Entity::class.qualifiedName!!)
        try {
            val jsonFormats = processJsonFormats(resolver)
            val entityClasses = annotated.asClassDeclarations()
            val models = entityClasses.associate {
                it.toClassName() to processEntity(it)
            }

            validate(models.values)

            val generator = Generator(models, jsonFormats, logger)
            val jsonFormatSpec = generator.generateJsonFormatAccessors()
            val files = generator.generate()

            files.forEach { (model, file) ->
                file.writeTo(codeGenerator, true, listOf(model.declaration.containingFile!!))
            }
            jsonFormatSpec.forEach { name, (declaration, file) ->
                file.writeTo(codeGenerator, true, listOf(declaration.containingFile!!))
            }

            return emptyList()
        } catch (e: ProcessorException) {
            logger.error(e.messageWithSymbolContext(), e.symbol)
            return annotated.toList()
        }
    }

    private fun processJsonFormats(resolver: Resolver): JsonFormatModel {
        val formatProviders = resolver.getSymbolsWithAnnotation(JsonFormat::class.qualifiedName!!)
        val nameToFunctionMap = formatProviders.associate {
            val funDeclaration = (it as KSFunctionDeclaration)
            if ((funDeclaration.returnType?.toTypeName() as? ClassName)?.canonicalName == "kotlinx.serialization.json.Json") {
                val formatName = it.getAnnotation(JsonFormat::class)?.getArgumentAs<String>()!!
                val createName = funDeclaration.qualifiedName!!.asString()
                formatName to funDeclaration
            } else {
                throw ProcessorException("@JsonFormat annotated functions should return a kotlinx json configuration (kotlinx.serialization.json.Json).", it)
            }
        }
        return JsonFormatModel(nameToFunctionMap)
    }

    private fun Sequence<KSAnnotated>.asClassDeclarations(): Sequence<KSClassDeclaration> {
        return mapNotNull {
            // allow (normal) class and data class
            it as? KSClassDeclaration ?: throw ProcessorException("Not a class declaration", it)
        }
    }

    @Throws(ProcessorException::class)
    private fun processEntity(entityClass: KSClassDeclaration): EntityModel {

        val tableName = getTableName(entityClass)
        val props     = entityClass.getAllProperties()
        val idProps   = entityClass.findIdProperties()

        val referenceProps = props.filter { it.getAnnotation(References::class) != null }
        val backReferenceProps = props.filter { it.getAnnotation(BackReference::class) != null }
        
        val constructorParameters = entityClass.primaryConstructor?.parameters
            ?.associate { it.name?.asString() to it.hasDefault } ?: emptyMap()

        fun KSPropertyDeclaration.isReferenceProp() =
            this in referenceProps || this in backReferenceProps

        fun KSPropertyDeclaration.validateNonColumnConstructorParam() {
            val constructorDefault = constructorParameters[getPropName()] ?: return
            if (!constructorDefault && !type.toTypeName().isNullable) {
                throw ProcessorException(
                    "Non-column property '${getPropName()}' must be nullable or declare a default value in the constructor.",
                    this
                )
            }
        }

        val columnProps = props.filter { it.getAnnotation(Column::class) != null }

        columnProps.filter { it.isReferenceProp() }.forEach {
            throw ProcessorException(
                "@Column cannot be applied to @References or @BackReference properties.",
                it
            )
        }

        columnProps.filterNot { it.hasBackingField }.forEach {
            throw ProcessorException(
                "@Column property '${it.getPropName()}' must have a backing field.",
                it
            )
        }

        idProps.map { it.first }.filterNot { it in columnProps }.forEach {
            throw ProcessorException(
                "@Id property '${it.getPropName()}' must also be annotated with @Column.",
                it
            )
        }

        props.filterNot { it in columnProps || it.isReferenceProp() }.forEach { prop ->
            prop.validateNonColumnConstructorParam()
        }

        val annotations = entityClass.annotations
            .filterNot { it.annotationType.toTypeName() == Entity::class.asTypeName() }
            .map { it.parse() }

        val uniqueAnnotations = mutableMapOf<String, MutableList<ColumnModel>>()

        val columns = columnProps.associateWith { declaration ->
            toProperty(declaration, idProps, uniqueAnnotations)
        }

        val refColumns = referenceProps.associate { declaration ->
            val prop = toProperty(declaration, idProps, uniqueAnnotations)
            if (!prop.type.isNullable) throw ProcessorException("@References annotated props should be nullable and have default null.", declaration)
            val ref = ReferenceInfo.WithFK(
                related = declaration.getAnnotation(References::class)?.getArgumentAs<KSType>(0)?.toTypeName()!!,
                localIdProps = declaration.getAnnotation(References::class)?.getArgumentAs<List<String>>(1)!!.toTypedArray()
            )
            prop to ref
        }

        val backRefColumns = backReferenceProps.associate { declaration ->
            val prop = toProperty(declaration, idProps, uniqueAnnotations)
            if (!prop.type.isNullable) throw ProcessorException("@BackReference annotated props should be nullable and have default null.", declaration)
            val baseType = prop.type.run {
                if (this is ParameterizedTypeName) this.rawType
                else this
            }.notNull
            val ref = ReferenceInfo.Reverse(
                related = declaration.getAnnotation(BackReference::class)?.getArgumentAs<KSType>(0)?.toTypeName()!!,
                isMany = baseType == LIST
            )
            prop to ref
        }

        val primaryKey = when {
            idProps.size == 1 -> PrimaryKey.Simple(columns[idProps.first().first]!!)
            idProps.size >  1 -> PrimaryKey.Composite(idProps.map { columns[it.first]!! })
            else -> throw ProcessorException("No @Id annotation found", entityClass)
        }

        val indexes = getIndexes(entityClass, columns)

        return EntityModel(
            declaration = entityClass,
            originalClassName = entityClass.toClassName(),
            tableName = tableName,
            columns = columns.values.toList(),
            annotations = annotations.toList(),
            primaryKey = primaryKey,
            uniques = uniqueAnnotations,
            indexes = indexes,
            references = refColumns,
            backReferences = backRefColumns
        )
    }

    fun toProperty(
        declaration: KSPropertyDeclaration,
        idProperties: List<Pair<KSPropertyDeclaration, KSAnnotation>>,
        uniqueAnnotations: MutableMap<String, MutableList<ColumnModel>>,
    ): ColumnModel {

        validateTimestampAnnotation(declaration)

        val name = declaration.getPropName()
        val type = declaration.type.toTypeName()
        val columnAnnotation = declaration.getAnnotation(Column::class)

        val default = if (type.notNull == STRING) {
            declaration.getAnnotation(DefaultText::class)?.getArgumentAs<String>()?.let { CodeBlock.of("%S", it) }
        } else {
            declaration.getAnnotation(Default::class)?.getArgumentAs<String>()?.let { CodeBlock.of("%L", it) }
        }
        val columnName       = (columnAnnotation?.valueByKey("name") as? String)?.takeUnless { it.isBlank() } ?: name.decapitate()
        val isUnique         = (columnAnnotation?.valueByKey("unique") as? Boolean) ?: false
        val length           = (columnAnnotation?.valueByKey("length") as? Int)?.takeIf { it > 0 } ?: 255
        val precision        = (columnAnnotation?.valueByKey("precision") as? Int)?.takeIf { it > 0 } ?: 0
        val scale            = (columnAnnotation?.valueByKey("scale") as? Int)?.takeIf { it > 0 } ?: 0
        val columnDefinition = (columnAnnotation?.valueByKey("definition") as? String).orEmpty()

        val foreignKey = declaration.getAnnotation(ForeignKey::class)?.let {
            val remoteType = it.getArgumentAs<KSType>()?.toTypeName()!!
            val remoteColumn = it.getArgumentAs<String>(1)?.takeUnless { it.isEmpty() }
            FKInfo(remoteType, remoteColumn)
        }

        val idAnnotation = declaration.getAnnotation(Id::class)
        val autoIncrement = idAnnotation?.valueByKey("autoGenerate") as? Boolean ?: false
        val idGenerator = (idAnnotation?.valueByKey("generator") as? KSType)?.toClassName()?.takeUnless{
            it.canonicalName in setOf("kotlin.Nothing","java.lang.Void")
        }

        val converter = getConverter(declaration)

        // Validate text column annotations (Text, MediumText, LargeText)
        // - use converter's targetType if present, otherwise use the original property type to check if it's String.
        val columnType = (converter?.dbType ?: declaration.type.toTypeName()).notNull
        val isStringProp = columnType == STRING || (columnType is ClassName && columnType.canonicalName == "kotlin.String")
        val textProps = listOf(Collate::class, Text::class, MediumText::class, LargeText::class).mapNotNull {
            declaration.getAnnotation(it)?.also {
                // Throw error if not String type
                if (!isStringProp) {
                    val propertyName = declaration.getPropName()
                    val entityName   = (declaration.parentDeclaration as? KSClassDeclaration)?.simpleName?.asString() ?: "Unknown"
                    throw ProcessorException(
                        "${it.annotationType.toTypeName()} can only be used on a String property or a property with a String converter target type. " +
                        "($entityName -> $propertyName)",
                        it
                    )
                }
            }
        }

        val otherProps = listOf(Json::class, Jsonb::class).mapNotNull {
            declaration.getAnnotation(it)
        }.mapNotNull {
            when (it.annotationType.toTypeName()) {
                Json::class.asTypeName() -> FieldAttrs.ColType.Json.Json(it.getArgumentAs()!!)
                Jsonb::class.asTypeName() -> FieldAttrs.ColType.Json.Jsonb(it.getArgumentAs()!!)
                else -> null
            }
        }
        if (textProps.count { it.parse().cls.simpleName != "Collate" } > 1) {
            throw ProcessorException("Only one of Text, MediumText, LargeText can be applied to a String column.", declaration)
        }
        val props = textProps.mapNotNull {
            when (it.annotationType.toTypeName()) {
                Collate::class.asTypeName()    -> FieldAttrs.Collate(it.getArgumentAs<String>())
                Text::class.asTypeName()       -> FieldAttrs.ColType.String.Text.GenericText(it.getArgumentAs<Boolean>()!!)
                MediumText::class.asTypeName() -> FieldAttrs.ColType.String.Text.MediumText(it.getArgumentAs<Boolean>()!!)
                LargeText::class.asTypeName()  -> FieldAttrs.ColType.String.Text.LargeText(it.getArgumentAs<Boolean>()!!)
                else -> null
            }
        }
        val stringLengthAttr = if (isStringProp && props.none { it is FieldAttrs.ColType.String.Text }) {
            length.let { FieldAttrs.ColType.String.Varchar(it) }
        } else null

        return ColumnModel(
            declaration = declaration,
            nameInEntity = name,
            columnName = columnName,
            nameInDsl = name, // Always use the original property name, even for id columns
            type = declaration.type.toTypeName(),
            autoIncrementing = autoIncrement,
            default = default,
            foreignKey = foreignKey,
            attrs = props + listOfNotNull(stringLengthAttr) + otherProps,
            unique = isUnique,
            length = length,
            precision = precision,
            scale = scale,
            columnDefinition = columnDefinition,
            converter = converter,
            isMutable = declaration.isMutable,
            creationTimestamp = declaration.hasAnnotation(CreationTimestamp::class),
            updateTimestamp = declaration.hasAnnotation(UpdateTimestamp::class),
            idGenerator = idGenerator,
        ).also {
            if (isUnique) {
                val uniqueIndexName = "unique_${columnName}"
                uniqueAnnotations.getOrPut(uniqueIndexName) { mutableListOf() }.add(it)
            }
        }
    }

    private fun getConverter(declaration: KSPropertyDeclaration): ConverterInfo? {
        return declaration.getAnnotation(Convert::class)?.let {
            val converterClass         = it.getArgumentAs<KSType>(0)!!
            val converterDeclaration   = converterClass.declaration as KSClassDeclaration
            val attributeConverterType = converterDeclaration.superTypes.firstOrNull { superType ->
                runCatching {
                    superType.resolve().declaration.qualifiedName?.asString() == AttributeConverter::class.qualifiedName
                }.getOrDefault(false)
            }?.resolve() ?: throw ProcessorException(
                "Could not find AttributeConverter supertype for converter",
                declaration
            )

            val entityType = attributeConverterType.arguments[0].type!!.resolve().toTypeName()
            val dbType     = attributeConverterType.arguments[1].type!!.resolve().toTypeName()
            ConverterInfo(converterClass.toTypeName(), entityType, dbType)
        }
    }

    private fun getIndexes(
        entityClass: KSClassDeclaration,
        columns: Map<KSPropertyDeclaration, ColumnModel>
    ): List<IndexInfo> {

        val tableIndexes = (entityClass.getAnnotation(Table::class)?.valueByKey("indexes") as? List<*>)
            ?.filterIsInstance<KSAnnotation>()
            ?: return emptyList()
        
        // validate each index and collect column names for hash generation
        val allColumnNames = columns.values.map { it.columnName }.toSet()
        val processedIndexes = tableIndexes.map { indexAnnotation ->
            val explicitName = (indexAnnotation.valueByKey("name") as? String).takeUnless { it.isNullOrBlank() }

            val unique = indexAnnotation.valueByKey("unique") as? Boolean ?: false

            val columnList = (indexAnnotation.valueByKey("columnList") as? String).takeUnless { it.isNullOrBlank() }
                ?: throw ProcessorException("Index columnList must not be empty", entityClass)

            val columnNames = columnList.split(",").map { it.trim() }.filter { it.isNotEmpty() }.takeIf { it.isNotEmpty() }
                ?: throw ProcessorException("Index columnList must contain at least one column name", entityClass)

            // validate column's existence
            columnNames.filter { ! allColumnNames.contains(it) }.takeIf { it.isNotEmpty() }?.let { columnsNotExist ->
                val indexNameForError = explicitName ?: "auto-generated"
                throw ProcessorException(
                    "Columns(${columnsNotExist}) in index($indexNameForError) does not exist in entity '${entityClass.simpleName}'",
                    entityClass
                )
            }

            // Generate index name: use explicit name or generate hash-based name
            val indexName = explicitName ?: "idx_${generateHashId(columnNames)}"

            IndexInfo(indexName, columnNames, unique)
        }
        
        // Check for duplicate index names (including auto-generated ones)
        val indexNames = processedIndexes.map { it.name }
        val duplicateNames = indexNames.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicateNames.isNotEmpty()) {
            throw ProcessorException(
                "Duplicate index names found: ${duplicateNames.joinToString(", ")}. Index names must be unique.",
                entityClass
            )
        }
        
        return processedIndexes
    }

    /**
     * Generates a 10-character hash ID from column names.
     * The hash is deterministic: same column names in the same order will always produce the same hash.
     * Uses SHA-256 and converts to base36 (0-9, a-z) for a 10-character identifier.
     */
    private fun generateHashId(columnNames: List<String>): String {

        val input = columnNames.joinToString(",")
        
        // Generate SHA-256 hash
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        
        // Use first 6 bytes (48 bits) to ensure we have enough entropy for 10 base36 characters
        val bigInt = BigInteger(1, hashBytes.copyOfRange(0, 6))
        
        // Convert to base36 (0-9, a-z)
        val base36 = bigInt.toString(36)
        
        // extract 10 characters
        return when {
            base36.length < 10 -> base36.padStart(10, '0')
            base36.length > 10 -> base36.take(10)
            else -> base36
        }
    }

    private fun validateTimestampAnnotation(declaration: KSPropertyDeclaration) {
        val hasCreationTimestamp = declaration.hasAnnotation(CreationTimestamp::class)
        val hasUpdateTimestamp   = declaration.hasAnnotation(UpdateTimestamp::class)

        // Validate timestamp annotations can only be applied to DateTime types
        if (hasCreationTimestamp || hasUpdateTimestamp) {
            val columnType = declaration.type.toTypeName()
            val isDateTimeType = when ((columnType as? ClassName)?.canonicalName) {
                "java.util.Date",
                "java.time.LocalDate",
                "java.time.LocalDateTime",
                "java.time.Instant",
                "kotlinx.datetime.LocalDate",
                "kotlinx.datetime.LocalDateTime",
                "kotlin.time.Instant" -> true
                else -> false
            }
            if (!isDateTimeType) {
                val annotationName = if (hasCreationTimestamp) "@CreationTimestamp" else "@UpdateTimestamp"
                throw ProcessorException(
                    "$annotationName can only be applied to DateTime types (Date, LocalDate, LocalDateTime, Instant). Found: $columnType",
                    declaration
                )
            }
        }
    }

    private fun getTableName(entityClass: KSClassDeclaration): String {
        val nameFromTableAnnotation  = entityClass.getAnnotation(Table::class)?.valueByKey("name")?.toString()?.takeUnless { it.isBlank() }
        val nameFromEntityAnnotation = entityClass.getAnnotation(Entity::class)?.valueByKey("name")?.toString()?.takeUnless { it.isBlank() }
        return nameFromTableAnnotation ?: nameFromEntityAnnotation ?: entityClass.toClassName().simpleName
    }


    private fun validate(models: Iterable<EntityModel>) {
        models.forEach { table ->
            if (table.primaryKey is PrimaryKey.Composite && table.columns.any { it.autoIncrementing && it in table.primaryKey}) {
                logger.error("auto-increment on a composite key now allowed", table.declaration)
            }
        }
    }

}
