package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.StringToIntConverter
import io.mcarle.konvert.converter.api.config.ENABLE_CONVERTERS_OPTION
import io.mcarle.konvert.processor.exceptions.AmbiguousConstructorException
import io.mcarle.konvert.processor.exceptions.NoMatchingConstructorException
import io.mcarle.konvert.processor.exceptions.NoMatchingTypeConverterException
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals


@Suppress("RedundantVisibilityModifier")
@OptIn(ExperimentalCompilerApi::class)
class TargetStructureITest : KonverterITest() {

    @Test
    fun setPropertiesAfterConstructor() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(source="sourceProperty1", target="targetProperty1"), Mapping(source="sourceProperty2", target="targetProperty2"), Mapping(source="sourceProperty3", target="targetProperty3")
])
class SourceClass(
    val sourceProperty1: String,
    val sourceProperty2: String,
    val sourceProperty3: String
)
class TargetClass(
    var targetProperty1: String,
) {
    var targetProperty2: String = ""
    var targetProperty3: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty1 = sourceProperty1
            ).also { targetClass ->
              targetClass.targetProperty2 = sourceProperty2
              targetClass.targetProperty3 = sourceProperty3
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun ignoreDefinedProperties() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(source="sourceProperty1", target="targetProperty1"),
    Mapping(source="sourceProperty2", target="targetProperty2"),
    Mapping(target="targetProperty3", ignore = true)
])
class SourceClass(
    val sourceProperty1: String,
    val sourceProperty2: String,
    val sourceProperty3: String
)
class TargetClass(
    var targetProperty1: String,
) {
    var targetProperty2: String = ""
    var targetProperty3: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty1 = sourceProperty1
            ).also { targetClass ->
              targetClass.targetProperty2 = sourceProperty2
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun useSameSourcePropertyForDifferentTargets() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(source="sourceProperty", target="targetProperty1"),
    Mapping(source="sourceProperty", target="targetProperty2"),
    Mapping(source="sourceProperty", target="targetProperty3")
])
class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    var targetProperty1: String,
    var targetProperty2: String,
    var targetProperty3: String
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty1 = sourceProperty,
              targetProperty2 = sourceProperty,
              targetProperty3 = sourceProperty
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun useConstantAndExpression() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="targetProperty1", expression = "it.sourceProperty.lowercase()"),
    Mapping(target="targetProperty2", constant = "\"Hello\""),
])
class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    var targetProperty1: String,
    var targetProperty2: String
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty1 = let { it.sourceProperty.lowercase() },
              targetProperty2 = "Hello"
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun enableProvidedConverter() {
        val (compilation) = compileWith(
            enabledConverters = emptyList(),
            otherConverters = listOf(StringToIntConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(source="sourceProperty", target="targetProperty", enable=["StringToIntConverter"])
])
class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    var targetProperty: Int
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty = sourceProperty.toInt()
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotSetPropertiesNotPartOfNonEmptyConstructor() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="targetProperty", source = "property")
])
class SourceClass(
    val property: String
)
class TargetClass(
    var targetProperty: String
) {
    var property: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty = property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun setExplicitDefinedPropertiesNotPartOfNonEmptyConstructor() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="targetProperty", source = "property"),
    Mapping(target="property", source = "property")
])
class SourceClass(
    val property: String
)
class TargetClass(
    var targetProperty: String
) {
    var property: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty = property
            ).also { targetClass ->
              targetClass.property = property
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun setNotDefinedPropertiesOnEmptyConstructor() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="targetProperty", source = "property")
])
class SourceClass(
    val property: String
)
class TargetClass {
    var targetProperty: String = ""
    var property: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass().also { targetClass ->
              targetClass.targetProperty = property
              targetClass.property = property
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun setMutableProperties() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="property1", source = "property1")
])
class SourceClass(
    val property1: String,
    val property2: String,
    val property3: String
)
class TargetClass {
    var property1: String = ""
    var property2: String = ""
    var property3: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass().also { targetClass ->
              targetClass.property1 = property1
              targetClass.property2 = property2
              targetClass.property3 = property3
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotSetFinalProperties() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="property1", source = "property1")
])
class SourceClass(
    val property1: String,
    val property2: String,
    val property3: String
)
class TargetClass {
    val property1: String = ""
    val property2: String = ""
    val property3: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass()
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun useDefinedConstructor() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, constructorArgs = [MyInt::class], mappings=[
    Mapping(target="property1", source = "property1")
])
class SourceClass(
    val property1: String,
    val property2: Int,
    val property3: String
)
class TargetClass {
    var property1: String = ""
    var property2: Int = -1
    constructor(property1: String) {
        this.property1 = property1
    }
    constructor(property2: Int) {
        this.property2 = property2
    }
    val property3: String = ""
}
typealias MyInt = Int
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property2 = property2
            ).also { targetClass ->
              targetClass.property1 = property1
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun enforceUsingEmptyConstructor() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, constructorArgs = [])
class SourceClass(
    val property1: String,
    val property2: Int,
    val property3: String
)
class TargetClass(
    var property1: String,
    var property2: Int,
    var property3: String,
) {
    constructor(): this("",-1,"")
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass().also { targetClass ->
              targetClass.property1 = property1
              targetClass.property2 = property2
              targetClass.property3 = property3
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotUseEmptyConstructorWhenAnotherDefined() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, constructorArgs = [Int::class], mappings=[
    Mapping(target="property1", source = "property1")
])
class SourceClass(
    val property1: String,
    val property2: Int,
    val property3: String
)
class TargetClass {
    var property1: String = ""
    var property2: Int = -1
    constructor()
    constructor(property2: MyInt) {
        this.property2 = property2
    }
    val property3: String = ""
}
typealias MyInt = Int
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property2 = property2
            ).also { targetClass ->
              targetClass.property1 = property1
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun ignorePrivateConstructor() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="property1", source = "property1")
])
class SourceClass(
    val property1: String,
    val property2: Int,
    val property3: String
)
class TargetClass {
    var property1: String = ""
    var property2: Int = -1
    private constructor()
    private constructor(property1: String) {
        this.property1 = property1
    }
    constructor(property2: Int) {
        this.property2 = property2
    }
    val property3: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property2 = property2
            ).also { targetClass ->
              targetClass.property1 = property1
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun throwAmbiguousConstructorException() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(
    val property1: String,
    val property2: Int,
)
class TargetClass {
    var property1: String = ""
    var property2: Int = -1
    constructor(property1: String) {
        this.property1 = property1
    }
    constructor(property2: Int) {
        this.property2 = property2
    }
}
                """.trimIndent()
            )
        )
        assertEquals(expected = KotlinCompilation.ExitCode.COMPILATION_ERROR, actual = compilationResult.exitCode)
        assertContains(compilationResult.messages, AmbiguousConstructorException::class.qualifiedName!!)
    }

    @Test
    fun throwNoMatchingConstructorException() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(
    val property1: String,
    val property2: Int,
)
class TargetClass {
    var property1: String = ""
    var property2: Int = -1
    constructor(property3: String) {
        this.property1 = property3
    }
    constructor(property3: Int) {
        this.property2 = property3
    }
}
                """.trimIndent()
            )
        )
        assertEquals(expected = KotlinCompilation.ExitCode.COMPILATION_ERROR, actual = compilationResult.exitCode)
        assertContains(compilationResult.messages, NoMatchingConstructorException::class.qualifiedName!!)
    }

    @Test
    fun throwNoMatchingConstructorExceptionWhenConstructorTypesWrong() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class, constructorArgs = [String::class, Long::class])
