package com.example.camera1demo;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.blankj.utilcode.util.LogUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @说明 h264   aac   muxer 成mp4
 * 直接muxer h264和aac 无法muxer，
 * audioExtractor.setDataSource(sdcard_path + "/input.aac");无法读取
 * 1.需要将h264先混合成mpeg4包装的mp4（无音频）
 * 2.需要将aac（无adts）先混合成mpeg4容器包装的mp4（无视频）
 * 3.muxer混合包装好的音频和视频（分别从包装好的中重新分离出来aac和H264），生成新的视频文件
 */

public class H264_AAC_toMp4_MediaMuxer {
    private static String TAG = "H264_AAC_toMp4_MediaMuxer";

    /**
     * aac 复用 成mp4（无视频）
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void muxerAudio(String sdcard_path) {
        MediaMuxer audioMuxer;
        MediaExtractor mediaExtractor = null;
        try {
            mediaExtractor = new MediaExtractor();
            audioMuxer = new MediaMuxer(sdcard_path + "/mux_audio.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mediaExtractor.setDataSource(sdcard_path + "/input.mp4");
            int trackCount = mediaExtractor.getTrackCount();
            int audioTrackIndex = -1;
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String mineType = trackFormat.getString(MediaFormat.KEY_MIME);
                //音频信道
                if (mineType.startsWith("audio/")) {
                    audioTrackIndex = i;
                }
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            //切换到音频信道
            mediaExtractor.selectTrack(audioTrackIndex);
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(audioTrackIndex);
            int writeAudioIndex = audioMuxer.addTrack(trackFormat);
            audioMuxer.start();
            long sampletime = 0;
            long first_sampletime = 0;
            long second_sampletime = 0;
            {
                //  mediaExtractor.readSampleData(byteBuffer, 0);
                //   mediaExtractor.advance();
                mediaExtractor.readSampleData(byteBuffer, 0);
                first_sampletime = mediaExtractor.getSampleTime();
                mediaExtractor.advance();
                second_sampletime = mediaExtractor.getSampleTime();
                sampletime = Math.abs(second_sampletime - first_sampletime);//时间戳
            }
            mediaExtractor.unselectTrack(audioTrackIndex);
            mediaExtractor.selectTrack(audioTrackIndex);
            while (true) {
                int readSampleCount = mediaExtractor.readSampleData(byteBuffer, 0);
                Log.d(TAG, "audio:readSampleCount:" + readSampleCount);
                if (readSampleCount < 0) {
                    break;
                }
                bufferInfo.size = readSampleCount;
                bufferInfo.offset = 0;
                bufferInfo.flags = mediaExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs += sampletime;
                audioMuxer.writeSampleData(writeAudioIndex, byteBuffer, bufferInfo);
                byteBuffer.clear();
                mediaExtractor.advance();
            }
            Log.d(TAG, "muxerAudio finished!\n");
            audioMuxer.stop();
            audioMuxer.release();
            mediaExtractor.release();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * h264 复用 成 mp4（无音频）
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void muxerVideo(String sdcard_path) {
        MediaMuxer videoMuxer;
        MediaExtractor mediaExtractor = null;
        try {
            mediaExtractor = new MediaExtractor();
            videoMuxer = new MediaMuxer(sdcard_path + "/mux_video.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mediaExtractor.setDataSource(sdcard_path + "/input.mp4");
            int trackCount = mediaExtractor.getTrackCount();
            int videoTrackIndex = -1;
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String mineType = trackFormat.getString(MediaFormat.KEY_MIME);
                //音频信道
                if (mineType.startsWith("video/")) {
                    videoTrackIndex = i;
                }
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            //切换到音频信道
            mediaExtractor.selectTrack(videoTrackIndex);
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(videoTrackIndex);
            int writeVideoIndex = videoMuxer.addTrack(trackFormat);
            videoMuxer.start();
            long sampletime = 0;
            long first_sampletime = 0;
            long second_sampletime = 0;
            {
                //  mediaExtractor.readSampleData(byteBuffer, 0);
                //   mediaExtractor.advance();
                mediaExtractor.readSampleData(byteBuffer, 0);
                first_sampletime = mediaExtractor.getSampleTime();
                mediaExtractor.advance();
                second_sampletime = mediaExtractor.getSampleTime();
                sampletime = Math.abs(second_sampletime - first_sampletime);//时间戳
            }
            //上面只是获取时间戳，获取完后，重新选择下track
            mediaExtractor.unselectTrack(videoTrackIndex);
            mediaExtractor.selectTrack(videoTrackIndex);
            while (true) {
                int readSampleCount = mediaExtractor.readSampleData(byteBuffer, 0);
                Log.d(TAG, "audio:readSampleCount:" + readSampleCount);
                if (readSampleCount < 0) {
                    break;
                }
                bufferInfo.size = readSampleCount;
                bufferInfo.offset = 0;
                bufferInfo.flags = mediaExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs += sampletime;
                videoMuxer.writeSampleData(writeVideoIndex, byteBuffer, bufferInfo);
                byteBuffer.clear();
                mediaExtractor.advance();
            }
            Log.d(TAG, "muxerVideo finished!\n");
            videoMuxer.stop();
            videoMuxer.release();
            mediaExtractor.release();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 合并上面生成的aac（mp4容器） 和 h264（mp4容器）
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean combineVideo(String videoPath,String audioPath,String ouputPath) {
        MediaMuxer mediaMuxer;
        MediaExtractor videoExtractor = null;
        MediaExtractor audioExtractor = null;
        try {
            audioExtractor = new MediaExtractor();
            videoExtractor = new MediaExtractor();
            mediaMuxer = new MediaMuxer(ouputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoExtractor.setDataSource(videoPath);
            audioExtractor.setDataSource(audioPath);
            int trackCount = videoExtractor.getTrackCount();
            int audioTrackCount = audioExtractor.getTrackCount();
            int videoTrackIndex = -1;
            for (int i = 0; i < trackCount; i++) {
                MediaFormat videoFormat = videoExtractor.getTrackFormat(i);
                String mineType = videoFormat.getString(MediaFormat.KEY_MIME);
                //视频信道
                if (mineType.startsWith("video/")) {
                    videoTrackIndex = i;
                }
            }
            int audioTrackIndex = -1;
            LogUtils.d("audioTrackCount " + audioTrackCount);
            for (int i = 0; i < audioTrackCount; i++) {
                    MediaFormat audioFormat = audioExtractor.getTrackFormat(i);
                    String mineType = audioFormat.getString(MediaFormat.KEY_MIME);
                    //视频信道
                LogUtils.d("mineType " + mineType);
                    if (mineType.startsWith("audio/")) {
                        audioTrackIndex = i;
                    }
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            MediaCodec.BufferInfo audiobufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo videobufferInfo = new MediaCodec.BufferInfo();
            videoExtractor.selectTrack(videoTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);
            MediaFormat videotrackFormat = videoExtractor.getTrackFormat(videoTrackIndex);
            int writeVideoIndex = mediaMuxer.addTrack(videotrackFormat);
            MediaFormat audiotrackFormat = audioExtractor.getTrackFormat(audioTrackIndex);
            int writeAudioIndex = mediaMuxer.addTrack(audiotrackFormat);
            mediaMuxer.start();
            //video
            long sampletime = 0;
            long audioSampletime = 0;
            long first_sampletime = 0;
            long second_sampletime = 0;
            {
                videoExtractor.readSampleData(byteBuffer, 0);
                first_sampletime = videoExtractor.getSampleTime();
                videoExtractor.advance();
                second_sampletime = videoExtractor.getSampleTime();
                sampletime = Math.abs(second_sampletime - first_sampletime);//时间戳
                Log.d(TAG, "sampletime" + sampletime);

            }
            //上面只是获取时间戳，获取完后，重新选择下track
            videoExtractor.unselectTrack(videoTrackIndex);
            videoExtractor.selectTrack(videoTrackIndex);

            audioExtractor.readSampleData(byteBuffer,0);
            first_sampletime = audioExtractor.getSampleTime();
            audioExtractor.advance();
            second_sampletime = audioExtractor.getSampleTime();
            audioSampletime = Math.abs(second_sampletime - first_sampletime);//时间戳
            Log.d(TAG, "audioSampletime" + audioSampletime);
            //上面只是获取时间戳，获取完后，重新选择下track
            audioExtractor.unselectTrack(audioTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);

            while (true) {
                int readSampleCount = videoExtractor.readSampleData(byteBuffer, 0);
                Log.d(TAG, "video:readSampleCount:" + readSampleCount);
                if (readSampleCount < 0) {
                    break;
                }
                audiobufferInfo.size = readSampleCount;
                audiobufferInfo.offset = 0;
                audiobufferInfo.flags = videoExtractor.getSampleFlags();
                audiobufferInfo.presentationTimeUs += sampletime;
                mediaMuxer.writeSampleData(writeVideoIndex, byteBuffer, audiobufferInfo);
                byteBuffer.clear();
                videoExtractor.advance();
            }
            //audio
            while (true) {
                int readSampleCount = audioExtractor.readSampleData(byteBuffer, 0);
                Log.d(TAG, "audio:readSampleCount:" + readSampleCount);
                if (readSampleCount < 0) {
                    break;
                }
                videobufferInfo.size = readSampleCount;
                videobufferInfo.offset = 0;
                videobufferInfo.flags = audioExtractor.getSampleFlags();
                videobufferInfo.presentationTimeUs += audioSampletime;
                mediaMuxer.writeSampleData(writeAudioIndex, byteBuffer, videobufferInfo);
                byteBuffer.clear();
                audioExtractor.advance();
            }
            Log.d(TAG, "combineVideo finished!\n");
            mediaMuxer.stop();
            mediaMuxer.release();
            audioExtractor.release();
            videoExtractor.release();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}