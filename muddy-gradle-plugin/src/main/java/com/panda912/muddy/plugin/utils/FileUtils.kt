package com.panda912.muddy.plugin.utils

import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Created by panda on 2021/8/17 14:56.
 */
object FileUtils {

  /**
   * Converts a /-based path into a path using the system dependent separator.
   *
   * @param path the system independent path to convert
   * @return the system dependent path
   */
  fun toSystemDependentPath(path: String): String =
    if (File.separatorChar != '/') path.replace('/', File.separatorChar) else path

  /**
   * Converts a system-dependent path into a /-based path.
   *
   * @param path the system dependent path
   * @return the system independent path
   */
  fun toSystemIndependentPath(path: String): String =
    if (File.separatorChar != '/') path.replace(File.separatorChar, '/') else path

  /**
   * Computes the relative of a file or directory with respect to a directory.
   * For example, if the file's absolute path is `/a/b/c` and the directory
   * is `/a`, this method returns `b/c`.
   *
   * @param file the path that may not correspond to any existing path in the filesystem
   * @param dir the directory to compute the path relative to
   * @return the relative path from `dir` to `file`; if `file` is a directory
   * the path comes appended with the file separator (see documentation on `relativize`
   * on java's `URI` class)
   */
  fun relativePossiblyNonExistingPath(file: File, dir: File): String =
    toSystemDependentPath(dir.toURI().relativize(file.toURI()).path)

  /**
   * eg. input directory's absolute path is `/a/b`, input file's absolute path is `/a/b/c/A.class`,
   * output directory's absolute path is @{code /d/e}, then the output file's absolute path is `/d/e/c/A.class`
   *
   * @param inputDir  input directory's absolute path.
   * @param inputFile input file's absolute path. (a fully-qualified class name)
   * @param outputDir output directory's absolute path.
   * @return output file's absolute path
   */
  fun getOutputFile(inputDir: File, inputFile: File, outputDir: File): File =
    File(outputDir, toSystemIndependentPath(relativePossiblyNonExistingPath(inputFile, inputDir)))

  /**
   * Deletes a file or an empty directory if it exists.
   *
   * @param file the file or directory to delete. The file/directory may not exist; if the
   * directory exists, it must be empty.
   */
  @Throws(IOException::class)
  fun deleteIfExists(file: File): Boolean = Files.deleteIfExists(file.toPath())

  /**
   * ensure the file's parent directories has been created.
   */
  fun ensureParentDirsCreated(file: File): Boolean =
    with(file.parentFile) {
      if (!this.exists())
        this.mkdirs()
      else
        true
    }

}