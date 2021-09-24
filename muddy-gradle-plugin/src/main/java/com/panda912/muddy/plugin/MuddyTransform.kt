package com.panda912.muddy.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.panda912.muddy.plugin.extension.DefaultMuddyExtension
import com.panda912.muddy.plugin.utils.FileUtils
import com.panda912.muddy.plugin.utils.MUDDY_CLASS
import com.panda912.muddy.plugin.utils.toInternalName
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.io.File
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

  private lateinit var includes: List<String>
  private lateinit var excludes: List<String>

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
            val outputClassFile = FileUtils.getOutputFile(input, changedFile, output)
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
            val outputClassFile = FileUtils.getOutputFile(input, it, output)
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
    includes = extension.includes.map { it.replace(".", "/") }
    excludes = extension.excludes.map { it.replace(".", "/") }

    io.forEach { (input, output) ->
      if (input.isDirectory) { // directory
        if (isIncremental) {
          changedFiles.filter { it.key.startsWith(input) }.forEach { (changedFile, status) ->
            val outputClassFile = FileUtils.getOutputFile(input, changedFile, output)
            when (status) {
              Status.ADDED,
              Status.CHANGED -> {
                outputClassFile.writeBytes(getModifiedClass(changedFile.inputStream()))
              }
              Status.REMOVED -> FileUtils.deleteIfExists(outputClassFile)
            }
          }
        } else {
          // delete old directories
          output.deleteRecursively()
          // find all class files, exclude Muddy.class
          input.walkBottomUp().toList().parallelStream().filter { it.isFile }.forEach {
            val outputClassFile = FileUtils.getOutputFile(input, it, output)
            if (it.name.endsWith(".class") && it.nameWithoutExtension != "Muddy") {
              FileUtils.ensureParentDirsCreated(outputClassFile)
              outputClassFile.writeBytes(getModifiedClass(it.inputStream()))
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
          getModifiedClass(inputStream)
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

  @Throws(IOException::class)
  private fun getModifiedClass(inputStream: InputStream): ByteArray {
    val cr = ClassReader(inputStream)
    val cn = ClassNode(Opcodes.ASM9)
    cr.accept(cn, 0)

    val className = cr.className

    if (includes.isEmpty() && excludes.isEmpty()) {
      modifyBytecode(cn)
    } else if (includes.isNotEmpty()) {
      if (includes.any { className.startsWith(it) }) { // class in includes list
        modifyBytecode(cn)
      }
    } else if (excludes.isNotEmpty()) {
      if (!excludes.any { className.startsWith(it) }) { // class not in excludes list
        modifyBytecode(cn)
      }
    }

    val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
    cn.accept(cw)
    return cw.toByteArray()
  }

  private fun modifyBytecode(cn: ClassNode) {
    val constFields: List<FieldNode> = cn.fields.filter {
      it.desc == "Ljava/lang/String;" && !(it.value as? String).isNullOrEmpty() &&
          (it.access == Opcodes.ACC_STATIC + Opcodes.ACC_FINAL ||
              it.access == Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL ||
              it.access == Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL ||
              it.access == Opcodes.ACC_PROTECTED + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL)
    }

    for (method in cn.methods) {
      for (insn in method.instructions.toArray()) {
        if (insn is LdcInsnNode && insn.cst is String) {
          insn.cst = Muddy.xor(insn.cst as String, extension.muddyKey)
          val muddyMethodInsn = MethodInsnNode(
            Opcodes.INVOKESTATIC,
            MUDDY_CLASS.toInternalName(),
            "xor",
            "(Ljava/lang/String;)Ljava/lang/String;",
            false
          )
          method.instructions.insert(insn, muddyMethodInsn)
        }
      }
    }

    if (constFields.isNotEmpty()) {
      val clinit = cn.methods.find { it.name == "<clinit>" }
        ?: MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)

      val insnList = InsnList()
      for (field in constFields) {
        insnList.add(LdcInsnNode(Muddy.xor(field.value as String, extension.muddyKey)))
        insnList.add(
          MethodInsnNode(
            Opcodes.INVOKESTATIC,
            MUDDY_CLASS.toInternalName(),
            "xor",
            "(Ljava/lang/String;)Ljava/lang/String;",
            false
          )
        )
        insnList.add(
          FieldInsnNode(
            Opcodes.PUTSTATIC,
            cn.name,
            field.name,
            "Ljava/lang/String;"
          )
        )
        field.value = null
      }
      insnList.add(clinit.instructions)
      if (insnList.last.type != AbstractInsnNode.INSN || insnList.last.opcode != Opcodes.RETURN) {
        insnList.add(InsnNode(Opcodes.RETURN))
      }
      clinit.instructions = insnList
      cn.methods.removeIf { it.name == "<clinit>" }
      cn.methods.add(clinit)
    }
  }

}