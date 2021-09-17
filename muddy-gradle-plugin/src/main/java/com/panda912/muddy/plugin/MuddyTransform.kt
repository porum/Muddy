package com.panda912.muddy.plugin

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.panda912.muddy.plugin.bytecode.ModifyClassVisitor
import com.panda912.muddy.plugin.utils.Log
import com.panda912.muddy.plugin.utils.Util
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

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
    Log.i(
      name,
      "isIncremental: $isIncremental, key: ${extension.muddyKey}, enable: ${extension.enable}"
    )

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
          input.walkBottomUp().toList().parallelStream().filter { it.isFile }.forEach {
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
      if (input.isDirectory) { // directory
        if (isIncremental) {
          changedFiles.filter { it.key.startsWith(input) }.forEach { (changedFile, status) ->
            val outputClassFile = Util.getOutputFile(input, changedFile, output)
            when (status) {
              Status.ADDED,
              Status.CHANGED -> {
                modifyBytecode(changedFile, outputClassFile)
              }
              Status.REMOVED -> FileUtils.deleteIfExists(outputClassFile)
            }
          }
        } else {
          // delete old directories
          output.deleteRecursively()
          // find all class files, exclude Muddy.class
          input.walkBottomUp().toList().parallelStream().filter { it.isFile }.forEach {
            val outputClassFile = Util.getOutputFile(input, it, output)
            if (it.name.endsWith(SdkConstants.DOT_CLASS) && it.nameWithoutExtension != "Muddy") {
              Util.ensureParentDirsCreated(outputClassFile)
              modifyBytecode(it, outputClassFile)
            } else {
              it.copyTo(outputClassFile, true)
            }
          }
        }
      } else { // jar
        if (isIncremental) {
          when (changedFiles[input]) {
            Status.ADDED,
            Status.CHANGED -> {
              FileUtils.deleteIfExists(output)
              handleJar(input, output)
            }
            Status.REMOVED -> FileUtils.deleteIfExists(output)
          }
        } else {
          FileUtils.deleteIfExists(output)
          handleJar(input, output)
        }
      }
    }
  }

  @Throws(IOException::class)
  private fun handleJar(inputJar: File, outputJar: File) {
    val jarFile = JarFile(inputJar)
    val jos = JarOutputStream(outputJar.outputStream().buffered())
    jarFile.entries().iterator().forEachRemaining { entry ->
      if (!entry.isDirectory) {
        val inputStream = jarFile.getInputStream(entry)
        val bytes = if (entry.name.endsWith(".class")) {
          getModifiedJarClass(inputStream)
        } else {
          inputStream.readBytes()
        }
        jos.putNextEntry(JarEntry(entry.name))
        jos.write(bytes)
      }
    }
    jos.finish()
    jos.flush()
    jos.close()
  }

  private fun getModifiedJarClass(inputStream: InputStream): ByteArray {
    val cr = ClassReader(inputStream)
    val cw = ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
    val cv = ModifyClassVisitor(Opcodes.ASM7, cw, extension.muddyKey)
    cr.accept(cv, 0)
    return cw.toByteArray()
  }

  /**
   * modify input class and then output to dist file
   */
  @Throws(IOException::class)
  private fun modifyBytecode(input: File, output: File) {
    val cr = ClassReader(FileInputStream(input))
    val cw = ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
    val cv = ModifyClassVisitor(Opcodes.ASM7, cw, extension.muddyKey)
    cr.accept(cv, 0)
    output.writeBytes(cw.toByteArray())
  }

}