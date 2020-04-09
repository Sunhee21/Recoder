package com.example.camera1demo

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.Camera
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ScreenUtils
import kotlin.math.max
import kotlin.math.min

/**
 * @intro
 * @author sunhee
 * @date 2020/4/9
 */
class CameraOne(val activity:Activity):ICamera {


    /**
     * 前置
     */
    private var facingFrontCameraInfo: Camera.CameraInfo? = null
    private var facingFrontCameraId: Int? = null

    /**
     * 后置
     */
    private var facingBackCameraInfo: Camera.CameraInfo? = null
    private var facingBackCameraId: Int? = null

    private var mCurrentCameraInfo: Camera.CameraInfo? = null
    private var mCurrentCameraId: Int? = null
    private var mCurrentCamera: Camera? = null

    init {

        for (cameraId in 0 until Camera.getNumberOfCameras()) {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(cameraId, cameraInfo)
            if (cameraInfo.facing == CameraShootFragment.FACING_BACK) {
                facingBackCameraInfo = cameraInfo
                facingBackCameraId = cameraId
            } else if (cameraInfo.facing == CameraShootFragment.FACING_FRONT) {
                facingFrontCameraInfo = cameraInfo
                facingFrontCameraId = cameraId
            }
        }

    }

    fun hasBackCamera() = facingBackCameraInfo != null
    fun hasFrontCamera() = facingFrontCameraInfo != null


    private fun releasePreview() {
        mCurrentCamera?.setPreviewCallback(null)
        mCurrentCamera?.stopPreview()
    }
    private fun closeCamera() {
        mCurrentCamera?.release()
        mCurrentCamera = null
    }



    override fun opencamera(cameraId: Int) {
        releasePreview()
        closeCamera()
        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (hasBackCamera() && (cameraId == null || cameraId == facingBackCameraId)) {
                mCurrentCamera = Camera.open(facingBackCameraId!!)
                mCurrentCameraInfo = facingBackCameraInfo
                mCurrentCameraId = facingBackCameraId
            } else if (hasFrontCamera() && (cameraId == null || cameraId == facingFrontCameraId)) {
                mCurrentCamera = Camera.open(facingFrontCameraId!!)
                mCurrentCameraInfo = facingFrontCameraInfo
                mCurrentCameraId = facingFrontCameraId
            } else {
                throw RuntimeException("没相机可以开启")
            }
        }
        configCameraParameters()
    }
    private var rotationDegree: Int = 0


    private fun configCameraParameters() {
        val longSide = max(ScreenUtils.getScreenHeight(), ScreenUtils.getScreenWidth())
        val shortSide = min(ScreenUtils.getScreenHeight(), ScreenUtils.getScreenWidth())
        rotationDegree = getCameraDisplayOrientation(
            activity!!,
            mCurrentCameraInfo!!
        )

        mCurrentCamera!!.parameters?.also {
            it.setRotation(rotationDegree)
            // 设置聚焦模式
            // 设置聚焦模式
            val supportedFocusModes: List<String> =
                it.supportedFocusModes

            // 连续聚焦
            // 连续聚焦
            if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                it.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            }
            // 自动聚焦
            // 自动聚焦
            if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                it.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            }
            it.previewFrameRate = 30
            setPreviewSize(it, longSide, shortSide)
            mCurrentCamera?.setDisplayOrientation(
                rotationDegree
            )
            mCurrentCamera!!.parameters = it
        }

    }
    private var previewWidth = 0
    private var previewHeight = 0

    private fun setPreviewSize(parameters: Camera.Parameters, surfaceWidth: Int, surfaceHeight: Int) {
        if (mCurrentCamera != null) {
            val supportPreviewSize = parameters.supportedPreviewSizes
            supportPreviewSize.forEach {
                LogUtils.d("支持的预览尺寸 : ${it.width} ${it.height}")
            }
            val size = getCloselyPreSize(surfaceWidth,surfaceHeight,supportPreviewSize)?:return

            previewHeight = size.width
            previewWidth = size.height
            parameters.setPreviewSize(size.width, size.height)
            if (isPreviewFormatSupported(parameters, ImageFormat.NV21)) {
                parameters.previewFormat = ImageFormat.NV21
                val frameWidth: Int = size.width
                val frameHeight: Int = size.height
                val previewFormat = parameters.previewFormat
                val pixelFormat = PixelFormat()
                PixelFormat.getPixelFormatInfo(previewFormat, pixelFormat)
                val bufferSize =
                    frameWidth * frameHeight * pixelFormat.bitsPerPixel / 8
                mCurrentCamera!!.addCallbackBuffer(ByteArray(bufferSize))
                mCurrentCamera!!.addCallbackBuffer(ByteArray(bufferSize))
                mCurrentCamera!!.addCallbackBuffer(ByteArray(bufferSize))
//                        Log.d(TAG, "Add three callback buffers with size: $bufferSize")
            }

        }
    }

    /**
     * 判断指定的预览格式是否支持。
     */
    private fun isPreviewFormatSupported(
        parameters: Camera.Parameters,
        format: Int
    ): Boolean {
        val supportedPreviewFormats =
            parameters.supportedPreviewFormats
        return supportedPreviewFormats != null && supportedPreviewFormats.contains(format)
    }


    override fun startPreview(sufaceHolder: SurfaceHolder) {
        mCurrentCamera?.setPreviewDisplay(sufaceHolder)
        mCurrentCamera?.startPreview()
//        mCurrentCamera?.setPreviewCallback(this)
    }

    override fun stopPreview() {
        releasePreview()
        closeCamera()
    }
}