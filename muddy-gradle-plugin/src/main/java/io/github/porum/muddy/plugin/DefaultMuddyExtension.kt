package io.github.porum.muddy.plugin

/**
 * Created by panda on 2021/8/4 10:43
 */
open class DefaultMuddyExtension : MuddyExtension {

  var enable: Boolean = true
  var muddyKey: Int = 2021

  override fun isEnable(enable: Boolean) {
    this.enable = enable
  }

  override fun setKey(muddyKey: Int) {
    this.muddyKey = muddyKey
  }
}