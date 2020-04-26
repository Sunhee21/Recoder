package com.example.camera1demo.view

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.hardware.Camera
import android.os.AsyncTask
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.example.camera1demo.record.FrameVideoRecorder
import com.example.camera1demo.record.NV21ToBitmap
import com.example.camera1demo.camera.*
import com.example.camera1demo.record.obtaintTrueByte
import java.io.File

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
    IFrameCallback {
    private val TAG = this::class.java.name

    private var videoRecorder: FrameVideoRecorder? = null
    private val mCamera: ICamera

    init {
        mCamera = CameraOne(context as Activity)
        mCamera.setFrameCallback(this)
        holder?.addCallback(this)
    }

    private var previewSize = Point(0, 0)


    private var surfaceHolder: SurfaceHolder? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (holder != null) {
            surfaceHolder = holder
            surfaceWidth = width
            surfaceHeight = height
        }
    }


    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        mCamera.stopPreview()
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
    }


    fun openCamera() {
        mCamera.opencamera(mCamera.getCameraId())
        surfaceHolder?.let {
            previewSize = mCamera.startPreview(it, surfaceWidth, surfaceHeight)
        }
    }

    fun switchCamera() {
        surfaceHolder?.let {
            if (mCamera.getCameraId() != Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera.opencamera(Camera.CameraInfo.CAMERA_FACING_BACK)
            } else {
                mCamera.opencamera(Camera.CameraInfo.CAMERA_FACING_FRONT)
            }
            previewSize = mCamera.startPreview(it, surfaceWidth, surfaceHeight)
        }
    }


    /**
     * 帧回调
     */
    override fun onFrameCallback(byteArray: ByteArray) {
//        Log.d(TAG, "帧数回调 - ${System.currentTimeMillis()}")
        if (iFlash != null) {
            AsyncTask.execute {
                val dat = obtaintTrueByte(
                    byteArray,
                    mCamera.getCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT,
                    previewSize.x,
                    previewSize.y
                )
                val b = NV21ToBitmap(
                    context,
                    previewSize.x,
                    previewSize.y
                ).nv21ToBitmap(dat)
                post {
                    iFlash?.onFlashCallback(b)
                    iFlash = null
                }
            }
        }
        if (iShoot != null) {
            videoRecorder?.addCameraFrameByte(
                byteArray
                , mCamera.getCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT
                , previewSize.x
                , previewSize.y
            )
        }
    }

    //<editor-fold desc="功能区">
    private var iFlash: IFlash? = null
    private var iShoot: IShoot? = null

    /**
     * 拍照
     */
    fun takePicture(iFlash: IFlash) {
        if (iShoot != null) {
            Log.d(TAG, "当前正在录像")
            return
        }
        this.iFlash = iFlash
    }

    /**
     * 录像
     */
    fun startShoot(file: File, iShoot: IShoot) {
        if (iFlash != null) {
            Log.d(TAG, "当前正在拍照")
            return
        }
        if (videoRecorder == null)
            videoRecorder = FrameVideoRecorder()
        videoRecorder?.setVideoSavePath(file.absolutePath)
        videoRecorder?.setFileCallback(object : FrameVideoRecorder.OnVideoCallback {
            override fun onVideo(file: File) {
                iShoot.onShootCallback(file)
            }

            override fun onFail() {
            }

        })
        videoRecorder?.start(previewSize.x, previewSize.y)

        this.iShoot = iShoot
    }


    fun stopShoot() {
        videoRecorder?.stop()
        iShoot = null
    }
    //</editor-fold>


}