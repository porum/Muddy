package com.panda912.muddy.plugin

import com.android.build.gradle.AppExtension
import com.ss.android.ugc.bytex.common.BaseContext
import org.gradle.api.Project

/**
 * Created by panda on 2022/4/26 16:34
 */
class MuddyContext(
  project: Project?,
  android: AppExtension?,
  extension: MuddyExtension?
) : BaseContext<MuddyExtension>(project, android, extension)