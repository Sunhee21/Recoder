//package com.example.camera1demo.camera
//
//import android.app.Activity
//
///**
// * @intro
// * @author sunhee
// * @date 2020/4/25
// */
//object SimpleCamera {
//
//    private var mCamera: ICamera? = null
//
//    fun openCamera(activity: Activity) {
//        if (mCamera == null)
//            mCamera = CameraOne(activity)
//    }
//
//    fun switchCamera(){
//
//    }
//
//    fun closeCamera() {
//        mCamera?.stopPreview()
//    }
//
//    fun setFrameCallback(callback: IFrameCallback) {
//        mCamera?.setFrameCallback(callback)
//    }
//
//
//    fun destory() {
//        mCamera = null
//    }
//}