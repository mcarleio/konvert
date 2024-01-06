package io.mcarle.konvert.internal.docs

import io.mcarle.konvert.converter.*
import io.mcarle.konvert.converter.api.TypeConverter
import net.steppschuh.markdowngenerator.table.Table
import net.steppschuh.markdowngenerator.text.emphasis.BoldText
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Date
import java.util.ServiceLoader
import kotlin.io.path.createDirectories
import kotlin.reflect.KClass

fun main() {
    val list = ServiceLoader.load(TypeConverter::class.java).toList()

    val outputDir = Paths.get("typeconverter", "gen")
    outputDir.createDirectories()

    val basicTypesList = generateBasicTypeConverterTable(outputDir, list)
    generateTemporalTypeConverterTable(outputDir, list, basicTypesList)
    generateIterablesTypeConverterTable(outputDir, list)
    generateEnumTypeConverterTable(outputDir, list, basicTypesList)
    generateMapTypeConverterTable(outputDir, list)
}

const val FROM_TO = "From\\To"

fun generateBasicTypeConverterTable(outputDir: Path, typeConverters: List<TypeConverter>): List<KClass<*>> {
    val basicTypeConverters = typeConverters
        .filterIsInstance<BaseTypeConverter>()
        .sortedWith(kotlin.comparisons.compareBy({ it.sourceClass.simpleName }, { it.targetClass.simpleName }))

    val basicClasses = (basicTypeConverters.flatMap { listOf(it.sourceClass, it.targetClass) })
        .distinct()
        .sortedBy { it.simpleName }

    val builder = Table.Builder()
        .withAlignments(Table.ALIGN_CENTER, *basicClasses.map { Table.ALIGN_CENTER }.toTypedArray())
        .addRow(FROM_TO, *basicClasses.map { it.simpleName }.toTypedArray())

    basicClasses.forEachIndexed { index, clazz ->
        builder.addRow(
            BoldText(clazz.simpleName),
            *basicTypeConverters.filter { it.sourceClass == clazz }
                .map { if (it.enabledByDefault) "✔" else "☑" }
                .toMutableList().also {
                    it.add(index, "✔")
                }
                .toTypedArray()
        )
    }

    Files.writeString(outputDir.resolve("basic.md"), builder.build().toString() + fixedFirstColumn(120))

    return basicClasses
}

