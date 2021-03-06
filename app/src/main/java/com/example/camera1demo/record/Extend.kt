package com.example.camera1demo.record

import android.hardware.Camera


/**
 * @intro
 * @author sunhee
 * @date 2020/4/1
 */


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
    return rotateYUV420Degree180(
        yuv,
        imageWidth,
        imageHeight
    )
}

fun obtaintTrueByte(data: ByteArray,isFacingFront:Boolean,previewWidth: Int,previewHeight: Int):ByteArray?{
    return if (!isFacingFront){
        rotateYUV420Degree90(
            data,
            previewHeight,
            previewWidth
        )//原始帧是未旋转的 previewHeight 即原图的宽
    }else{
        rotateYUV420Degree270(
            data,
            previewHeight,
            previewWidth
        )//
    }
}


fun getCloselyPreSize(
    surfaceWidth: Int, surfaceHeight: Int,
    preSizeList: List<Camera.Size>
): Camera.Size? {
    val ReqTmpWidth: Int
    val ReqTmpHeight: Int
    // 当屏幕为垂直的时候需要把宽高值进行调换，保证宽大于高
    if (true) {
        ReqTmpWidth = surfaceHeight
        ReqTmpHeight = surfaceWidth
    } else {
        ReqTmpWidth = surfaceWidth
        ReqTmpHeight = surfaceHeight
    }
    //先查找preview中是否存在与surfaceview相同宽高的尺寸
    for (size in preSizeList) {
        if (size.width == ReqTmpWidth && size.height == ReqTmpHeight) {
            return size
        }
    }
    // 得到与传入的宽高比最接近的size
    val reqRatio = ReqTmpWidth.toFloat() / ReqTmpHeight
    var curRatio: Float
    var deltaRatio: Float
    var deltaRatioMin = Float.MAX_VALUE
    var retSize: Camera.Size? = null
    for (size in preSizeList) {
        curRatio = size.width.toFloat() / size.height
        deltaRatio = Math.abs(reqRatio - curRatio)
        if (deltaRatio < deltaRatioMin) {
            deltaRatioMin = deltaRatio
            retSize = size
        }
    }
    return retSize
}


