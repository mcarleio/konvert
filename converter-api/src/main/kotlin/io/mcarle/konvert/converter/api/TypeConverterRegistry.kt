package io.mcarle.konvert.converter.api

import com.google.devtools.ksp.processing.Resolver
import io.mcarle.konvert.api.TypeConverterName
import java.util.ServiceLoader

object TypeConverterRegistry : Iterable<TypeConverter> {

    private val typeConverterList = sortedMapOf<Int, MutableList<TypeConverter>>()
    private val additionallyEnabledConverters = mutableListOf<TypeConverterName>()

    val availableConverters by lazy {
        ServiceLoader.load(TypeConverter::class.java, this::class.java.classLoader).toList()
    }

    init {
        reinitConverterList(*availableConverters.toTypedArray())
    }

    /**
     * Only for tests!
     */
    fun reinitConverterList(vararg typeConverter: TypeConverter) {
        typeConverterList.clear()
        typeConverter.groupBy { it.priority }.forEach { (prio, converters) ->
            typeConverterList[prio] = converters.toMutableList()
        }
    }

    fun addConverters(order: Int, converterList: List<TypeConverter>) {
        typeConverterList[order] ?: mutableListOf<TypeConverter>().also {
            typeConverterList[order] = it
        } += converterList
    }

    fun initConverters(resolver: Resolver) {
        typeConverterList.values.flatten().forEach { it.init(resolver) }
    }

    fun <T> withAdditionallyEnabledConverters(
        converters: List<TypeConverterName>,
        codeBlock: TypeConverterRegistry.() -> T
    ): T {
        try {
            additionallyEnabledConverters.clear()
            additionallyEnabledConverters.addAll(converters)
            return codeBlock(this)
        } finally {
            additionallyEnabledConverters.clear()
        }
    }

    override fun iterator(): Iterator<TypeConverter> {
        return typeConverterList.values.flatten().filter {
            (it.enabledByDefault || it.name in additionallyEnabledConverters)
        }.iterator()
    }

}