class SourceClass(
    val property1: String,
    val property2: Int,
)
class TargetClass(
    val property1: String,
    val property2: Int,
)
                """.trimIndent()
            )
        )
        assertEquals(expected = KotlinCompilation.ExitCode.COMPILATION_ERROR, actual = compilationResult.exitCode)
        assertContains(compilationResult.messages, NoMatchingConstructorException::class.qualifiedName!!)
    }

    @Test
    fun throwNotNullOperatorNotEnabledExceptionWhenSourceNullableAndTargetNot() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass?): TargetClass
}

class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        assertEquals(expected = KotlinCompilation.ExitCode.COMPILATION_ERROR, actual = compilationResult.exitCode)
        assertContains(compilationResult.messages, NotNullOperatorNotEnabledException::class.qualifiedName!!)
    }

    @Test
    fun throwPropertyMappingNotExistingException() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
}

class SourceClass(val someDifferentPropertyName: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        assertEquals(expected = KotlinCompilation.ExitCode.COMPILATION_ERROR, actual = compilationResult.exitCode)
        assertContains(compilationResult.messages, PropertyMappingNotExistingException::class.qualifiedName!!)
    }

    @Test
    fun workWithValueClasses() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings = [
    Mapping(source = "source", target ="target")
])
data class SourceClass(val source: SourceValueClass)
data class TargetClass(val target: TargetValueClass)


@KonvertTo(TargetValueClass::class)
@JvmInline
value class SourceValueClass(val value: String)

@JvmInline
value class TargetValueClass(val value: String)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              target = source.toTargetValueClass()
            )
            """.trimIndent(),
            extensionFunctionCode
        )

        val extensionFunctionValueClassCode = compilation.generatedSourceFor("SourceValueClassKonverter.kt")
        println(extensionFunctionValueClassCode)

        assertSourceEquals(
            """
            public fun SourceValueClass.toTargetValueClass(): TargetValueClass = TargetValueClass(
              value = value
            )
            """.trimIndent(),
            extensionFunctionValueClassCode
        )
    }

    @Test
    fun ignoreMissingSourceParamForConstructorParametersWithDefaultValue() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class)
