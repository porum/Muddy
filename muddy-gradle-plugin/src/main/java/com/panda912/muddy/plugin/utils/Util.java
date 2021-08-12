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

package com.panda912.muddy.plugin.utils;

import com.android.annotations.NonNull;
import com.android.utils.FileUtils;

import java.io.File;

/**
 * {@link FileUtils}
 * <p>
 * Created by panda on 2018/9/18 下午1:21.
 */
public class Util {

  /**
   * Converts a /-based path into a path using the system dependent separator.
   *
   * @param path the system independent path to convert
   * @return the system dependent path
   */
  @NonNull
  public static String toSystemDependentPath(@NonNull String path) {
    if (File.separatorChar != '/') {
      path = path.replace('/', File.separatorChar);
    }
    return path;
  }

  @NonNull
  public static String toSystemDependentPath(@NonNull File file) {
    String path = file.getAbsolutePath();
    if (File.separatorChar != '/') {
      path = path.replace('/', File.separatorChar);
    }
    return path;
  }

  /**
   * Converts a system-dependent path into a /-based path.
   *
   * @param path the system dependent path
   * @return the system independent path
   */
  @NonNull
  public static String toSystemIndependentPath(@NonNull String path) {
    if (File.separatorChar != '/') {
      path = path.replace(File.separatorChar, '/');
    }
    return path;
  }

  @NonNull
  public static String toSystemIndependentPath(@NonNull File file) {
    String path = file.getAbsolutePath();
    if (File.separatorChar != '/') {
      path = path.replace(File.separatorChar, '/');
    }
    return path;
  }

  /**
   * eg. input directory's absolute path is {@code /a/b}, input file's absolute path is {@code /a/b/c/A.class},
   * output directory's absolute path is @{code /d/e}, then the output file's absolute path is {@code /d/e/c/A.class}
   *
   * @param inputDir  input directory's absolute path.
   * @param inputFile input file's absolute path. (a fully-qualified class name)
   * @param outputDir output directory's absolute path.
   * @return output file's absolute path
   */
  public static File getOutputFile(File inputDir, File inputFile, File outputDir) {
    String relativePath = getRelativePath(inputFile, inputDir);
    return new File(outputDir, relativePath);
  }

  public static String getRelativePath(File file, File rootPath) {
    String relativePath = FileUtils.relativePossiblyNonExistingPath(file, rootPath);
    return FileUtils.toSystemIndependentPath(relativePath);
  }

  public static boolean ensureParentDirsCreated(File file) {
    File parent = file.getParentFile();
    if (!parent.exists()) {
      return parent.mkdirs();
    }
    return true;
  }
}
