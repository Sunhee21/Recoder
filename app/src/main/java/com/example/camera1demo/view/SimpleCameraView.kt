package com.example.camera1demo.view

import android.app.Activity
import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.camera1demo.camera.CameraOne
import com.example.camera1demo.camera.ICamera
import com.example.camera1demo.camera.IFrameCallback

/**
 * @intro
 * @author sunhee
 * @date 2020/4/16
 */

class SimpleCameraView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defaultStyle: Int = 0
) : SurfaceView(context, attributeSet, defaultStyle)
    , SurfaceHolder.Callback,
    IFrameCallback
{

    private val mCamera: ICamera

    init {
        mCamera = CameraOne(context as Activity)
        mCamera.setFrameCallback(this)
        holder?.addCallback(this)
    }

    fun switchCamera(){
        if (mCamera.getCameraId() != Camera.CameraInfo.CAMERA_FACING_BACK){
            mCamera.opencamera(Camera.CameraInfo.CAMERA_FACING_BACK)
        }else{
            mCamera.opencamera(Camera.CameraInfo.CAMERA_FACING_FRONT)
        }
        surfaceHolder?.let {
            mCamera.startPreview(it,surfaceWidth,surfaceHeight)
        }
    }

    private var surfaceHolder: SurfaceHolder? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (holder != null){
            surfaceHolder = holder
            surfaceWidth = width
            surfaceHeight = height
        }
        surfaceHolder?.let {
            mCamera.startPreview(it,surfaceWidth,surfaceHeight)
        }
    }


    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        mCamera.stopPreview()
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        mCamera.opencamera(Camera.CameraInfo.CAMERA_FACING_BACK)
    }

    val TAG = this::class.java.name
    /**
     * 帧回调
     */
    override fun onFrameCallback(byteArray: ByteArray) {
        Log.d(TAG,"帧数回调 - ${System.currentTimeMillis()}")
    }

}