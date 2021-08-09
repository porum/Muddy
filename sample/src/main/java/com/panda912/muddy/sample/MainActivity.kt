package com.panda912.muddy.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.panda912.muddy.R

class MainActivity : AppCompatActivity() {

  companion object {
    private const val TAG = "MainActivity"
  }

  private val TAG = "MainActivity_TAG"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    findViewById<TextView>(R.id.textview).text = TAG
  }
}