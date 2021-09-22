package com.panda912.muddy.plugin.extension

/**
 * Created by panda on 2021/8/4 10:43
 */
open class DefaultMuddyExtension : MuddyExtension {

  var enable: Boolean = true
  var muddyKey: Int = 2021
  var excludes = listOf<String>()
  var includes = listOf<String>()

  override fun isEnable(enable: Boolean) {
    this.enable = enable
  }

  override fun setKey(muddyKey: Int) {
    this.muddyKey = muddyKey
  }

  override fun excludes(list: List<String>) {
    excludes = list
  }

  override fun includes(list: List<String>) {
    includes = list
  }
}