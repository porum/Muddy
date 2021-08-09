package com.panda912.muddy.plugin.utils

/**
 * Created by panda on 2021/7/28 10:58
 */
object Log {

  private var logImpl: ILogger = ILogger

  @JvmStatic
  fun setImpl(impl: ILogger) {
    logImpl = impl
  }

  @JvmStatic
  fun i(tag: String, msg: String) {
    logImpl.i(tag, msg)
  }

  @JvmStatic
  fun e(tag: String, msg: String, throwable: Throwable?) {
    logImpl.e(tag, msg, throwable)
  }
}


interface ILogger {
  fun i(tag: String, msg: String)
  fun e(tag: String, msg: String, throwable: Throwable?)

  companion object DEFAULT : ILogger {
    override fun i(tag: String, msg: String) {
      println("[I] $tag: $msg")
    }

    override fun e(tag: String, msg: String, throwable: Throwable?) {
      System.err.println("[E] $tag: $msg $throwable")
    }
  }
}