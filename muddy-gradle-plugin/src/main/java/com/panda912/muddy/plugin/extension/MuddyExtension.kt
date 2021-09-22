package com.panda912.muddy.plugin.extension

/**
 * Created by panda on 2021/8/4 10:41
 */
interface MuddyExtension {
  fun isEnable(enable: Boolean)
  fun setKey(muddyKey: Int)
  fun excludes(list: List<String>)
  fun includes(list: List<String>)
}