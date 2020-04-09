package com.example.camera1demo

import android.view.SurfaceHolder

/**
 * @intro
 * @author sunhee
 * @date 2020/4/9
 */
interface ICamera {

    fun opencamera(id:Int)

    fun startPreview(sufaceHolder: SurfaceHolder)

    fun stopPreview()


}