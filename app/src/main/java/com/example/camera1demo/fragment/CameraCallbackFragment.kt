package com.example.camera1demo.fragment

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.*
import com.example.camera1demo.R
import com.example.camera1demo.camera.IFlash
import com.example.camera1demo.camera.IShoot
import kotlinx.android.synthetic.main.fragment_camera_callback.*
import java.io.File
import java.io.Serializable
import kotlin.math.max

/**
 * @intro
 * @author sunhee
 * @date 2020/4/16
 */
class CameraCallbackFragment : Fragment(), SurfaceHolder.Callback {
    companion object {


        fun newInstance() = CameraCallbackFragment()

        const val MAX_DURATION = 15;
        const val MIN_DURATION = 1500L;

        const val HDANDLER_DELAY = MAX_DURATION * 10;
    }

    private var previewMediaPlayer: MediaPlayer? = null
    private var stopAnimSet: AnimatorSet? = null
    private var startAnimSet: AnimatorSet? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera_callback, container, false)
    }

    private var isTimeout = false
    private var shootBitmap: Bitmap? = null
    private var shootDuration: Long = 0L
    private var mDownTime = 0L
    private lateinit var videoFile: File
    private var isCancel: Boolean = false
    private var mProgress: Int = 0
    private var handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> {
                    if (mProgress <= 100) {
                        main_progress_bar.setProgress(mProgress)
                    }
                    mProgress++
                    if (mProgress <= 100)
                        sendMessageDelayed(this.obtainMessage(0), HDANDLER_DELAY.toLong())

                }
                2 -> {
                    startRecordAnim()
                }
            }
        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        simpleCameraView.holder?.addCallback(this)
        initViewClick()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        videoFile = File(Utils.getApp().filesDir.toString() + "/test1.mp4")
    }

    private var isRecording = false
    private fun initViewClick() {
        btn_switch.setOnClickListener {
            simpleCameraView.switchCamera()
        }
        main_progress_bar.setOnProgressEndListener {
            isTimeout = true
            stopRecord()
        }
        main_press_control.setOnTouchListener { _, event ->
            if (!checkPermission()) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isTimeout = false
                    isCancel = false
                    tv_info.text = "  "//只能空白不然按钮动画被裁减bug
                    mDownTime = System.currentTimeMillis()
                    handler.sendEmptyMessageDelayed(2, 400L)
                    true
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    if (!isTimeout) {
                        tv_info.text = "轻触拍照，按住摄像"
                        handler.removeMessages(2)
                        if (isRecording) {
                            if (isCancel) Toast.makeText(context, "撤销", Toast.LENGTH_SHORT).show()
                            stopRecord()
                        } else {
                            takePicture()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val rect = Rect()
                    if (main_progress_bar.getGlobalVisibleRect(rect)) {
                        isCancel = !rect.contains(event.rawX.toInt(), event.rawY.toInt())
                        LogUtils.d("isCancel  ${rect}  ,${event.rawX.toInt()}  ${event.rawY.toInt()}")
                    }
                    true
                }
                else -> false
            }

        }
        btn_close.setOnClickListener {
            activity?.finish()
        }
        view_send.backLayout.setOnClickListener {
            shootBitmap = null
            previewVideoContainer.visibility = View.GONE
            previewPictureView.visibility = View.GONE
            view_send.stopAnim()
            record_layout.visibility = View.VISIBLE
            main_progress_bar.setProgress(0)
            tv_info.visibility = View.VISIBLE
            previewMediaPlayer?.release()
            previewMediaPlayer = null
        }
        view_send.selectLayout.setOnClickListener {
            //选择
            previewMediaPlayer?.release()
            previewMediaPlayer = null
            val intent = Intent()
            if (shootBitmap != null) {
                val savePath =
                    activity!!.filesDir.absolutePath + "/shoot_pic/${System.currentTimeMillis()}"
                if (ImageUtils.save(shootBitmap, savePath, Bitmap.CompressFormat.JPEG)) {
                    val imageInfo =
                        GoldenPictureResult(savePath, shootBitmap!!.width, shootBitmap!!.height)
                    intent.putExtra(GOLDEN_RESULT, imageInfo)
                    activity!!.setResult(GOLDEN_PICTURE_RESULT, intent)
                }
            } else if (FileUtils.getFileLength(videoFile.absolutePath) != -1L) {
                val videoInfo = GoldenVideoResult(
                    videoFile.absolutePath,
                    shootDuration,
                    ScreenUtils.getScreenWidth(),
                    ScreenUtils.getScreenHeight()
                )
                intent.putExtra(GOLDEN_RESULT, videoInfo)
                activity!!.setResult(GOLDEN_VIDEO_RESULT, intent)
            }
            activity!!.finish()
        }
    }

    /**
     * 拍照
     */
    private fun takePicture() {
        simpleCameraView.takePicture(object : IFlash {
            override fun onFlashCallback(bitmap: Bitmap) {
                displayPicture(bitmap)
            }

        })
    }

    private fun displayPicture(bitmap: Bitmap) {
        showSend()
        previewPictureView.apply {
            shootBitmap = bitmap
            setImageBitmap(bitmap)
            visibility = View.VISIBLE
        }
    }

    /**
     * 完成拍摄时才执行
     */
    private fun showSend() {
        record_layout.visibility = View.GONE
        view_send.startAnim()
        tv_info.visibility = View.GONE
    }

    /**
     * stop录像
     */
    private fun stopRecord() {
        isRecording = false
        shootDuration = ((mProgress - 1)/*ui展示的少1个周期*/ * HDANDLER_DELAY).toLong()
        simpleCameraView.stopShoot()
        stopAnim(Runnable {})
    }

    /**
     * 录制按钮的放大动画
     */
    private fun startRecordAnim() {
        startAnimSet = AnimatorSet()
        startAnimSet!!.playTogether(
            ObjectAnimator.ofFloat(main_press_control, "scaleX", 1f, 0.5f),
            ObjectAnimator.ofFloat(main_press_control, "scaleY", 1f, 0.5f),
            ObjectAnimator.ofFloat(main_progress_bar, "scaleX", 1f, 1.3f),
            ObjectAnimator.ofFloat(main_progress_bar, "scaleY", 1f, 1.3f)
        )
        startAnimSet!!.setDuration(250).start()
        startAnimSet!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                mProgress = 0
                main_progress_bar.setProgress(mProgress)
                handler.removeMessages(0)
                handler.sendMessage(handler.obtainMessage(0))
                startRecord()
            }
        })
    }

    /**
     * 录制按钮的缩小动画
     */
    private fun stopAnim(runnable: Runnable) {
        mProgress = 0
        main_progress_bar.setProgress(mProgress)
        handler.removeMessages(0)

        stopAnimSet = AnimatorSet()
        stopAnimSet!!.playTogether(
            ObjectAnimator.ofFloat(main_press_control, "scaleX", 0.5f, 1f),
            ObjectAnimator.ofFloat(main_press_control, "scaleY", 0.5f, 1f),
            ObjectAnimator.ofFloat(main_progress_bar, "scaleX", 1.3f, 1f),
            ObjectAnimator.ofFloat(main_progress_bar, "scaleY", 1.3f, 1f)
        )
        stopAnimSet!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mProgress = 0
                main_progress_bar.setProgress(mProgress)
                handler.removeMessages(0)
                handler.post(runnable)
            }
        })
        stopAnimSet!!.setDuration(250).start()
    }

    /**
     * 录像
     */
    fun startRecord() {
        record()
    }


    //<editor-fold desc="权限相关">
     val REQUEST_PERMISSION = 0x11//单单启动预览只有相机权限要弹窗
     val REQUEST_PERMISSION_NEED_AUDIO = 0x12 //点击拍照什么的需要提示存储和录音权限
    private fun requestPemission(code: Int = REQUEST_PERMISSION) {
        requestPermissions(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            )
            , code
        )
    }

    private fun checkPermission(): Boolean {
        val p1 = ContextCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val p2 = ContextCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val p3 = ContextCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!p1 || !p2 || !p3) {
            requestPemission(REQUEST_PERMISSION_NEED_AUDIO)
        }
        return p1 && p2 && p3
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION || requestCode == REQUEST_PERMISSION_NEED_AUDIO) {
            permissions.forEachIndexed { index, s ->
                val grant = grantResults[index]
//                val should = ActivityCompat.shouldShowRequestPermissionRationale(activity!!, s)
                if (s == Manifest.permission.CAMERA) {
                    if (grant == PackageManager.PERMISSION_GRANTED) {
                        //打开相机
                        simpleCameraView.openCamera()
                    } else {
                        showPermissionDialog("缺少相机权限")
                    }
                } else if (s == Manifest.permission.RECORD_AUDIO) {
                    if (grant == PackageManager.PERMISSION_GRANTED) {
                        //可以录制视频
                    } else {
                        if (requestCode == REQUEST_PERMISSION_NEED_AUDIO)
                            showPermissionDialog("缺少录音权限")
                    }
                } else if (s == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    if (grant == PackageManager.PERMISSION_GRANTED) {
                        //可以保存文件
                    } else {
                        if (requestCode == REQUEST_PERMISSION_NEED_AUDIO)
                            showPermissionDialog("缺少存储权限")
                    }
                }
            }

        }

    }
    private var permissionDialog: AlertDialog? = null

    private fun showPermissionDialog(str: String) {
        if (permissionDialog == null)
            permissionDialog = AlertDialog.Builder(context)
                .setMessage(str)
                .setCancelable(false)
                .setPositiveButton("去打开") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    permissionDialog = null
                    val intent = Intent()
                    intent.action =
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS;//设置去向意图
                    val uri = Uri.fromParts("package", context!!.packageName, null)
                    intent.data = uri
                    context!!.startActivity(intent);
                }
                .setNegativeButton("取消") { _: DialogInterface, _: Int ->
                    activity?.finish()
                }
                .show()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        requestPemission()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
    }

    //</editor-fold>

    private fun record() {
        isRecording = true
        simpleCameraView.startShoot(videoFile, object : IShoot {

            override fun onShootCallback(file: File) {
                if (!isCancel && checkShootDuration()) {
                    previewVideoContainer.visibility = View.VISIBLE
                    if (previewVideoView.isAvailable) {
                        startVideo()
                    } else {
                        previewVideoView.surfaceTextureListener =
                            object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureSizeChanged(
                                    surface: SurfaceTexture?,
                                    width: Int,
                                    height: Int
                                ) {
                                }

                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) =
                                    true

                                override fun onSurfaceTextureAvailable(
                                    surface: SurfaceTexture?,
                                    width: Int,
                                    height: Int
                                ) {
                                    startVideo()
                                }
                            }
                    }
                }
            }

        })


    }

    private fun checkShootDuration(): Boolean {
        if (shootDuration > MIN_DURATION) {
            return true
        } else {
            Toast.makeText(activity, "录制时长过短", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    /**
     * 拍摄完播放预览
     */
    private fun startVideo() {
        showSend()
        previewMediaPlayer = MediaPlayer().apply {
            setSurface(Surface(previewVideoView.surfaceTexture))
            setDataSource(videoFile.absolutePath)
            isLooping = true
            setOnCompletionListener {

            }
            setOnVideoSizeChangedListener { _, width, height ->
                previewVideoView.apply {
                    layoutParams = layoutParams.apply {
                        val scaleX = previewVideoContainer.width / width.toFloat()
                        val scaleY = previewVideoContainer.height / height.toFloat()
                        val scale = max(scaleX, scaleY)

                        this.width = (width * scale).toInt()
                        this.height = (height * scale).toInt()
                    }
                }
            }
            prepare()
            start()

        }
    }


}

/**
 * 返回结果
 */
data class GoldenPictureResult(
    val path: String,
    val width: Int,
    val height: Int
) : Serializable

data class GoldenVideoResult(
    val path: String,
    val length: Long,
    val width: Int,
    val height: Int
) : Serializable

val GOLDEN_PICTURE_RESULT: Int = 1010
val GOLDEN_VIDEO_RESULT: Int = 1011
val GOLDEN_RESULT = "golden_result"