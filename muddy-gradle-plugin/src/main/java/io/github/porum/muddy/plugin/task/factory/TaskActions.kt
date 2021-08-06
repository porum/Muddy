package io.github.porum.muddy.plugin.task.factory

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider


/**
 * Basic task information for creation
 */
interface TaskInformation<TaskT: Task> {
  /** The name of the task to be created.  */
  val name: String

  /** The class type of the task to created.  */
  val type: Class<TaskT>
}

/** Lazy Creation Action for non variant aware tasks
 *
 * This contains both meta-data to create the task ([name], [type])
 * and actions to configure the task ([preConfigure], [configure], [handleProvider])
 */
abstract class TaskCreationAction<TaskT : Task> : TaskInformation<TaskT>, PreConfigAction,
  TaskConfigAction<TaskT>, TaskProviderCallback<TaskT> {

  override fun preConfigure(taskName: String) {
    // default does nothing
  }

  override fun handleProvider(taskProvider: TaskProvider<TaskT>) {
    // default does nothing
  }
}

/**
 * Configuration Action for tasks.
 */
interface TaskConfigAction<TaskT: Task> {

  /** Configures the task. */
  fun configure(task: TaskT)
}

/**
 * Pre-Configuration Action for lazily created tasks.
 */
interface PreConfigAction {
  /**
   * Pre-configures the task, acting on the taskName.
   *
   * This is meant to handle configuration that must happen always, even when the task
   * is configured lazily.
   *
   * @param taskName the task name
   */
  fun preConfigure(taskName: String)
}

/**
 * Callback for [TaskProvider]
 *
 * Once a TaskProvider is created this is called to process it.
 */
interface TaskProviderCallback<TaskT: Task> {
  fun handleProvider(taskProvider: TaskProvider<TaskT>)
}