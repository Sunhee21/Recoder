package com.example.camera1demo


import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Camera
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.*
import kotlinx.android.synthetic.main.fragment_camera_shoot.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.max
import kotlin.math.min


/**
 * @intro
 * @author sunhee
 * @date 2020/3/23
 */
class CameraShootFragment : Fragment(), SurfaceHolder.Callback, Camera.PreviewCallback {

    companion object {

        const val FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK
        const val FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT
        private const val yuvqueuesize = 100
        var YUVQueue: ArrayBlockingQueue<Bitmap?>? = ArrayBlockingQueue(yuvqueuesize)

        fun newInstance() = CameraShootFragment()

    }


    private var rotationDegree: Int = 0
    private var avcEncoder: AvcEncoder? = null

    private var bitmapThread: HandlerThread = HandlerThread("bitmapThread").apply { start() }
    private var bitmapHandler: Handler = Handler(bitmapThread.looper)

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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera_shoot, container, false)
    }

    private lateinit var nV21ToBitmap :NV21ToBitmap

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nV21ToBitmap = NV21ToBitmap(context!!)
        initViewClick()
        initCameraInfo()
        surfaceView.holder?.addCallback(this)
    }

    val outPath = Utils.getApp().filesDir.toString() + "/test1.mp4";

    var shoot: Boolean = false
    var record: Boolean = false
    private var videoRecorder:FrameVideoRecorder?=null


    var startRecordeUs = 0L
    private fun initViewClick() {
        bt_shot.setOnClickListener {
            shoot = true
        }
        bt_shoot.setOnClickListener {
            record = !record
            bt_shoot.text = if (record) {
                record = true
                startRecordeUs = System.nanoTime()/1000L
                videoRecorder = FrameVideoRecorder().setVideoSavePath(outPath)
                videoRecorder?.start(previewWidth,previewHeight)
//                avEncoder = AvcEncoder(
//                    previewSize!!.width,
//                    previewSize!!.height,
//                    30,
//                    8500 * 1000
//                )
//                avEncoder?.StartEncoderThread()
                "停"
            } else {
                record = false
                startRecordeUs = 0L
                videoRecorder?.stop()
//                avEncoder?.StopThread()
//                avcEncoder?.finish()
                "摄"
            }
        }
        bt_switch.setOnClickListener {
            switchCamera()
        }
    }


    private fun initCameraInfo() {
        for (cameraId in 0 until Camera.getNumberOfCameras()) {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(cameraId, cameraInfo)
            if (cameraInfo.facing == FACING_BACK) {
                facingBackCameraInfo = cameraInfo
                facingBackCameraId = cameraId
            } else if (cameraInfo.facing == FACING_FRONT) {
                facingFrontCameraInfo = cameraInfo
                facingFrontCameraId = cameraId
            }
        }
    }


    /**
     * 切换前后置摄像头
     */
    fun switchCamera() {
        val cameraId = switchCameraId()
        stopPreview()
        closeCamera()
        openCamera(cameraId)
        configCameraParameters()
        startPreview(mPreviewSurfaceHolder)
    }


    /**
     * 切换前后置时切换ID
     */
    private fun switchCameraId(): Int {
        return if (mCurrentCameraId === facingBackCameraId && hasBackCamera()) {
            facingFrontCameraId ?: 0
        } else if (mCurrentCameraId === facingFrontCameraId && hasFrontCamera()) {
            facingBackCameraId ?: 0
        } else {
            throw java.lang.RuntimeException("No available camera id to switch.")
        }
    }


    private fun openCamera(cameraId: Int? = null) {
        if (mCurrentCamera != null) {
            throw RuntimeException("相机已开启")
        }

        if (ContextCompat.checkSelfPermission(
                context!!,
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

    }

    fun configCameraParameters() {



        val longSide = max(ScreenUtils.getScreenHeight(), ScreenUtils.getScreenWidth())
        val shortSide = min(ScreenUtils.getScreenHeight(), ScreenUtils.getScreenWidth())
        rotationDegree = getCameraDisplayOrientation(
            activity!!,
            mCurrentCameraInfo!!
        )

        mCurrentCamera!!.parameters?.also {
            it.setRotation(rotationDegree)
            it.previewFrameRate = 30
            setPreviewSize(it, longSide, shortSide)
            mCurrentCamera?.setDisplayOrientation(
                rotationDegree
            )
            mCurrentCamera!!.parameters = it
        }

        LogUtils.d("-----------------${previewWidth} ${previewHeight}")

    }

    private val PREVIEW_FORMAT = ImageFormat.NV21
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

//    private var previewSize: Camera.Size? = null
    private var previewWidth = 0
    private var previewHeight = 0
    

    private fun setPreviewSize(parameters: Camera.Parameters, longSide: Int, shortSide: Int) {
        if (mCurrentCamera != null && longSide != 0 && shortSide != 0) {
            val aspectRatio = longSide * 1.0f / shortSide
            val supportPreviewSize = parameters.supportedPreviewSizes
            supportPreviewSize.forEach {
                LogUtils.d("支持的预览尺寸 : ${it.width} ${it.height}")
            }
            for (size in supportPreviewSize) {
                if (size.width * 1f / size.height == aspectRatio && size.height <= shortSide && size.width <= longSide) {
                    previewHeight = size.width
                    previewWidth = size.height
                    parameters.setPreviewSize(size.width, size.height)
                    if (isPreviewFormatSupported(parameters, PREVIEW_FORMAT)) {
                        parameters.previewFormat = PREVIEW_FORMAT
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

                    break;
                }
            }
        }
    }


    fun closeCamera() {
        mCurrentCamera?.release()
        mCurrentCamera = null
    }

    private var mPreviewSurfaceHolder: SurfaceHolder? = null

    fun hasBackCamera() = facingBackCameraInfo != null
    fun hasFrontCamera() = facingFrontCameraInfo != null

    override fun surfaceCreated(holder: SurfaceHolder?) {
        openCamera(mCurrentCameraId)
        configCameraParameters()
        startPreview(mPreviewSurfaceHolder ?: holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        mPreviewSurfaceHolder = holder
    }


    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        stopPreview()
        closeCamera()
    }

    fun startPreview(holder: SurfaceHolder?) {
        mCurrentCamera?.setPreviewDisplay(holder)
        mCurrentCamera?.startPreview()
        mCurrentCamera?.setPreviewCallback(this)
    }


    fun stopPreview() {
        mCurrentCamera?.setPreviewCallback(null)
        mCurrentCamera?.stopPreview()
    }


    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        mCurrentCamera?.addCallbackBuffer(data)

        val now = System.nanoTime()/1000L
        var timestamp = 0L
        if (record && startRecordeUs == 0L){
            startRecordeUs = now
        }else{
            timestamp = now - startRecordeUs
        }


//        calculaeFps()
        bitmapHandler.post {

            if (shoot && data != null) {
                val data2 = obtaintTrueByte(data)
                val bitmap = generateBitmap(data2!!)//旋转后的 previewWidth 才成为视觉上的宽
                activity?.runOnUiThread {
                    bitmap?.also { iv_preview.setImageBitmap(it) }
                }
                shoot= false
            }

            if (record && data != null && data.isNotEmpty()) {
//                val now = System.currentTimeMillis()
                val data2 = obtaintTrueByte(data)
                val bitmap = generateBitmap(data2!!)
//                LogUtils.d("图片处理耗时:${System.currentTimeMillis() - now}")
                bitmap?.also {
                    videoRecorder?.putVideoFrame(VideoFrame(it,timestamp))
                    LogUtils.d(" timestamp2 : $timestamp")
//                    putYUVData(it)
                    LogUtils.d("未处理队列：${YUVQueue!!.size}")
                }
            }
        }

    }


    fun obtaintTrueByte(data: ByteArray):ByteArray?{
        return if (mCurrentCameraId != facingFrontCameraId){
            rotateYUV420Degree90(data,previewHeight,previewWidth)//原始帧是未旋转的 previewHeight 即原图的宽
        }else{
            rotateYUV420Degree270(data,previewHeight,previewWidth)//
        }
    }

    fun generateBitmap(data: ByteArray):Bitmap {
        return nV21ToBitmap.nv21ToBitmap(data,previewWidth,previewHeight)
    }



    var nows = System.currentTimeMillis()
    var fps = 0
    fun calculaeFps(){
        if (System.currentTimeMillis() - nows >= 1000L){
            nows = System.currentTimeMillis()
            LogUtils.d("fps:${fps}")
            fps = 0
        }else{
            ++fps
        }
    }





}