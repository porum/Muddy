package com.panda912.muddy.plugin.utils

/**
 * Created by panda on 2021/9/18 10:38
 */

val MUDDY_CLASS = "com.panda912.muddy.lib.Muddy"

fun String.toInternalName() = this.replace(".", "/")