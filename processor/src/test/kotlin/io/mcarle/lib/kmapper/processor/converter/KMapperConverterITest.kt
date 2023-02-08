package io.mcarle.lib.kmapper.processor.converter

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.lib.kmapper.annotation.KMappers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class KMapperConverterITest : ConverterITest() {


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
import io.mcarle.lib.kmapper.annotation.KMapper
import io.mcarle.lib.kmapper.annotation.KMapping

class Bla(
    val test: String  
)
class Blub(
    val test: Int  
)

@KMapper
interface FooFooMapper {
    @KMapping
    fun something(somewhere: Bla): Blub
}
        """.trimIndent()
    )

    override fun loadAdditionalCode(compilation: KotlinCompilation): String {
        return compilation.generatedSourceFor("FooFooMapperImpl.kt")
    }

    override fun verifyMapper(
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>,
        classLoader: ClassLoader
    ) {
        KMappers.classLoader += classLoader
//        TypeConverterRegistry.filterIsInstance<KMapperConverter>().forEach {
//            val converterInterfaceClass = classLoader.loadClass(it.mapKSClassDeclaration.qualifiedName?.asString())
//            val converterImplClass = classLoader.loadClass(it.mapKSClassDeclaration.qualifiedName?.asString() + "Impl")
//            KMappers.add(converterInterfaceClass.kotlin, converterImplClass.kotlin.objectInstance!!)
//        }


        val sourceInstance = (sourceKClass.constructors.first().parameters.first().type.classifier as KClass<*>)
            .constructors.first().call("123").let {
                sourceKClass.constructors.first().call(it)
            }

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }
    }

}

