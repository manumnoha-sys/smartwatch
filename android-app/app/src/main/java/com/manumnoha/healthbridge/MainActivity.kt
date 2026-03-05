package com.manumnoha.healthbridge

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(android.R.layout.simple_list_item_1)
        (findViewById<TextView>(android.R.id.text1)).text =
            "Health Bridge is running.\n\nSyncing:\n· CGM glucose (every 5 min)\n· Wodify workouts (every 30 min)"
    }
}
