package com.example.camera1demo.camera

import android.graphics.Point
import android.graphics.SurfaceTexture
import android.view.SurfaceHolder

/**
 * @intro
 * @author sunhee
 * @date 2020/4/9
 */
interface ICamera {

    var iFrameCallback: IFrameCallback?

    fun opencamera(id:Int)



    fun startPreview(sufaceHolder: SurfaceHolder,surfaceWidth:Int,surfaceHeight: Int):Point
    fun startPreview(texture: SurfaceTexture,surfaceWidth:Int,surfaceHeight: Int):Point

    fun stopPreview()

    /**
     * 用于判断是否前置 后置摄像头
     */
    fun getCameraId():Int


    fun setFrameCallback(callback: IFrameCallback){
        iFrameCallback = callback
    }

}