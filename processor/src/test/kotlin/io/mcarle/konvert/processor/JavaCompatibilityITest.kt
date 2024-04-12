package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class JavaCompatibilityITest : KonverterITest() {

    @Test
    fun java() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "Address.kt",
                    contents =
                    """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.Konfig

@KonvertFrom(JavaAddress::class, options = [
    Konfig(key = "konvert.enforce-not-null", value = "true")
])
data class Address(val street: String) {
    companion object
}
                    """.trimIndent()
                ),
                SourceFile.java(
                    name = "JavaAddress.java",
                    contents =
                    """
public class JavaAddress {
    private String street_ = "";

    public String getStreet() {
        return street_;
    }

}
                """.trimIndent()
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("AddressKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun Address.Companion.fromJavaAddress(javaAddress: JavaAddress): Address = Address(
              street = javaAddress.street!!
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}
