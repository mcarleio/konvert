package io.mcarle.lib.kmapper.processor.converter.annotated

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.lib.kmapper.annotation.KMappers
import io.mcarle.lib.kmapper.processor.converter.ConverterITest
import io.mcarle.lib.kmapper.processor.converter.StringToIntConverter
import io.mcarle.lib.kmapper.processor.converter.generatedSourceFor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.memberExtensionFunctions

class KMapToConverterITest : ConverterMapToITest() {


    @Test
    fun converterTest() {
        super.converterTest(
            converter = StringToIntConverter(),
            sourceTypeName = "Bla",
            targetTypeName = "Blub"
        )
    }

    override fun generateAdditionalCode(): SourceFile = SourceFile.kotlin(
        name = "Additional.kt",
        contents =
        """
import io.mcarle.lib.kmapper.annotation.KMapTo
import io.mcarle.lib.kmapper.annotation.KMapping

@KMapTo(Blub::class)
class Bla(
    val test: String  
)
class Blub(
    val test: Int  
)
        """.trimIndent()
    )

    override fun loadAdditionalCode(compilation: KotlinCompilation): String {
        return compilation.generatedSourceFor("BlaKMapExtensions.kt")
    }

    override fun verifyMapper(
        sourceTypeName: String,
        targetTypeName: String,
        mapperKClass: KClass<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>,
        classLoader: ClassLoader
    ) {
        KMappers.classLoader += classLoader

        val sourceInstance = (sourceKClass.constructors.first().parameters.first().type.classifier as KClass<*>)
            .constructors.first().call("123").let {
                sourceKClass.constructors.first().call(it)
            }

//        val targetInstance = sourceKClass.memberExtensionFunctions.first { it.name == "mapToYyy" }.call(sourceInstance)
        val method = mapperKClass.java.methods.first { it.name == "mapToYyy" } // ugly workaround to access generated member function
        val targetInstance = method.invoke(null, sourceInstance)

        assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }
    }

}

