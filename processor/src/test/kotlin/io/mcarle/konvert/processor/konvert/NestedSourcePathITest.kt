package io.mcarle.konvert.processor.konvert

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.INVALID_MAPPING_STRATEGY_OPTION
import io.mcarle.konvert.converter.api.config.InvalidMappingStrategy
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains

@OptIn(ExperimentalCompilerApi::class)
class NestedSourcePathITest : KonverterITest() {

    private fun konverterSource(mapping: String, signature: String, classes: String = DEFAULT_CLASSES) = SourceFile.kotlin(
        name = "TestCode.kt",
        contents =
            """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

$classes

@Konverter
interface Mapper {
    @Konvert(mappings = [$mapping])
    $signature
}
            """.trimIndent()
    )

    @Test
    fun mapNestedPathOfSingleSource() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = konverterSource(
                mapping = """Mapping(target = "sessionId", source = "identity.sessionId")""",
                signature = "fun toDto(cart: Cart): CartDto"
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toDto(cart: Cart): CartDto = CartDto(
                id = cart.id,
                sessionId = cart.identity.sessionId
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun mapPathQualifiedByTheSourceParameter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = konverterSource(
                mapping = """Mapping(target = "sessionId", source = "cart.identity.sessionId")""",
                signature = "fun toDto(cart: Cart): CartDto"
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "sessionId = cart.identity.sessionId")
    }

    @Test
    fun readAfterNullableStepNullSafely() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = konverterSource(
                mapping = """Mapping(target = "sessionId", source = "identity.sessionId")""",
                signature = "fun toDto(cart: Cart): CartDto",
                classes = """
class Identity(val sessionId: String)
class Cart(val id: Long, val identity: Identity?)
class CartDto(val id: Long, val sessionId: String?)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "sessionId = cart.identity?.sessionId\n")
    }

    @Test
    fun enforceNotNullForNullablePathIntoNonNullTarget() {
        enforceNotNull = true
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = konverterSource(
                mapping = """Mapping(target = "sessionId", source = "identity.sessionId")""",
                signature = "fun toDto(cart: Cart): CartDto",
                classes = NULLABLE_IDENTITY_CLASSES
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "sessionId = cart.identity?.sessionId!!")
    }

    @Test
    fun failOnNullablePathIntoNonNullTargetWithoutEnforceNotNull() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = konverterSource(
                mapping = """Mapping(target = "sessionId", source = "identity.sessionId")""",
                signature = "fun toDto(cart: Cart): CartDto",
                classes = NULLABLE_IDENTITY_CLASSES
            )
        )

        assertContains(
            compilationResult.messages,
            "Mapping from cart.identity?.sessionId:String? to sessionId:String without !! operator not possible."
        )
    }

    @Test
    fun startPathOfNullableSourceFromTheSmartCastValue() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = konverterSource(
                mapping = """Mapping(target = "sessionId", source = "identity.sessionId")""",
                signature = "fun toDto(cart: Cart?): CartDto?"
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toDto(cart: Cart?): CartDto? = cart?.let {
                CartDto(
                  id = cart.id,
                  sessionId = cart.identity.sessionId
                )
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun readComputedPropertyAtTheEndOfThePath() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = konverterSource(
                mapping = """Mapping(target = "sessionId", source = "identity.sessionIdOrNull")""",
                signature = "fun toDto(cart: Cart): CartDto",
                classes = """
class Identity(private val raw: String) {
    val sessionIdOrNull: String? get() = raw.takeIf { it.isNotBlank() }
}
class Cart(val id: Long, val identity: Identity)
class CartDto(val id: Long, val sessionId: String?)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "sessionId = cart.identity.sessionIdOrNull\n")
    }

    @Test
    fun failOnMissingPathWithFailStrategy() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            options = mapOf(INVALID_MAPPING_STRATEGY_OPTION.key to InvalidMappingStrategy.FAIL.name),
            code = konverterSource(
                mapping = """Mapping(target = "sessionId", source = "identity.missing")""",
                signature = "fun toDto(cart: Cart): CartDto"
            )
        )

        assertContains(compilationResult.messages, "The referenced source field(s) [identity.missing] do not exist")
    }

    @Test
    fun warnOnMissingPathWithWarnStrategy() {
        val (compilation, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(INVALID_MAPPING_STRATEGY_OPTION.key to InvalidMappingStrategy.WARN.name),
            code = konverterSource(
                mapping = """Mapping(target = "sessionId", source = "identity.missing")""",
                signature = "fun toDto(cart: Cart): CartDto",
                classes = """
class Identity(val sessionId: String)
class Cart(val id: Long, val identity: Identity)
class CartDto(val id: Long, val sessionId: String? = null)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(compilationResult.messages, "as the source field 'identity.missing' does not exist.")
        assertContains(mapperCode, "id = cart.id\n")
    }

    @Test
    fun mapNestedPathWithKonvertTo() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

class Identity(val sessionId: String?)

@KonvertTo(CartDto::class, mappings = [Mapping(target = "sessionId", source = "identity.sessionId")])
class Cart(val id: Long, val identity: Identity?)
class CartDto(val id: Long, val sessionId: String?)
                    """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("CartKonverter.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "sessionId = identity?.sessionId\n")
    }

    @Test
    fun mapNestedPathWithKonvertFrom() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class Identity(val sessionId: String)
class Cart(val id: Long, val identity: Identity)
class CartDto(val id: Long, val sessionId: String) {
    @KonvertFrom(Cart::class, mappings = [Mapping(target = "sessionId", source = "identity.sessionId")])
    companion object
}
                    """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("CartDtoKonverter.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "sessionId = cart.identity.sessionId\n")
    }

    private companion object {
        val DEFAULT_CLASSES = """
class Identity(val sessionId: String)
class Cart(val id: Long, val identity: Identity)
class CartDto(val id: Long, val sessionId: String)
        """.trimIndent()

        val NULLABLE_IDENTITY_CLASSES = """
class Identity(val sessionId: String)
class Cart(val id: Long, val identity: Identity?)
class CartDto(val id: Long, val sessionId: String)
        """.trimIndent()
    }
}
