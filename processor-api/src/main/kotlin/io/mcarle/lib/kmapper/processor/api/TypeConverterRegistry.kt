package io.mcarle.lib.kmapper.processor.api

import java.util.*
import kotlin.reflect.KClass

object TypeConverterRegistry : Iterable<TypeConverter> {

    private val typeConverterList = sortedMapOf<Int, MutableList<TypeConverter>>()
    private var additionallyEnabledConvertersThreadLocal = ThreadLocal.withInitial {
        listOf<KClass<out TypeConverter>>()
    }

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

    fun initConverters(converterConfig: ConverterConfig) {
        typeConverterList.values.flatten().forEach { it.init(converterConfig) }
    }

    fun <T> withAdditionallyEnabledConverters(
        converters: List<KClass<out TypeConverter>>,
        codeBlock: TypeConverterRegistry.() -> T
    ): T {
        try {
            additionallyEnabledConvertersThreadLocal.set(converters)
            return codeBlock(this)
        } finally {
            additionallyEnabledConvertersThreadLocal.remove()
        }
    }

    override fun iterator(): Iterator<TypeConverter> {
        val additionallyEnabledConverters = additionallyEnabledConvertersThreadLocal.get()
        return typeConverterList.values.flatten().filter {
            (it.enabledByDefault || it::class in additionallyEnabledConverters)
        }.iterator()
    }

}