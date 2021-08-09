package com.panda912.muddy.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.panda912.muddy.plugin.task.GenerateMuddyTask
import com.panda912.muddy.plugin.task.factory.registerTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by panda on 2021/7/28 9:08
 */
class MuddyPlugin : Plugin<Project> {

  override fun apply(target: Project) {

    val muddyExt = target.extensions.create(
      MuddyExtension::class.java,
      "muddy",
      DefaultMuddyExtension::class.java
    ) as DefaultMuddyExtension

    when (val extension = target.extensions.getByName("android")) {
      is AppExtension -> {
        extension.registerTransform(MuddyTransform(muddyExt, false))

        target.afterEvaluate {
          extension.applicationVariants.configureEach {
            target.tasks.registerTask(GenerateMuddyTask.CreationAction(target, it, muddyExt))
          }
        }
      }
      is LibraryExtension -> {
        extension.registerTransform(MuddyTransform(muddyExt, true))
      }
      else -> throw GradleException("Muddy Plugin, Android Application/Library plugin required.")
    }
  }
}