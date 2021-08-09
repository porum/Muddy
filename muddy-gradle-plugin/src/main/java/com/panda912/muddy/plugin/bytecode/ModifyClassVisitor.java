/*
 * Copyright 2018 panda912
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.panda912.muddy.plugin.bytecode;

import com.panda912.muddy.plugin.utils.C;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by panda on 2018/8/29 下午4:13.
 */
public class ModifyClassVisitor extends ClassVisitor implements Opcodes {

  private String owner;

  /**
   * save the const field of [private/protected/public static final String]
   */
  private Map<String, String> constFieldMap;

  private boolean clinitExist = false;

  private final int muddyKey;

  public ModifyClassVisitor(int api, ClassVisitor cv, int key) {
    super(api, cv);
    this.muddyKey = key;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    owner = name;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    if (C.STRING.equals(desc)) {
      if (access == ACC_PUBLIC + ACC_STATIC + ACC_FINAL ||
          access == ACC_PRIVATE + ACC_STATIC + ACC_FINAL ||
          access == ACC_PROTECTED + ACC_STATIC + ACC_FINAL) {
        if (value != null && ((String) value).length() != 0) {
          if (constFieldMap == null) {
            constFieldMap = new HashMap<>();
          }
          constFieldMap.put(name, Muddy.xor((String) value, muddyKey));
        }
        return super.visitField(access, name, desc, signature, null);
      }
    }
    return super.visitField(access, name, desc, signature, value);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    clinitExist = C.CLINIT.equals(name) && !clinitExist;

    MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
    return new ModifyConstVisitor(ASM5, mv, owner, name, constFieldMap, muddyKey);
  }

  @Override
  public void visitEnd() {
    if (!clinitExist && constFieldMap != null) {
      MethodVisitor mv = cv.visitMethod(ACC_STATIC, C.CLINIT, "()V", null, null);
      constFieldMap.forEach((key, value) -> {
        mv.visitLdcInsn(value);
        mv.visitMethodInsn(INVOKESTATIC, C.MUDDY_CLASS, "xor", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitFieldInsn(PUTSTATIC, owner, key, "Ljava/lang/String;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
      });
    }
    super.visitEnd();
  }
}