fun generateTemporalTypeConverterTable(outputDir: Path, typeConverters: List<TypeConverter>, basicTypesList: List<KClass<*>>) {
    val map = mutableMapOf<DescriptiveClass, MutableMap<DescriptiveClass, Boolean>>()
    typeConverters.filterIsInstance<TemporalToTemporalConverter>().forEach {
        map.getOrPut(DescriptiveClass.from(it, it.sourceClass)) { mutableMapOf() }[DescriptiveClass.to(it, it.targetClass)] =
            it.enabledByDefault
    }
    typeConverters.filterIsInstance<TemporalToXConverter>().forEach {
        map.getOrPut(DescriptiveClass.from(it, it.sourceClass)) { mutableMapOf() }[DescriptiveClass.to(it, it.targetClass)] =
            it.enabledByDefault
    }
    typeConverters.filterIsInstance<XToTemporalConverter>().forEach {
        map.getOrPut(DescriptiveClass.from(it, it.sourceClass)) { mutableMapOf() }[DescriptiveClass.to(it, it.targetClass)] =
            it.enabledByDefault
    }
    typeConverters.filterIsInstance<DateToTemporalConverter>().forEach {
        map.getOrPut(DescriptiveClass.from(it, Date::class)) { mutableMapOf() }[DescriptiveClass.to(it, it.targetClass)] =
            it.enabledByDefault
    }
    typeConverters.filterIsInstance<DateToXConverter>().forEach {
        map.getOrPut(DescriptiveClass.from(it, Date::class)) { mutableMapOf() }[DescriptiveClass.to(it, it.targetClass)] =
            it.enabledByDefault
    }
    typeConverters.filterIsInstance<TemporalToDateConverter>().forEach {
        map.getOrPut(DescriptiveClass.from(it, it.sourceClass)) { mutableMapOf() }[DescriptiveClass.to(it, Date::class)] =
            it.enabledByDefault
    }
    typeConverters.filterIsInstance<XToDateConverter>().forEach {
        map.getOrPut(DescriptiveClass.from(it, it.sourceClass)) { mutableMapOf() }[DescriptiveClass.to(it, Date::class)] =
            it.enabledByDefault
    }

    val from = map.keys.sortedWith(compareBy({ it.clazz.simpleName }, { it.description }))
    val to = map.values.flatMap { it.keys }.distinct().sortedWith(compareBy({ it.clazz.simpleName }, { it.description }))

    val toWithoutBasic = to.filterNot { it.clazz in basicTypesList }

    val builder = Table.Builder()
        .withAlignments(Table.ALIGN_CENTER, *toWithoutBasic.map { Table.ALIGN_CENTER }.toTypedArray())
        .addRow(FROM_TO, *toWithoutBasic.map { it.toString() }.toTypedArray())

    from.forEach { fromClass ->
        builder.addRow(
            BoldText(fromClass.toString()),
            *toWithoutBasic.map { toClass ->
                if (fromClass == toClass) return@map "✔"
                when (map[fromClass]!![toClass]) {
                    null -> null
                    true -> "✔"
                    false -> "☑"
                }
            }.toTypedArray()
        )

    }

    Files.writeString(outputDir.resolve("to_temporal.md"), builder.build().toString() + fixedFirstColumn(140))

    val fromWithoutBasic = from.filterNot { it.clazz in basicTypesList }
    val toOnlyBasic = to.filter { it.clazz in basicTypesList }

    val builder2 = Table.Builder()
        .withAlignments(Table.ALIGN_CENTER, *toOnlyBasic.map { Table.ALIGN_CENTER }.toTypedArray())
        .addRow(FROM_TO, *toOnlyBasic.map { it.toString() }.toTypedArray())

    fromWithoutBasic.forEach { fromClass ->
        builder2.addRow(
            BoldText(fromClass.toString()),
            *toOnlyBasic.map { toClass ->
                if (fromClass == toClass) return@map "✔"
                when (map[fromClass]!![toClass]) {
                    null -> null
                    true -> "✔"
                    false -> "☑"
                }
            }.toTypedArray()
        )

    }

    Files.writeString(
        outputDir.resolve("from_temporal.md"),
        builder2.build().toString() + fixedFirstColumn(140)
    )
}

fun generateIterablesTypeConverterTable(outputDir: Path, typeConverters: List<TypeConverter>) {
    val iterableToIterableConverter = typeConverters
        .filterIsInstance<IterableToIterableConverter>()
        .first()

    val supportedIterables = IterableToIterableConverter.supported().map { it.substringAfterLast(".") }.sorted()

    val builder = Table.Builder()
        .withAlignments(Table.ALIGN_CENTER, *supportedIterables.map { Table.ALIGN_CENTER }.toTypedArray())
        .addRow(FROM_TO, *supportedIterables.toTypedArray())

    supportedIterables.forEach { clazzName ->
        builder.addRow(
            BoldText(clazzName),
            *supportedIterables
                .map { if (iterableToIterableConverter.enabledByDefault) "✔" else "☑" }
                .toTypedArray()
        )
    }

    Files.writeString(outputDir.resolve("iterable.md"), builder.build().toString() + fixedFirstColumn(160))
}

fun generateMapTypeConverterTable(outputDir: Path, typeConverters: List<TypeConverter>) {
    val mapToMapConverter = typeConverters
        .filterIsInstance<MapToMapConverter>()
        .first()

    val supportedMaps = MapToMapConverter.supported().map { it.substringAfterLast(".") }.sorted()

    val builder = Table.Builder()
        .withAlignments(Table.ALIGN_CENTER, *supportedMaps.map { Table.ALIGN_CENTER }.toTypedArray())
        .addRow(FROM_TO, *supportedMaps.toTypedArray())

    supportedMaps.forEach { clazzName ->
        builder.addRow(
            BoldText(clazzName),
            *supportedMaps
                .map { if (mapToMapConverter.enabledByDefault) "✔" else "☑" }
                .toTypedArray()
        )
    }

    Files.writeString(outputDir.resolve("map.md"), builder.build().toString() + fixedFirstColumn(140))
}

