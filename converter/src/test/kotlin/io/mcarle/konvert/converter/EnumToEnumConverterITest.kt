package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class EnumToEnumConverterITest : ConverterITest() {

    companion object {

        @JvmStatic
        fun types(): List<Arguments> = listOf("MyFirstEnum").toConverterTestArguments {
            it to "MySecondEnum"
        }
    }

    @ParameterizedTest
    @MethodSource("types")
    fun converterTest(sourceTypeName: String, targetTypeName: String) {
        super.converterTest(EnumToEnumConverter(), sourceTypeName, targetTypeName)
    }

    @ParameterizedTest
    @MethodSource("types")
    fun missingEnumValues(sourceTypeName: String, targetTypeName: String) {
        expectedResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR
        super.converterTest(EnumToEnumConverter(), targetTypeName, sourceTypeName)
    }

    @Test
    fun enumsInDifferentPackages() {
        super.converterTest(EnumToEnumConverter(), "a.OtherEnum", "b.OtherEnum")
    }

    override fun generateAdditionalCode(): List<SourceFile> = listOf(
        SourceFile.kotlin(
            name = "MyEnums.kt",
            contents =
            """
enum class FirstEnum {
    XXX,
    YYY,
    ZZZ
}
enum class SecondEnum {
    AAA,
    ZZZ,
    XXX,
    YYY,
}
typealias MyFirstEnum = FirstEnum
typealias MySecondEnum = SecondEnum
        """.trimIndent()
        ), SourceFile.kotlin(
            name = "a/OtherEnum.kt",
            contents = """
package a

enum class OtherEnum {
    AAA,
    BBB,
    CCC
}
        """.trimIndent()
        ), SourceFile.kotlin(
            name = "b/OtherEnum.kt",
            contents = """
package b

enum class OtherEnum {
    AAA,
    BBB,
    CCC,
    DDD
}
        """.trimIndent()
        )
    )

    override fun verifyMapper(
        converter: TypeConverter,
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>
    ) {
        val enumValue =
            (sourceKClass.members.first { it.name == "test" }.returnType.classifier as KClass<*>).java.enumConstants.random() as Enum<*>
        val sourceInstance = sourceKClass.constructors.first().call(
            enumValue
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }
        assertEquals(enumValue.toString(), targetValue.toString())
    }

}

