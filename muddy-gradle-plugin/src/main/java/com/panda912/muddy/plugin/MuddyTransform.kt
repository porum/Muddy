package com.panda912.muddy.plugin

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.panda912.muddy.plugin.bytecode.ModifyClassVisitor
import com.panda912.muddy.plugin.utils.Log
import com.panda912.muddy.plugin.utils.Util
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by panda on 2021/7/28 9:10
 */
class MuddyTransform(
  private val extension: DefaultMuddyExtension,
  private val isLibrary: Boolean
) : Transform() {

  override fun getName(): String {
    return "muddy"
  }

  override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
    return TransformManager.CONTENT_CLASS
  }

  override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
    return if (isLibrary) TransformManager.PROJECT_ONLY else TransformManager.SCOPE_FULL_PROJECT
  }

  override fun isIncremental(): Boolean {
    return true
  }

  override fun transform(transformInvocation: TransformInvocation) {
    super.transform(transformInvocation)

    val isIncremental = transformInvocation.isIncremental && isIncremental
    Log.i(name, "isIncremental: $isIncremental, key: ${extension.muddyKey}, enable: ${extension.enable}")

    // directory or jar path
    val io = ConcurrentHashMap<File, File>()
    val changedFiles = ConcurrentHashMap<File, Status>()

    for (input in transformInvocation.inputs) {
      for (directoryInput in input.directoryInputs) {
        val inputDir = directoryInput.file
        val outputDir = transformInvocation.outputProvider.getContentLocation(
          directoryInput.name,
          directoryInput.contentTypes,
          directoryInput.scopes,
          Format.DIRECTORY
        )

        io[inputDir] = outputDir
        changedFiles.putAll(directoryInput.changedFiles)
      }

      for (jarInput in input.jarInputs) {
        val inputJar = jarInput.file
        val outputJar = transformInvocation.outputProvider.getContentLocation(
          jarInput.name,
          jarInput.contentTypes,
          jarInput.scopes,
          Format.JAR
        )

        io[inputJar] = outputJar
        changedFiles[inputJar] = jarInput.status
      }
    }

    if (extension.enable) {
      doTransform(io, changedFiles, isIncremental)
    } else {
      transparent(io, changedFiles, isIncremental)
    }
  }

  private fun transparent(
    io: Map<File, File>,
    changedFiles: Map<File, Status>,
    isIncremental: Boolean
  ) {
    io.forEach { (input, output) ->
      if (input.isDirectory) {
        if (isIncremental) {
          changedFiles.filter { it.key.startsWith(input) }.forEach { (changedFile, status) ->
            val outputClassFile = Util.getOutputFile(input, changedFile, output)
            when (status) {
              Status.ADDED,
              Status.CHANGED -> {
                changedFile.copyTo(outputClassFile, true)
              }
              Status.REMOVED -> FileUtils.deleteIfExists(outputClassFile)
            }
          }
        } else {
          output.deleteRecursively()
          input.walkBottomUp().filter { it.isFile }.forEach {
            val outputClassFile = Util.getOutputFile(input, it, output)
            it.copyTo(outputClassFile)
          }
        }
      } else {
        output.deleteRecursively()
        input.copyRecursively(output, true)
      }
    }
  }

  private fun doTransform(
    io: Map<File, File>,
    changedFiles: Map<File, Status>,
    isIncremental: Boolean
  ) {
    io.forEach { (input, output) ->
      if (input.isDirectory) {
        if (isIncremental) {
          changedFiles.filter { it.key.startsWith(input) }.forEach { (changedFile, status) ->
            val outputClassFile = Util.getOutputFile(input, changedFile, output)
            when (status) {
              Status.ADDED,
              Status.CHANGED -> {
                //                Log.i(name, "input: $input")
                //                Log.i(name, "output: $output")
                //                Log.i(name, "changedFile: $changedFile")
                //                Log.i(name, "outputClassFile: $outputClassFile")
                generateNewClass(changedFile, outputClassFile)
              }
              Status.REMOVED -> FileUtils.deleteIfExists(outputClassFile)
            }
          }
        } else {
          // delete old directories
          output.deleteRecursively()
          // find all class files, but except Muddy.class
          input.walkBottomUp().filter { it.isFile }.forEach {
            //            Log.i(name, "classFile: $it")
            val outputClassFile = Util.getOutputFile(input, it, output)
            //            Log.i(name, "output: $outputClassFile")
            if (it.name.endsWith(SdkConstants.DOT_CLASS) && it.nameWithoutExtension != "Muddy") {
              outputClassFile.ensureParentDirsCreated()
              generateNewClass(it, outputClassFile)
            } else {
              it.copyTo(outputClassFile, true)
            }
          }
        }
      } else {
        output.deleteRecursively()
        input.copyRecursively(output, true)
      }
    }
  }

  /**
   * modify input class and then output to dist file
   */
  @Throws(IOException::class)
  private fun generateNewClass(input: File, output: File) {
    val cr = ClassReader(FileInputStream(input))
    val cw = ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
    val cv = ModifyClassVisitor(Opcodes.ASM5, cw, extension.muddyKey)
    cr.accept(cv, Opcodes.ASM5)
    output.writeBytes(cw.toByteArray())
  }

}