package com.dshatz.exposeddataclass

import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.Ignore
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.FileLocation
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlin.reflect.KClass

@Throws(ProcessorException::class)
fun KSClassDeclaration.findPropWithAnnotation(cls: KClass<*>): Pair<KSPropertyDeclaration, KSAnnotation> {
    return findPropsWithAnnotation(cls).singleOrNull()
        ?: throw ProcessorException("Expected single annotation ${cls.simpleName}", this)
}

@Throws(ProcessorException::class)
fun KSClassDeclaration.findPropsWithAnnotation(cls: KClass<*>): List<Pair<KSPropertyDeclaration, KSAnnotation>> {
    return getAllProperties().associateWith {
        it.getAnnotation(cls)
    }.entries
        .filter { it.value != null }
        .map { it.key to it.value!! }
}

fun KSAnnotated.getAnnotation(cls: KClass<*>): KSAnnotation? {
    return annotations.find { ka ->
        ka.annotationType.resolve().declaration.qualifiedName?.asString() == cls.qualifiedName
    }
}

class ProcessorException(message: String, val symbol: KSNode): Exception(message)

fun ProcessorException.messageWithSymbolContext(): String {
    val base = message.orEmpty()
    val context = symbol.describeSymbolContext() ?: return base
    return "$base ($context)"
}

private fun KSNode.describeSymbolContext(): String? {
    val parts = mutableListOf<String>()

    when (this) {
        is KSPropertyDeclaration -> {
            parts += "class=${parentDeclaration?.qualifiedName?.asString() ?: "<unknown>"}"
            parts += "field=${getPropName()}"
        }
        is KSClassDeclaration -> {
            parts += "class=${qualifiedName?.asString() ?: simpleName.asString()}"
        }
        is KSValueParameter -> {
            parts += "parameter=${getName()}"
        }
    }

    (location as? FileLocation)?.let {
        parts += "location=${it.filePath}:${it.lineNumber}"
    }

    return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
}

fun KSPropertyDeclaration.getPropName(): String {
    return simpleName.asString()
}

fun KSClassDeclaration.getClassName(): String {
    return simpleName.asString()
}

fun KSValueParameter.getName(): String {
    return name?.asString() ?: throw ProcessorException("Unable to find default value", this)
}

fun String.decapitate(): String = replaceFirstChar { it.lowercase() }

inline fun <reified T> KSAnnotation.getArgumentAs(index: Int = 0): T? {
    return arguments.getOrNull(index)?.value?.let {
        it as T
    }
}

fun KSAnnotation.parse(): AnnotationInfo = AnnotationInfo(
    cls = annotationType.toTypeName() as ClassName,
    params = arguments.mapIndexedNotNull { idx, arg ->
        val default = defaultArguments.getOrNull(idx)
        if (arg.value == default?.value) null
        else arg.value
    }
)

fun AnnotationInfo.generate(): AnnotationSpec {
    val a = AnnotationSpec.builder(cls)
    params.forEach {
        a.addMember(CodeBlock.of(it.toString()))
    }
    return a.build()
}

fun KSAnnotation.valueByKey(name: String): Any? {
    return this.arguments.firstOrNull { it.name?.asString() == name }?.value
}

fun KSAnnotation.isArgumentDefault(name: String): Boolean {
    val arg = arguments.firstOrNull { it.name?.asString() == name }
    val defaultArg = defaultArguments.firstOrNull { it.name?.asString() == name }
    return arg?.value == defaultArg?.value
}

fun KSAnnotated?.hasTransientAnnotation(): Boolean {
    return this?.annotations?.any {
        when (it.annotationType.resolve().declaration.qualifiedName?.asString()) {
            "kotlin.jvm.Transient",
            "kotlinx.serialization.Transient" -> true
            else -> false
        }
    } == true
}

fun KSPropertyDeclaration.hasAnnotation(annotation: KClass<*>): Boolean {
    return this.getAnnotation(annotation) != null
}

fun KSPropertyDeclaration.hasTransientMarker(): Boolean {
    return hasTransientAnnotation() || getter.hasTransientAnnotation() || setter.hasTransientAnnotation()
}

fun KSAnnotated?.hasIgnoreAnnotation(): Boolean {
    return this?.getAnnotation(Ignore::class) != null
}

fun KSPropertyDeclaration.hasIgnoreMarker(): Boolean {
    return hasIgnoreAnnotation() || getter.hasIgnoreAnnotation() || setter.hasIgnoreAnnotation()
}

fun KSClassDeclaration.findIdProperties(): List<Pair<KSPropertyDeclaration, KSAnnotation>> {
    return this.findPropsWithAnnotation(Id::class)
}

fun KClass<*>.asKClassTypeName(): TypeName {
    return KClass::class.asTypeName().parameterizedBy(
        WildcardTypeName.producerOf(
            this.asTypeName().parameterizedBy(WildcardTypeName.producerOf(Any::class.asTypeName()))
        )
    )
}

val TypeName.notNull: TypeName
    get() = this.copy(nullable = false)

val TypeName.nullable: TypeName
    get() = this.copy(nullable = true)
