package io.mcarle.lib.kmapper.processor.converter.annotated

import io.mcarle.lib.kmapper.processor.converter.StringToIntConverter
import org.junit.jupiter.api.Test


class KMapToTest : ConverterMapToITest() {

    @Test
    fun test() {
        super.converterTest(
            converter = StringToIntConverter(),
            sourceTypeName = "String",
            targetTypeName = "Int"
        )
    }

}