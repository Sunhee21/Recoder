package com.example.camera1demo.camera

/**
 * @intro
 * @author sunhee
 * @date 2020/4/16
 */
interface IFrameCallback{

    /**
     * @param byteArray 每一帧数据 需要 判断前后置摄像头 旋转相应度数
     *
     */
    fun onFrameCallback(byteArray: ByteArray)

}