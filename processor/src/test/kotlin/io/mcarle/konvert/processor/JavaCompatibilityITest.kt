package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.BooleanToStringConverter
import io.mcarle.konvert.converter.SameTypeConverter
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class JavaCompatibilityITest : KonverterITest() {

    @Test
    fun javaGetter() {
        enforceNotNull = true
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "Address.kt",
                    contents =
                        """
import io.mcarle.konvert.api.KonvertFrom

@KonvertFrom(JavaAddress::class)
data class Address(val street: String, val verified: Boolean) {
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
    private Boolean verified = false;

    public String getStreet() {
        return street_;
    }

    public Boolean getVerified() {
        return verified;
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
              street = javaAddress.street!!,
              verified = javaAddress.verified!!
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun javaSetter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "Address.kt",
                    contents =
                        """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(JavaAddress::class, mappings = [
    Mapping(target = "id", ignore = true)
])
data class Address(val street: String, val streetNum: Int, val verified: Boolean, val primary: Boolean)
                    """.trimIndent()
                ),
                SourceFile.java(
                    name = "JavaAddress.java",
                    contents =
                        """
public class JavaAddress {
    private int id = 0;
    private String street_ = "";
    private int streetNum = 0;
    private Boolean verified = null;
    private boolean primary = false;

    public void setStreet(String street) {
        this.street_ = street;
    }

    public String getStreet() {
        return this.street_;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isPrimary() {
        return this.primary;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public JavaAddress setStreetNum(int streetNum) {
        this.streetNum = streetNum;
        return this;
    }

    public int getStreetNum() {
        return this.streetNum;
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
            public fun Address.toJavaAddress(): JavaAddress = JavaAddress().also { javaAddress ->
              javaAddress.street = street
              javaAddress.setVerified(verified)
              javaAddress.isPrimary = primary
              javaAddress.streetNum = streetNum
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun javaGetterForPrimitiveBoolean() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "Bar.kt",
                    contents =
                        """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.Konfig

data class Bar2(val bar2: Boolean)

@Konverter
interface BarMapper {
  @Konvert(
    mappings = [
      Mapping(target = "bar2", source = "isBar1"),
    ]
  )
  fun fromBar1(bar1: Bar1): Bar2

  fun fromJavaLocalDate(localDate: java.time.LocalDate): String {
    return localDate.toString()
  }
}


                    """.trimIndent()
                ),
                SourceFile.java(
                    name = "Bar1.java",
                    contents =
                        """
public class Bar1 {
    private boolean bar1;

    public Bar1(boolean bar1) {
        this.bar1 = bar1;
    }

    public void setBar1(boolean bar1) {
        this.bar1 = bar1;
    }

    public boolean isBar1() {
        return bar1;
    }

}
                """.trimIndent()
                )
            )
        )
        val mapperCode = compilation.generatedSourceFor("BarMapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import java.time.LocalDate
            import kotlin.String

            public object BarMapperImpl : BarMapper {
              override fun fromBar1(bar1: Bar1): Bar2 = Bar2(
                bar2 = bar1.isBar1
              )

              override fun fromJavaLocalDate(localDate: LocalDate): String = super.fromJavaLocalDate(localDate)
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun javaRecords() {
        enforceNotNull = true
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), BooleanToStringConverter()),
            code = arrayOf(
                SourceFile.java(
                    name = "SourceClass.java",
                    contents =
                        """
public record SourceClass(
    String street,
    String city,
    String state,
    String zip,
    Boolean international,
    boolean de
) {}
                        """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "TestCode.kt",
                    contents =
                        """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class TargetClass(
    val street: String,
    val streetNumber: String,
    val zip: String,
    val city: String,
    val country: String,
    val de: Boolean,
) {
    @KonvertFrom(SourceClass::class,
        mappings = [
            Mapping(source = "state", target = "country"),
            Mapping(source = "international", target = "streetNumber"),
        ]
    )
    companion object
}
                """.trimIndent()
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun TargetClass.Companion.fromSourceClass(sourceClass: SourceClass): TargetClass = TargetClass(
              street = sourceClass.street!!,
              streetNumber = sourceClass.international?.toString()!!,
              zip = sourceClass.zip!!,
              city = sourceClass.city!!,
              country = sourceClass.state!!,
              de = sourceClass.de
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun javaConstructor() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "SourceClass.kt",
                    contents =
                        """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings = [
    Mapping(source = "country", target = "state"),
    Mapping(target = "street", expression = "it.street + \" \" + it.streetNumber"),
])
class SourceClass(
    val street: String,
    val streetNumber: String,
    val zip: String,
    val city: String,
    val country: String,
    val primary: Boolean,
)
                """.trimIndent()
                ),
                SourceFile.java(
                    name = "TargetClass.java",
                    contents =
                        """
public class TargetClass {
    private String street;
    private String city;
    private String state;
    private String zip;
    private boolean primary;

    public TargetClass(
        String street,
        String city,
        String state,
        String zip,
        boolean primary
    ) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.primary = primary;
    }
}
                        """.trimIndent()
                ),
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              let { it.street + " " + it.streetNumber },
              city,
              country,
              zip,
              primary
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}