fun generateEnumTypeConverterTable(outputDir: Path, typeConverters: List<TypeConverter>, basicTypesList: List<KClass<*>>) {
    val map = mutableMapOf<DescriptiveClass, MutableMap<DescriptiveClass, Boolean>>()
    typeConverters.filterIsInstance<EnumToEnumConverter>().forEach {
        map.getOrPut(DescriptiveClass.from(it, Enum::class)) { mutableMapOf() }[DescriptiveClass.to(it, Enum::class)] = it.enabledByDefault
    }
    typeConverters.filterIsInstance<EnumToXConverter>().forEach {
        map.getOrPut(DescriptiveClass.from(it, Enum::class)) { mutableMapOf() }[DescriptiveClass.to(it, it.targetClass)] =
            it.enabledByDefault
    }
    typeConverters.filterIsInstance<XToEnumConverter>().forEach {
        map.getOrPut(DescriptiveClass.from(it, it.sourceClass)) { mutableMapOf() }[DescriptiveClass.to(it, Enum::class)] =
            it.enabledByDefault
    }

    val from = map.keys.sortedWith(compareBy({ it.clazz.simpleName }, { it.description }))
    val to = map.values.flatMap { it.keys }.distinct().sortedWith(compareBy({ it.clazz.simpleName }, { it.description }))

    val toWithoutBasic = to.filterNot { it.clazz in basicTypesList }

    val builder = Table.Builder()
        .withAlignments(Table.ALIGN_CENTER, *toWithoutBasic.map { Table.ALIGN_CENTER }.toTypedArray())
        .addRow(FROM_TO, *toWithoutBasic.map { it.toString() }.toTypedArray())

    from.forEach { fromClass ->
        builder.addRow(
            BoldText(fromClass.toString()),
            *toWithoutBasic.map { toClass ->
                when (map[fromClass]!![toClass]) {
                    null -> null
                    true -> "✔"
                    false -> "☑"
                }
            }.toTypedArray()
        )
    }

    Files.writeString(outputDir.resolve("to_enum.md"), builder.build().toString() + fixedFirstColumn(120))

    val fromWithoutBasic = from.filterNot { it.clazz in basicTypesList }
    val toWithoutEnum = to.filter { it.clazz != Enum::class }

    val builder2 = Table.Builder()
        .withAlignments(Table.ALIGN_CENTER, *toWithoutEnum.map { Table.ALIGN_CENTER }.toTypedArray())
        .addRow(FROM_TO, *toWithoutEnum.map { it.toString() }.toTypedArray())

    fromWithoutBasic.forEach { fromClass ->
        builder2.addRow(
            BoldText(fromClass.toString()),
            *toWithoutEnum.map { toClass ->
                when (map[fromClass]!![toClass]) {
                    null -> null
                    true -> "✔"
                    false -> "☑"
                }
            }.toTypedArray()
        )
    }

    Files.writeString(outputDir.resolve("from_enum.md"), builder2.build().toString() + fixedFirstColumn(120))

}

data class DescriptiveClass(val description: String?, val clazz: KClass<*>) {

    companion object {
        fun from(converter: Any, sourceClass: KClass<*>) = DescriptiveClass(
            when (converter) {
                is LongEpochMillisToDateConverter -> epochMillis
                is LongEpochMillisToInstantConverter -> epochMillis
                is LongEpochSecondsToDateConverter -> epochSeconds
                is LongEpochSecondsToInstantConverter -> epochSeconds
                else -> null
            },
            sourceClass
        )

        fun to(converter: Any, targetClazz: KClass<*>) = DescriptiveClass(
            when (converter) {
                is DateToLongEpochMillisConverter -> epochMillis
                is InstantToLongEpochMillisConverter -> epochMillis
                is OffsetDateTimeToLongEpochMillisConverter -> epochMillis
                is ZonedDateTimeToLongEpochMillisConverter -> epochMillis
                is DateToLongEpochSecondsConverter -> epochSeconds
                is InstantToLongEpochSecondsConverter -> epochSeconds
                is OffsetDateTimeToLongEpochSecondsConverter -> epochSeconds
                is ZonedDateTimeToLongEpochSecondsConverter -> epochSeconds
                else -> null
            },
            targetClazz
        )
    }

    override fun toString(): String {
        return clazz.simpleName + if (description != null) " ($description)" else ""
    }
}

fun fixedFirstColumn(column: Int) = "\n{: .fixed-first-column-$column }"
const val epochMillis = "epoch ms"
const val epochSeconds = "epoch s"
