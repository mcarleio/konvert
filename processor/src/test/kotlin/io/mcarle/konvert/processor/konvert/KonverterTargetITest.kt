package io.mcarle.konvert.processor.konvert

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.IntToStringConverter
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertNull

@OptIn(ExperimentalCompilerApi::class)
class KonverterTargetITest : KonverterITest() {

    @Test
    fun mapIntoExistingTargetInstance() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter

class SourceClass(
    val property: String
)
class TargetClass {
    var property: String = ""
}

@Konverter
interface Mapper {
    @Konvert
    fun update(@Konverter.Source source: SourceClass, @Konverter.Target target: TargetClass)
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun update(source: SourceClass, target: TargetClass) {
                target.property = source.property
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun returnTheUpdatedTargetInstance() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter

class SourceClass(
    val property: String
)
class TargetClass {
    var property: String = ""
}

@Konverter
interface Mapper {
    @Konvert
    fun update(source: SourceClass, @Konverter.Target target: TargetClass): TargetClass
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun update(source: SourceClass, target: TargetClass): TargetClass {
                target.property = source.property
                return target
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun keepIgnoredTargetPropertyUntouchedAndStillMapTheRemainingProperties() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val property: String,
    val other: String
)
class TargetClass {
    var id: Long = 0
    var property: String = ""
    var other: String = ""
}

@Konverter
interface Mapper {
    @Konvert(mappings = [
        Mapping(target = "id", ignore = true),
        Mapping(target = "other", constant = "\"fixed\"")
    ])
    fun update(source: SourceClass, @Konverter.Target target: TargetClass)
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun update(source: SourceClass, target: TargetClass) {
                target.property = source.property
                target.other = "fixed"
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun doNotTouchTheTargetInstanceForANullSource() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter

class SourceClass(
    val property: String
)
class TargetClass {
    var property: String = ""
}

@Konverter
interface Mapper {
    @Konvert
    fun update(source: SourceClass?, @Konverter.Target target: TargetClass)
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun update(source: SourceClass?, target: TargetClass) {
                source?.let {
                  target.property = source.property
                }
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun doNotRegisterUpdateFunctionAsTypeConverter() {
        compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter

class SourceClass(
    val property: String
)
class TargetClass {
    var property: String = ""
}

@Konverter
interface Mapper {
    @Konvert
    fun update(source: SourceClass, @Konverter.Target target: TargetClass)
}
                """.trimIndent()
            )
        )

        val konverterOfUpdateFunction = TypeConverterRegistry
            .filterIsInstance<KonvertTypeConverter>()
            .firstOrNull { it.mapFunctionName == "update" }
        assertNull(
            konverterOfUpdateFunction,
            "An update function must not be registered as a type converter, as it needs a target instance"
        )
    }

    @Test
    fun useExpressionsConvertersAndAdditionalParametersWhenUpdating() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IntToStringConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val number: Int,
    val text: String
)
class TargetClass {
    var number: String = ""
    var text: String = ""
    var addition: String = ""
}

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "text", expression = "it.text.uppercase()")])
    fun update(@Konverter.Source source: SourceClass, @Konverter.Target target: TargetClass, addition: String)
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import kotlin.String

            public object MapperImpl : Mapper {
              override fun update(
                source: SourceClass,
                target: TargetClass,
                addition: String,
              ) {
                target.number = source.number.toString()
                target.text = source.let { it.text.uppercase() }
                target.addition = addition
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun failOnMultipleTargetAnnotatedParameters() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter

class SourceClass(
    val property: String
)
class TargetClass {
    var property: String = ""
}

@Konverter
interface Mapper {
    @Konvert
    fun update(
        source: SourceClass,
        @Konverter.Target target: TargetClass,
        @Konverter.Target otherTarget: TargetClass
    )
}
                """.trimIndent()
            )
        )

        assertContains(compilationResult.messages, "multiple parameters were annotated with @Konverter.Target")
    }

    @Test
    fun failOnReturnTypeDifferentFromTargetParameterType() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter

class SourceClass(
    val property: String
)
class TargetClass {
    var property: String = ""
}

@Konverter
interface Mapper {
    @Konvert
    fun update(source: SourceClass, @Konverter.Target target: TargetClass): String
}
                """.trimIndent()
            )
        )

        assertContains(compilationResult.messages, "must return the type of its @Konverter.Target annotated parameter")
    }
}
