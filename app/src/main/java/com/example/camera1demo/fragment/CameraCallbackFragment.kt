package com.example.camera1demo.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.camera1demo.R
import com.example.camera1demo.camera.IFlash
import com.example.camera1demo.camera.IShoot
import kotlinx.android.synthetic.main.fragment_camera_callback.*
import java.io.File

/**
 * @intro
 * @author sunhee
 * @date 2020/4/16
 */
class CameraCallbackFragment:Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera_callback, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewClick()
    }

    private fun initViewClick() {
        bt_switch.setOnClickListener {
            simpleCameraView.switchCamera()
        }
        bt_flash.setOnClickListener {
            simpleCameraView.takePicture(object :IFlash{
                override fun onFlashCallback(bitmap: Bitmap) {
                    iv_preview.setImageBitmap(bitmap)
                }
            })
        }
        bt_shoot.setOnClickListener {
            simpleCameraView.startShoot(object :IShoot{
                override fun onShootCallback(file: File) {

                }
            })
        }
    }

}