package com.example.camera1demo

import android.graphics.Bitmap
import java.sql.Timestamp

/**
 * @intro
 * @author sunhee
 * @date 2020/4/6
 */


/**
 * 视频的每一帧 数据及时间
 */
data class VideoFrame(val frameBitmap: Bitmap
                      ,val timestamp: Long) {



}