import dev.architectury.at.io.AccessTransformFormats
import dev.architectury.loom.accesstransformer.Aw2At
import dev.architectury.loom.util.LfWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files

abstract class ConvertAwToAtTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val accessWidener: RegularFileProperty

    @get:OutputFile
    abstract val accessTransformer: RegularFileProperty

    @TaskAction
    fun convert() {
        val accessWidenerPath = accessWidener.get().asFile.toPath()
        val accessTransformerPath = accessTransformer.get().asFile.toPath()

        val parent = accessTransformerPath.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }

        Files.newBufferedReader(accessWidenerPath, StandardCharsets.UTF_8).use { reader ->
            val accessTransformSet = Aw2At.toAccessTransformSet(reader)

            LfWriter(Files.newBufferedWriter(accessTransformerPath, StandardCharsets.UTF_8)).use { writer ->
                AccessTransformFormats.FML.write(writer, accessTransformSet)
            }
        }
    }
}
