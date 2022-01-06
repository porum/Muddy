package com.panda912.muddy.plugin

import com.android.build.api.extension.AndroidComponentsExtension
import com.android.build.api.instrumentation.*
import com.android.build.api.instrumentation.InstrumentationScope.ALL
import com.android.build.api.instrumentation.InstrumentationScope.PROJECT
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.LibraryVariant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor

/**
 * Created by panda on 2021/7/28 9:08
 */
@Suppress("UnstableApiUsage")
class MuddyPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val android = project.extensions.getByType(AndroidComponentsExtension::class.java)
    val extension = project.extensions.create("muddy", MuddyExtension::class.java)
    android.onVariants(android.selector().withBuildType("release")) { variant ->
      if (variant !is ApplicationVariant && variant !is LibraryVariant) {
        throw GradleException("muddy not support current variant: $variant")
      }
      val scope = if (variant is ApplicationVariant) ALL else PROJECT
      variant.transformClassesWith(MuddyClassVisitorFactory::class.java, scope) {
        it.includes.set(extension.includes)
      }
      variant.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)
    }
  }
}

@Suppress("UnstableApiUsage")
interface FilterParamsImpl : InstrumentationParameters {

  @get:Input
  val includes: ListProperty<String>
}

@Suppress("UnstableApiUsage")
abstract class MuddyClassVisitorFactory : AsmClassVisitorFactory<FilterParamsImpl> {
  override fun createClassVisitor(
    classContext: ClassContext,
    nextClassVisitor: ClassVisitor
  ): ClassVisitor {
    return MuddyClassVisitor(nextClassVisitor)
  }

  override fun isInstrumentable(classData: ClassData): Boolean {
    val includes = parameters.get().includes.get()
    if (includes.isNotEmpty()) {
      return includes.any { classData.className.startsWith(it) }
    }
    return false
  }
}