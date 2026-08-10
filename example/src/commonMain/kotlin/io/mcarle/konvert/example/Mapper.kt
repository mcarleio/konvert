package io.mcarle.konvert.example

import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.config.KONVERTER_GENERATE_CLASS
import io.mcarle.konvert.api.converter.LONG_TO_UINT_CONVERTER
import io.mcarle.konvert.api.converter.STRING_TO_INT_CONVERTER

@Konverter
interface AMapper {
    fun map(domain: Domain): DTO
    fun map(domains: Collection<Domain>): List<DTO>
}

data class Domain(val prop: String)
data class DTO(val prop: String)
