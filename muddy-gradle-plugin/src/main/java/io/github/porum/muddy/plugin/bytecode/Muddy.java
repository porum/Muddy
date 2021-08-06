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

package io.github.porum.muddy.plugin.bytecode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by panda on 2018/9/7 下午5:19.
 */
public class Muddy {

  private static final Map<String, String> constMap = new HashMap<>();

  private Muddy() {
  }

  public static String xor(String plainText, int key) {
    String result = constMap.get(plainText);
    if (result != null) {
      return result;
    }

    char[] ch = plainText.toCharArray();
    for (int i = 0; i < ch.length; i++) {
      ch[i] = (char) (ch[i] ^ key);
    }
    result = new String(ch);
    constMap.put(plainText, result);
    return result;
  }

//  public static String xor(String plainText) {
//    String result = constMap.get(plainText);
//    if (result != null) {
//      return result;
//    }
//
//    char[] ch = plainText.toCharArray();
//    for (int i = 0; i < ch.length; i++) {
//      ch[i] = (char) (ch[i] ^ 2012);
//    }
//    result = new String(ch);
//    constMap.put(plainText, result);
//    return result;
//  }
}
