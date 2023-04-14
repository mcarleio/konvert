package io.mcarle.konvert.converter.api

import com.google.devtools.ksp.processing.Resolver
import java.util.ServiceLoader
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

    fun initConverters(resolver: Resolver) {
        typeConverterList.values.flatten().forEach { it.init(resolver) }
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