data class SourceClass(val property: String)
data class TargetClass(val property: String, val optional: Boolean = true)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun setNullableConstructorParametersToNullValueWhenMissingOrIgnored() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings = [
    Mapping(target = "ignored", ignore = true)
])
data class SourceClass(val property: String)
data class TargetClass(val property: String, val missing: Boolean?, val ignored: Boolean?)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property,
              missing = null,
              ignored = null
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun enableConvertersOption() {
        val (compilation) = compileWith(
            enabledConverters = emptyList(), // intentionally empty, as enabled via option
            otherConverters = listOf(StringToIntConverter()), // StringToIntConverter is not enabled by default
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Konfig

@KonvertTo(TargetClass::class, options = [
    Konfig(key = "${ENABLE_CONVERTERS_OPTION.key}", value= "StringToIntConverter")
])
data class SourceClass(val property: String)
data class TargetClass(val property: Int)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property.toInt()
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun throwExceptionOnMissingConverter() {
        val (_, compilationResult) = compileWith(
            enabledConverters = emptyList(), // intentionally empty
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
data class SourceClass(val property: String)
data class TargetClass(val property: Int)
                """.trimIndent()
            )
        )
        assertContains(compilationResult.messages, NoMatchingTypeConverterException::class.qualifiedName!!)
    }

    @Test
    fun emptyConstructorPropertiesDifferentPackage() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "a/SourceClass.kt",
                    contents =
                        """
package a

import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.processor.SomeTestClass
import b.TargetClass

@KonvertTo(TargetClass::class)
data class SourceClass(val property: SomeTestClass)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "b/TargetClass.kt",
                    contents =
                        """
package b

import io.mcarle.konvert.processor.SomeOtherTestClass

class TargetClass {
    var property: SomeOtherTestClass? = null
}
                    """.trimIndent()
                ),
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            package a

            import b.TargetClass
            import io.mcarle.konvert.processor.toSomeOtherTestClass

            public fun SourceClass.toTargetClass(): TargetClass = TargetClass().also { targetClass ->
              targetClass.property = property.toSomeOtherTestClass()
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun determineConstructorWithParameterBeingIgnoredWhichHasADefaultValueWhileHavingAnotherNonMatchingConstructor_issue160() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "code.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings = [
    Mapping(target = "property", ignore = true)
])
data class SourceClass(var id: Int)
data class TargetClass(var property: Double? = null, var id: Int) {
    constructor(
        other: String,
    ) : this(
        id = other.toInt(),
    )
}
                    """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              id = id
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}
