package com.example.camera1demo.record

import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.example.camera1demo.*
import java.io.File
import java.util.concurrent.ArrayBlockingQueue


/**
 * @intro
 * @author sunhee
 * @date 2020/4/6
 */
class FrameVideoRecorder {


    private var avcEncoder: AvcEncoder? = null

    private var mMediaRecorder: MediaRecorder? = null


    companion object {
        val queueSize = 10
        const val OP_YUV = 1001
        var FrameQueue: ArrayBlockingQueue<VideoFrame?> = ArrayBlockingQueue(
            queueSize
        )
    }

    var videoSavePath: String? = null

    private var encodeThread: HandlerThread = HandlerThread("encodeThread").apply { start() }
    private var bitmapThread: HandlerThread = HandlerThread("bitmapThread").apply { start() }
    private var encodeHandler: Handler = Handler(encodeThread.looper)
    private var bitmapHandler: Handler = Handler(bitmapThread.looper)
    private var mainHandler = Handler(Looper.getMainLooper())

    fun setVideoSavePath(outputPath: String): FrameVideoRecorder {
        this.videoSavePath = outputPath
        return this
    }

    interface OnVideoCallback {
        fun onVideo(file: File)
        fun onFail()
    }

    var onVideoCallback: OnVideoCallback? = null

    fun setFileCallback(onVideoCallback: OnVideoCallback): FrameVideoRecorder {
        this.onVideoCallback = onVideoCallback
        return this
    }

    var startRecordeUs = 0L
    private var nV21ToBitmap: NV21ToBitmap? = null


    private val isRecording
        get() = avcEncoder?.isRunning == true



    private var isRecordStart = false


    private val tempVideoPath
        get() = run { FileUtils.getDirName(videoSavePath) + "no_audio.mp4" }
    private val tempAudioPath
        get() = run { FileUtils.getDirName(videoSavePath) + "no_video.mp4" }

    fun addCameraFrameByte(byteArray: ByteArray, isFacingFront: Boolean,frameWidth:Int,frameHeight:Int) {
        val now = System.nanoTime() / 1000L
        if (isRecording) {
            val timestamp = if (startRecordeUs == 0L) {/*为0则首帧*/
                startRecordeUs = now
                0
            } else {
                now - startRecordeUs
            }
            bitmapHandler.post {
                val yuvByte = obtaintTrueByte(
                    byteArray,
                    isFacingFront,
                    frameWidth,
                    frameHeight
                )
                    ?: return@post
                val canUse = nV21ToBitmap?.checkSize(frameWidth,frameHeight)?:false //切换摄像头可能导致帧尺寸改变，改变需要重新实例化
                if (!canUse){
                    nV21ToBitmap = NV21ToBitmap(
                        Utils.getApp(),
                        frameWidth,
                        frameHeight
                    )
                }
                val bitmap =
                    nV21ToBitmap?.nv21ToBitmap(yuvByte) ?: return@post
                putVideoFrame(
                    VideoFrame(
                        bitmap,
                        timestamp
                    )
                )
            }
        }
    }


    fun putVideoFrame(x: VideoFrame) {
        if (FrameQueue.size >= queueSize) {
            FrameQueue.poll()
        }
        FrameQueue.add(x)
    }

    private var previewWidth: Int = 0
    private var previewHeight: Int = 0

    fun start(width: Int, height: Int) {
        if (isRecordStart){
            LogUtils.d("正在录制...")
            return
        }
        isRecordStart = true
        previewWidth = width
        previewHeight = height
        check(previewWidth != 0)
        check(previewHeight != 0)
        LogUtils.d("FrameQueue.size :${FrameQueue.size}")
        if (FileUtils.isFileExists(videoSavePath))
            FileUtils.delete(videoSavePath)
        synchronized(recordVideoLock) {
            nV21ToBitmap = NV21ToBitmap(
                Utils.getApp(),
                width,
                height
            )
            avcEncoder = AvcEncoder(30, tempVideoPath, 0)
        }
        encodeHandler.post {
            checkNotNull(videoSavePath)
            stardReord()
            startRecordVideo()
        }
    }

    fun stop() {
        stopRecord()
        stopRecordVideo()
    }

    private fun startRecordVideo() {
        avcEncoder?.start(previewWidth, previewHeight)
    }

    private fun stopRecordVideo() {
        synchronized(recordVideoLock) {
            avcEncoder?.stop {
                //在encodeThread回调
                //结束后合并语音和视频文件
                H264_AAC_toMp4_MediaMuxer.combineVideo(
                    tempVideoPath,
                    tempAudioPath,
                    videoSavePath
                )
                if (!BuildConfig.DEBUG) {
                    FileUtils.delete(tempVideoPath)
                    FileUtils.delete(tempAudioPath)
                }
                nV21ToBitmap = null
                mainHandler.post {
                    isRecordStart = false
                    if (FileUtils.isFileExists(videoSavePath) && File(videoSavePath).length() > 0)
                        onVideoCallback?.onVideo(File(videoSavePath))
                    else
                        onVideoCallback?.onFail()
                }
            }
            avcEncoder = null
        }
    }


    private val recordAudioLock = Any()
    private val recordVideoLock = Any()

    private fun stardReord() {
        synchronized(recordAudioLock) {
            if (mMediaRecorder == null) {
                mMediaRecorder = MediaRecorder()
            }
            mMediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
//                setAudioSamplingRate(44100)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(tempAudioPath);
                prepare();
                start();
            }
        }
    }


    private fun stopRecord() {
        synchronized(recordAudioLock) {
            mMediaRecorder?.apply {
                stop();
                release();
                mMediaRecorder = null;
            }
        }
    }

    fun release() {
        encodeThread.quit()
        bitmapThread.quit()
    }

}