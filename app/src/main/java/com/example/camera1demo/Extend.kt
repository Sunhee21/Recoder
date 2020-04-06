package com.example.camera1demo

import android.app.Activity
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.view.Surface


/**
 * @intro
 * @author sunhee
 * @date 2020/4/1
 */
fun getCameraDisplayOrientation(activity:Activity,cameraInfo: CameraInfo): Int {
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

fun rotateYUV420Degree90(
    data: ByteArray,
    imageWidth: Int,
    imageHeight: Int
): ByteArray? {
    val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
    // Rotate the Y luma
    var i = 0
    for (x in 0 until imageWidth) {
        for (y in imageHeight - 1 downTo 0) {
            yuv[i] = data[y * imageWidth + x]
            i++
        }
    }
    // Rotate the U and V color components
    i = imageWidth * imageHeight * 3 / 2 - 1
    var x = imageWidth - 1
    while (x > 0) {
        for (y in 0 until imageHeight / 2) {
            yuv[i] = data[imageWidth * imageHeight + y * imageWidth + x]
            i--
            yuv[i] = data[imageWidth * imageHeight + y * imageWidth + (x - 1)]
            i--
        }
        x = x - 2
    }
    return yuv
}

fun rotateYUV420Degree180(
    data: ByteArray,
    imageWidth: Int,
    imageHeight: Int
): ByteArray? {
    val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
    var i: Int
    var count = 0
    i = imageWidth * imageHeight - 1
    while (i >= 0) {
        yuv[count] = data[i]
        count++
        i--
    }
    i = imageWidth * imageHeight * 3 / 2 - 1
    while (i >= imageWidth
        * imageHeight
    ) {
        yuv[count] = data[i - 1]
        count++
        yuv[count] = data[i]
        count++
        i -= 2
    }
    return yuv
}

fun rotateYUV420Degree270(
    data: ByteArray,
    imageWidth: Int,
    imageHeight: Int
): ByteArray? {
    val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
    var wh = 0
    var uvHeight = 0
    if (imageWidth != 0 || imageHeight != 0) {
        wh = imageWidth * imageHeight
        uvHeight = imageHeight shr 1 //uvHeight = height / 2
    }
    //旋转Y
    var k = 0
    for (i in 0 until imageWidth) {
        var nPos = 0
        for (j in 0 until imageHeight) {
            yuv[k] = data[nPos + i]
            k++
            nPos += imageWidth
        }
    }
    var i = 0
    while (i < imageWidth) {
        var nPos = wh
        for (j in 0 until uvHeight) {
            yuv[k] = data[nPos + i]
            yuv[k + 1] = data[nPos + i + 1]
            k += 2
            nPos += imageWidth
        }
        i += 2
    }
    //这一部分可以直接旋转270度，但是图像颜色不对
//	    // Rotate the Y luma
//	    int i = 0;
//	    for(int x = imageWidth-1;x >= 0;x--)
//	    {
//	        for(int y = 0;y < imageHeight;y++)
//	        {
//	            yuv[i] = data[y*imageWidth+x];
//	            i++;
//	        }
//
//	    }
//	    // Rotate the U and V color components
//		i = imageWidth*imageHeight;
//	    for(int x = imageWidth-1;x > 0;x=x-2)
//	    {
//	        for(int y = 0;y < imageHeight/2;y++)
//	        {
//	            yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
//	            i++;
//	            yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
//	            i++;
//	        }
//	    }
    return rotateYUV420Degree180(yuv, imageWidth, imageHeight)
}

fun obtaintTrueByte(data: ByteArray,isFacingFront:Boolean,previewWidth: Int,previewHeight: Int):ByteArray?{
    return if (!isFacingFront){
        rotateYUV420Degree90(data,previewHeight,previewWidth)//原始帧是未旋转的 previewHeight 即原图的宽
    }else{
        rotateYUV420Degree270(data,previewHeight,previewWidth)//
    }
}


