package com.panda912.muddy.plugin

import com.panda912.muddy.plugin.utils.MUDDY_CLASS
import com.panda912.muddy.plugin.utils.toInternalName
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type


/**
 * Created by panda on 2021/8/3 11:05
 */
object MuddyDump {

  @JvmStatic
  fun dump(key: Int): ByteArray {

    val cw = ClassWriter(0).apply {
      visit(
        Opcodes.V1_7,
        Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
        MUDDY_CLASS.toInternalName(),
        null,
        Type.getInternalName(Object::class.java),
        null
      )
    }

    val fv = cw.visitField(
      Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL or Opcodes.ACC_STATIC,
      "constMap",
      "Ljava/util/Map;",
      "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
      null
    )
    fv.visitEnd()

    with(cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null)) {
      visitCode()
      visitVarInsn(Opcodes.ALOAD, 0)
      visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        Type.getInternalName(Object::class.java),
        "<init>",
        "()V",
        false
      )
      visitInsn(Opcodes.RETURN)
      visitMaxs(1, 1)
      visitEnd()
    }

    with(
      cw.visitMethod(
        Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
        "xor",
        "(Ljava/lang/String;)Ljava/lang/String;",
        null,
        null
      )
    ) {
      visitCode()
      visitFieldInsn(Opcodes.GETSTATIC, MUDDY_CLASS.toInternalName(), "constMap", "Ljava/util/Map;")
      visitVarInsn(Opcodes.ALOAD, 0)
      visitMethodInsn(
        Opcodes.INVOKEINTERFACE,
        "java/util/Map",
        "get",
        "(Ljava/lang/Object;)Ljava/lang/Object;",
        true
      )
      visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
      visitVarInsn(Opcodes.ASTORE, 1)
      visitVarInsn(Opcodes.ALOAD, 1)
      val label0 = Label()
      visitJumpInsn(Opcodes.IFNULL, label0)
      visitVarInsn(Opcodes.ALOAD, 1)
      visitInsn(Opcodes.ARETURN)
      visitLabel(label0)
      visitVarInsn(Opcodes.ALOAD, 0)
      visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false)
      visitVarInsn(Opcodes.ASTORE, 2)
      visitInsn(Opcodes.ICONST_0)
      visitVarInsn(Opcodes.ISTORE, 3)
      val label1 = Label()
      visitLabel(label1)
      visitVarInsn(Opcodes.ILOAD, 3)
      visitVarInsn(Opcodes.ALOAD, 2)
      visitInsn(Opcodes.ARRAYLENGTH)
      val label2 = Label()
      visitJumpInsn(Opcodes.IF_ICMPGE, label2)
      visitVarInsn(Opcodes.ALOAD, 2)
      visitVarInsn(Opcodes.ILOAD, 3)
      visitVarInsn(Opcodes.ALOAD, 2)
      visitVarInsn(Opcodes.ILOAD, 3)
      visitInsn(Opcodes.CALOAD)
      visitIntInsn(Opcodes.SIPUSH, key)
      visitInsn(Opcodes.IXOR)
      visitInsn(Opcodes.I2C)
      visitInsn(Opcodes.CASTORE)
      visitIincInsn(3, 1)
      visitJumpInsn(Opcodes.GOTO, label1)
      visitLabel(label2)
      visitTypeInsn(Opcodes.NEW, "java/lang/String")
      visitInsn(Opcodes.DUP)
      visitVarInsn(Opcodes.ALOAD, 2)
      visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false)
      visitVarInsn(Opcodes.ASTORE, 1)
      visitFieldInsn(Opcodes.GETSTATIC, MUDDY_CLASS.toInternalName(), "constMap", "Ljava/util/Map;")
      visitVarInsn(Opcodes.ALOAD, 0)
      visitVarInsn(Opcodes.ALOAD, 1)
      visitMethodInsn(
        Opcodes.INVOKEINTERFACE,
        "java/util/Map",
        "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
        true
      )
      visitInsn(Opcodes.POP)
      visitVarInsn(Opcodes.ALOAD, 1)
      visitInsn(Opcodes.ARETURN)
      visitMaxs(4, 4)
      visitEnd()
    }

    with(cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)) {
      visitCode()
      visitTypeInsn(Opcodes.NEW, "java/util/HashMap")
      visitInsn(Opcodes.DUP)
      visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false)
      visitFieldInsn(Opcodes.PUTSTATIC, MUDDY_CLASS.toInternalName(), "constMap", "Ljava/util/Map;")
      visitInsn(Opcodes.RETURN)
      visitMaxs(2, 0)
      visitEnd()
    }

    cw.visitEnd()

    return cw.toByteArray()
  }
}