package io.mcarle.konvert.processor.konvert

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.EnumToEnumConverter
import io.mcarle.konvert.converter.IterableToListConverter
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.INVALID_MAPPING_STRATEGY_OPTION
import io.mcarle.konvert.converter.api.config.InvalidMappingStrategy
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class MultipleSourcesITest : KonverterITest() {

    @Test
    fun mapDisjointPropertiesOfTwoSources() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class OrderEntity(val id: Long, val name: String)
class OrderEnrichment(val status: String)
class Order(val id: Long, val name: String, val status: String)

@Konverter
interface Mapper {
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment): Order
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toOrder(entity: OrderEntity, enrichment: OrderEnrichment): Order = Order(
                id = entity.id,
                name = entity.name,
                status = enrichment.status
              )
            }
            """.trimIndent(),
            mapperCode
        )
        assertTrue(
            TypeConverterRegistry.filterIsInstance<KonvertTypeConverter>().none { it.mapFunctionName == "toOrder" },
            "a function with several sources cannot serve as a type converter"
        )
    }

    @Test
    fun failOnPropertyFoundInTwoSources() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class OrderEntity(val id: Long, val name: String)
class OrderEnrichment(val name: String, val status: String)
class Order(val id: Long, val name: String, val status: String)

@Konverter
interface Mapper {
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment): Order
}
                """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            "The target 'name' matches several sources: [entity.name, enrichment.name]. " +
                "Choose one with @Mapping(target = \"name\", source = \"enrichment.name\"), or map it otherwise"
        )
    }

    @Test
    fun ignoreSameNamedPropertiesWithoutTarget() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class OrderEntity(val id: Long, val version: Int)
class OrderEnrichment(val status: String, val version: Int)
class Order(val id: Long, val status: String)

