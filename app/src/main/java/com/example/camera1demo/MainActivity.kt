package com.example.camera1demo

import android.hardware.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import com.example.camera1demo.fragment.CameraCallbackFragment
import com.example.camera1demo.fragment.CameraGLFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main)

        if (Camera.getNumberOfCameras() <= 0){
            LogUtils.d("摄像头个数${Camera.getNumberOfCameras()}")
            Toast.makeText(this,"手机没摄像头",Toast.LENGTH_SHORT).show()
            return
        }
        savedInstanceState ?: supportFragmentManager.beginTransaction()
                .replace(R.id.container, CameraGLFragment())
                .commit()
    }
}
