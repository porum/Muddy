package com.panda912.muddy.sample

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

  companion object {
    private const val TAG = "MainActivity"
  }

  private val TAG = "MainActivity_TAG"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    findViewById<TextView>(R.id.textview).text = TAG


    val array = arrayOf("panda", "asm", "muddy", "gradle-plugin")

    val arr = arrayOf(
      java.lang.String.format("name: %s", "panda")
    )
  }
}