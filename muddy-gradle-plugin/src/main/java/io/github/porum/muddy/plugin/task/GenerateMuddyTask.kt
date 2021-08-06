package io.github.porum.muddy.plugin.task

import com.android.build.gradle.api.BaseVariant
import io.github.porum.muddy.plugin.DefaultMuddyExtension
import io.github.porum.muddy.plugin.bytecode.MuddyDump
import io.github.porum.muddy.plugin.task.factory.TaskCreationAction
import io.github.porum.muddy.plugin.utils.C
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.properties.Delegates

/**
 * Created by panda on 2021/7/31 16:08
 */
@CacheableTask
abstract class GenerateMuddyTask : DefaultTask() {

  @get:OutputDirectory
  lateinit var classesDir: File
    private set

  @get:Input
  var muddyKey by Delegates.notNull<Int>()
    private set

  @TaskAction
  fun taskAction() {
    classesDir.deleteRecursively()
    Files.createDirectories(Paths.get(classesDir.toURI()))
    val target = File(classesDir, "${C.MUDDY_CLASS}.class")
    target.ensureParentDirsCreated()
    target.writeBytes(MuddyDump.dump(muddyKey))
  }

  class CreationAction(
    private val project: Project,
    private val variant: BaseVariant,
    private val extension: DefaultMuddyExtension
  ) : TaskCreationAction<GenerateMuddyTask>() {

    private val outputClassDir: File
      get() = File(project.buildDir, "intermediates/muddy/${variant.name}/generate")

    override val name: String
      get() = "generate${variant.name.capitalize()}MuddyClass"

    override val type: Class<GenerateMuddyTask>
      get() = GenerateMuddyTask::class.java

    override fun preConfigure(taskName: String) {
      val postJavac = project.files(outputClassDir).builtBy(taskName)
      variant.registerPostJavacGeneratedBytecode(postJavac)
    }

    override fun configure(task: GenerateMuddyTask) {
      task.classesDir = outputClassDir
      task.muddyKey = extension.muddyKey
      task.dependsOn(variant.javaCompileProvider)
    }
  }
}