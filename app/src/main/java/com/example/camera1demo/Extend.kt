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
    var i = 0
    var count = 0
    i = imageWidth * imageHeight - 1
    while (i >= 0) {
        yuv[count] = data[i]
        count++
        i--
    }
    i = imageWidth * imageHeight * 3 / 2 - 1
    i = imageWidth * imageHeight * 3 / 2 - 1
    while (i >= imageWidth
        * imageHeight
    ) {
        yuv[count++] = data[i - 1]
        yuv[count++] = data[i]
        i -= 2
    }
    return yuv
}

fun rotateYUV420Degree270(
    data: ByteArray, imageWidth: Int,
    imageHeight: Int
): ByteArray? {
    val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
    var nWidth = 0
    var nHeight = 0
    var wh = 0
    var uvHeight = 0
    if (imageWidth != nWidth || imageHeight != nHeight) {
        nWidth = imageWidth
        nHeight = imageHeight
        wh = imageWidth * imageHeight
        uvHeight = imageHeight shr 1 // uvHeight = height / 2
    }
    // ??Y
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
    return rotateYUV420Degree180(yuv, imageWidth, imageHeight)
}


//镜像；
fun frameMirror(data: ByteArray, width: Int, height: Int): ByteArray? {
    var tempData: Byte
    for (i in 0 until height * 3 / 2) {
        for (j in 0 until width / 2) {
            tempData = data[i * width + j]
            data[i * width + j] = data[(i + 1) * width - 1 - j]
            data[(i + 1) * width - 1 - j] = tempData
        }
    }
    return data
}


fun NV21_mirror(nv21_data: ByteArray, width: Int, height: Int): ByteArray? {
    var i: Int
    var left: Int
    var right: Int
    var temp: Byte
    var startPos = 0
    // mirror Y
    i = 0
    while (i < height) {
        left = startPos
        right = startPos + width - 1
        while (left < right) {
            temp = nv21_data[left]
            nv21_data[left] = nv21_data[right]
            nv21_data[right] = temp
            left++
            right--
        }
        startPos += width
        i++
    }
    // mirror U and V
    val offset = width * height
    startPos = 0
    i = 0
    while (i < height / 2) {
        left = offset + startPos
        right = offset + startPos + width - 2
        while (left < right) {
            temp = nv21_data[left]
            nv21_data[left] = nv21_data[right]
            nv21_data[right] = temp
            left++
            right--
            temp = nv21_data[left]
            nv21_data[left] = nv21_data[right]
            nv21_data[right] = temp
            left++
            right--
        }
        startPos += width
        i++
    }
    return nv21_data
}