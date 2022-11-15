package com.loodico.tools.NavDemoApp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class MainMenu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
    }

    fun launchBasicDriving(button : android.view.View) {
        val myIntent = Intent(this@MainMenu, BasicNavActivity::class.java)
        this@MainMenu.startActivity(myIntent)
    }
}