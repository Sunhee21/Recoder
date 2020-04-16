package com.example.camera1demo.camera

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.Camera
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.camera1demo.getCloselyPreSize

/**
 * @intro
 * @author sunhee
 * @date 2020/4/9
 */
class CameraOne(val activity: Activity) :
    ICamera, Camera.PreviewCallback {

    override var iFrameCallback: IFrameCallback? = null


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
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                facingBackCameraInfo = cameraInfo
                facingBackCameraId = cameraId
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
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
        stopPreview()
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
                Toast.makeText(activity, "没相机可以开启", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var rotationDegree: Int = 0


    private fun configCameraParameters(surfaceWidth: Int, surfaceHeight: Int) {
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
            setPreviewSize(it, surfaceWidth, surfaceHeight)
            mCurrentCamera?.setDisplayOrientation(
                rotationDegree
            )
            mCurrentCamera!!.parameters = it
        }

    }

    private fun getCameraDisplayOrientation(
        activity: Activity,
        cameraInfo: Camera.CameraInfo
    ): Int {
        val rotation: Int = activity.getWindowManager().getDefaultDisplay().getRotation()
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (cameraInfo.facing === Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else { // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360
        }
        return result
    }

    private var previewWidth = 0
    private var previewHeight = 0

    private fun setPreviewSize(
        parameters: Camera.Parameters,
        surfaceWidth: Int,
        surfaceHeight: Int
    ) {
        if (mCurrentCamera != null) {
            val supportPreviewSize = parameters.supportedPreviewSizes
//            supportPreviewSize.forEach {
//                LogUtils.d("支持的预览尺寸 : ${it.width} ${it.height}")
//            }
            val size = getCloselyPreSize(
                surfaceWidth,
                surfaceHeight,
                supportPreviewSize
            ) ?: return
            previewHeight = size.width
            previewWidth = size.height
            parameters.setPreviewSize(size.width, size.height)
            if (isPreviewFormatSupported(parameters, ImageFormat.NV21)) {
                parameters.previewFormat = ImageFormat.NV21
            }
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


    override fun startPreview(sufaceHolder: SurfaceHolder, surfaceWidth: Int, surfaceHeight: Int):Point {
        configCameraParameters(surfaceWidth, surfaceHeight)
        mCurrentCamera?.setPreviewDisplay(sufaceHolder)
        mCurrentCamera?.startPreview()
        mCurrentCamera?.setPreviewCallback(this)
        return Point(previewWidth,previewHeight)
    }

    override fun stopPreview() {
        releasePreview()
        closeCamera()
    }

    override fun getCameraId() = mCurrentCameraId ?: Camera.CameraInfo.CAMERA_FACING_BACK
    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (data != null)
            iFrameCallback?.onFrameCallback(data)
    }
}