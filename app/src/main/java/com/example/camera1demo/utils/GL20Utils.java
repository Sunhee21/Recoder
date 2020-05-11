package com.example.camera1demo.utils;

import android.opengl.Matrix;

/**
 * @author sunhee
 * @intro
 * @date 2020/5/11
 */
public class GL20Utils {



    //通过传入图片宽高和预览宽高，计算变换矩阵，得到的变换矩阵是预览类似ImageView的centerCrop效果
    public static float[] getShowMatrix(int imgWidth, int imgHeight, int viewWidth, int viewHeight) {
        float[] projection = new float[16];
        float[] camera = new float[16];
        float[] matrix = new float[16];

        float sWhView = (float) viewWidth / viewHeight;
        float sWhImg = (float) imgWidth / imgHeight;
        if (sWhImg > sWhView) {
            Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);
        } else {
            Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
        }
        Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
        Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        return matrix;
    }

    //旋转
    public static float[] rotate(float[] m, float angle) {
        Matrix.rotateM(m, 0, angle, 0, 0, 1);
        return m;
    }

    //镜像
    public static float[] flip(float[] m, boolean x, boolean y) {
        if (x || y) {
            Matrix.scaleM(m, 0, x ? -1 : 1, y ? -1 : 1, 1);
        }
        return m;
    }

}
