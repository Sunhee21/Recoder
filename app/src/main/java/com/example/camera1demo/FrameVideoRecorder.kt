package com.example.camera1demo

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.WorkerThread
import java.util.concurrent.ArrayBlockingQueue

/**
 * @intro
 * @author sunhee
 * @date 2020/4/6
 */
class FrameVideoRecorder {

    private var avcEncoder: AvcEncoder? = null
    companion object {
        val queueSize = 10
        var FrameQueue: ArrayBlockingQueue<VideoFrame?> = ArrayBlockingQueue(queueSize)
    }
    var videoSavePath: String? = null
    private var encodeThread: HandlerThread = HandlerThread("encodeThread").apply { start() }
    private var encodeHandler: Handler = Handler(encodeThread.looper)

    fun setVideoSavePath(outputPath: String): FrameVideoRecorder {
        this.videoSavePath = outputPath
        return this
    }



    fun putVideoFrame(x: VideoFrame): Unit {
        if (FrameQueue.size >= queueSize) {
            FrameQueue.poll()
        }
        FrameQueue.add(x)
    }


    fun start(width:Int,height: Int) {
        encodeHandler.post {
            checkNotNull(videoSavePath)
            avcEncoder = AvcEncoder(30, videoSavePath, 0)
            avcEncoder?.start(width, height)
        }
    }


    fun stop() {
        avcEncoder?.isRunning = false/*停止encodeThread内部的无线循环,encodeThread才能执行别的post*/
        encodeHandler.post {
            avcEncoder?.finish()
        }
    }

}