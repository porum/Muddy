package com.panda912.muddy.plugin.task.factory

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider


/**
 * Extension function for [TaskContainer] to add a way to create a task.
 */
fun <T : Task> TaskContainer.registerTask(
  creationAction: TaskCreationAction<T>,
  secondaryPreConfigAction: PreConfigAction? = null,
  secondaryAction: TaskConfigAction<in T>? = null,
  secondaryProviderCallback: TaskProviderCallback<T>? = null
): TaskProvider<T> {
  val actionWrapper = TaskConfigurationActions(
    creationAction,
    secondaryPreConfigAction,
    secondaryAction,
    secondaryProviderCallback
  )
  return this.register(creationAction.name, creationAction.type, actionWrapper)
    .also { provider ->
      actionWrapper.postRegisterHook(provider)
    }
}


/**
 * Wrapper for the [VariantTaskCreationAction] as a simple [Action] that is passed
 * to [TaskContainer.register].
 *
 * If the task is configured during the register then [VariantTaskCreationAction.preConfigure] is called
 * right away.
 *
 * After register, if it has not been called then it is called,
 * alongside [VariantTaskCreationAction.handleProvider]
 */
internal class TaskConfigurationActions<T : Task>(
  private val creationAction: TaskCreationAction<T>? = null,
  private val preConfigAction: PreConfigAction? = null,
  private val configureAction: TaskConfigAction<in T>? = null,
  private val providerHandler: TaskProviderCallback<T>? = null
) : Action<T> {

  var hasRunTaskProviderHandler = false
  var delayedTask: T? = null

  override fun execute(task: T) {
    // if we have not yet processed the task provider handle, then we delay this
    // to the post register hook
    if (hasRunTaskProviderHandler) {
      creationAction?.configure(task)
      configureAction?.configure(task)
    } else {
      delayedTask = task
    }
  }

  fun postRegisterHook(taskProvider: TaskProvider<T>) {
    creationAction?.preConfigure(taskProvider.name)
    preConfigAction?.preConfigure(taskProvider.name)

    creationAction?.handleProvider(taskProvider)
    providerHandler?.handleProvider(taskProvider)

    delayedTask?.let {
      creationAction?.configure(it)
      configureAction?.configure(it)
    }

    hasRunTaskProviderHandler = true
  }
}
