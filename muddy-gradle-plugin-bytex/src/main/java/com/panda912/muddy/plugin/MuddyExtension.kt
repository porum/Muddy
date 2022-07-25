package com.panda912.muddy.plugin

import com.ss.android.ugc.bytex.common.BaseExtension

/**
 * Created by panda on 2021/12/31 16:55
 */
open class MuddyExtension : BaseExtension() {

  override fun getName() = "muddy"

  var includes: List<String> = emptyList()
}