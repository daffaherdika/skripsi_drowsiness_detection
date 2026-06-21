package com.example.drowsinessdetection.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.drowsinessdetection.R
import com.example.drowsinessdetection.ui.home.HomeFragment
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi OpenCV
        if (!OpenCVLoader.initLocal()) {
            Toast.makeText(this, "OpenCV gagal dimuat!", Toast.LENGTH_LONG).show()
        }

        // Load HomeFragment pertama kali
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, HomeFragment())
                .commit()
        }
    }
}