@Konverter
interface Mapper {
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment): Order
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toOrder(entity: OrderEntity, enrichment: OrderEnrichment): Order = Order(
                id = entity.id,
                status = enrichment.status
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun resolveAmbiguityWithQualifiedSource() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class OrderEntity(val id: Long, val name: String)
class OrderEnrichment(val name: String, val status: String, val title: String)
class Order(val id: Long, val name: String, val status: String, val label: String)

@Konverter
interface Mapper {
    @Konvert(mappings = [
        Mapping(target = "name", source = "enrichment.name"),
        Mapping(target = "label", source = "title"),
    ])
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment): Order
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toOrder(entity: OrderEntity, enrichment: OrderEnrichment): Order = Order(
                id = entity.id,
                name = enrichment.name,
                status = enrichment.status,
                label = enrichment.title
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun failOnUnqualifiedMappingSourceFoundInTwoSources() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class OrderEntity(val id: Long, val title: String)
class OrderEnrichment(val status: String, val title: String)
class Order(val id: Long, val status: String, val label: String)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "label", source = "title")])
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment): Order
}
                """.trimIndent()
            )
        )

        assertContains(compilationResult.messages, "The target 'label' matches several sources: [entity.title, enrichment.title].")
    }

    @Test
    fun failOnMissingQualifiedSourceWithFailStrategy() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            options = mapOf(INVALID_MAPPING_STRATEGY_OPTION.key to InvalidMappingStrategy.FAIL.name),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class OrderEntity(val id: Long)
class OrderEnrichment(val status: String)
class Order(val id: Long, val status: String)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "status", source = "entity.status")])
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment): Order
}
                """.trimIndent()
            )
        )

        assertContains(compilationResult.messages, "The referenced source field(s) [entity.status] do not exist")
    }

    @Test
    fun mapNestedQualifiedPath() {
        enforceNotNull = true
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Customer(val name: String, val address: Address?)
class Address(val city: String)
class OrderEntity(val id: Long, val customer: Customer)
class OrderEnrichment(val status: String)
class Order(val id: Long, val status: String, val customerName: String, val city: String, val optionalCity: String?)

@Konverter
interface Mapper {
    @Konvert(mappings = [
        Mapping(target = "customerName", source = "entity.customer.name"),
        Mapping(target = "city", source = "entity.customer.address.city"),
        Mapping(target = "optionalCity", source = "customer.address.city"),
    ])
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment): Order
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toOrder(entity: OrderEntity, enrichment: OrderEnrichment): Order = Order(
                id = entity.id,
                status = enrichment.status,
                customerName = entity.customer.name,
                city = entity.customer.address?.city!!,
                optionalCity = entity.customer.address?.city
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun readNullableSourceNullSafely() {
        enforceNotNull = true
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class OrderEntity(val id: Long)
class OrderEnrichment(val status: String, val comment: String?)
class Order(val id: Long, val status: String, val comment: String?)

@Konverter
interface Mapper {
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment?): Order
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toOrder(entity: OrderEntity, enrichment: OrderEnrichment?): Order = Order(
                id = entity.id,
                status = enrichment?.status!!,
                comment = enrichment?.comment
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun failOnNullableSourceWithoutEnforceNotNull() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class OrderEntity(val id: Long)
class OrderEnrichment(val status: String)
class Order(val id: Long, val status: String)

@Konverter
interface Mapper {
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment?): Order
}
                """.trimIndent()
            )
        )

        assertContains(compilationResult.messages, "Mapping from enrichment?.status:String? to status:String without !! operator not possible.")
    }

    @Test
    fun convertSourcePropertiesThroughTypeConverters() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), EnumToEnumConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

enum class EntityStatus { OPEN, CLOSED }
enum class Status { OPEN, CLOSED }
class Line(val sku: String)
class LineDto(val sku: String)
class OrderEntity(val id: Long, val lines: List<Line>)
class OrderEnrichment(val status: EntityStatus)
class Order(val id: Long, val status: Status, val lines: List<LineDto>)

@Konverter
interface Mapper {
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment): Order
    fun toLineDto(line: Line): LineDto
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toOrder(entity: OrderEntity, enrichment: OrderEnrichment): Order = Order(
                id = entity.id,
                status = when (enrichment.status) {
                  EntityStatus.OPEN -> Status.OPEN
                  EntityStatus.CLOSED -> Status.CLOSED
                },
                lines = entity.lines.map { this.toLineDto(line = it) }
              )

              override fun toLineDto(line: Line): LineDto = LineDto(
                sku = line.sku
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun mapAdditionalParameterAlongsideTwoSources() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class OrderEntity(val id: Long, val note: String)
class OrderEnrichment(val status: String, val note: String)
class Order(val id: Long, val status: String, val note: String, val requestedBy: String)

@Konverter
interface Mapper {
    fun toOrder(
        @Konverter.Source entity: OrderEntity,
        @Konverter.Source enrichment: OrderEnrichment,
        note: String,
        requestedBy: String,
    ): Order
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
              override fun toOrder(
                entity: OrderEntity,
                enrichment: OrderEnrichment,
                note: String,
                requestedBy: String,
              ): Order = Order(
                id = entity.id,
                status = enrichment.status,
                note = note,
                requestedBy = requestedBy
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun runExpressionWithSourceParametersInScope() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class OrderEntity(val id: Long, val name: String)
class OrderEnrichment(val status: String)
class Order(val id: Long, val summary: String)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "summary", expression = "entity.name + \" \" + enrichment.status")])
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment): Order
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toOrder(entity: OrderEntity, enrichment: OrderEnrichment): Order = Order(
                id = entity.id,
                summary = run { entity.name + " " + enrichment.status }
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun mapMutableTargetPropertiesFromTwoSources() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class OrderEntity(val id: Long)
class OrderEnrichment(val status: String)
class Order(val id: Long) {
    var status: String = ""
}

@Konverter
interface Mapper {
    fun toOrder(@Konverter.Source order: OrderEntity, @Konverter.Source order0: OrderEnrichment): Order
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toOrder(order: OrderEntity, order0: OrderEnrichment): Order = Order(
                id = order.id
              ).also { order00 ->
                order00.status = order0.status
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun callSuperOfImplementedFunctionWithTwoSources() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class OrderEntity(val id: Long)
class OrderEnrichment(val status: String)
class Order(val id: Long, val status: String)

@Konverter
interface Mapper {
    fun toOrder(@Konverter.Source entity: OrderEntity, @Konverter.Source enrichment: OrderEnrichment): Order =
        Order(entity.id, enrichment.status)
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toOrder(entity: OrderEntity, enrichment: OrderEnrichment): Order = super.toOrder(
                  entity = entity,
                  enrichment = enrichment
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

}
