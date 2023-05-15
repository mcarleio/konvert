import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.Option
import org.reflections.Reflections
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale

fun main(vararg args: String) {
    val targetFolder = Path.of(args.getOrElse(0) { "build/generated/generator/kotlin" })

    buildConfigsCode().also { generated ->
        writeToFile(targetFolder, generated, "io.mcarle.konvert.api.config")
    }
    buildConvertersCode().also { generated ->
        writeToFile(targetFolder, generated, "io.mcarle.konvert.api.converter")
    }
}

fun buildConfigsCode(): String {
    return Reflections("io.mcarle.konvert")
        .getSubTypesOf(Option::class.java)
        .filter { it.kotlin.objectInstance != null }
        .sortedBy { it.canonicalName }
        .joinToString("\n") { clazz ->
            val optionKey = clazz.kotlin.objectInstance!!.key
            val constName = optionKey.removePrefix("konvert.")
                .uppercase(Locale.getDefault())
                .replace("[^A-Z0-9]".toRegex(), "_")
                .replace("__+".toRegex(), "_")
//            val constName = clazz.simpleName.removeSuffix("_OPTION")
            """
                /**
                 * @see ${clazz.canonicalName}
                 */
                const val $constName = "$optionKey"
            """.trimIndent()
        }
}

fun buildConvertersCode(): String {
    return TypeConverterRegistry.availableConverters
        .sortedBy { it.name }
        .joinToString("\n") {
            val constName = it::class.java.simpleName
                .replace("([A-Z]+)".toRegex(), "_\$1")
                .uppercase(Locale.getDefault())
                .removePrefix("_")
            """
                /**
                 * @see ${it::class.qualifiedName}
                 */
                const val $constName = "${it.name}"
            """.trimIndent()
        }
}

fun writeToFile(kotlinDirPath: Path, content: String, packageStr: String) {
    val packagePath = kotlinDirPath.resolve(packageStr.replace(".", "/"))
    Files.createDirectories(packagePath)
    Files.writeString(
        packagePath.resolve("names.kt"),
        "package $packageStr\n\n$content",
        StandardOpenOption.CREATE, StandardOpenOption.SYNC
    )
}
