package com.example.camera1demo.view.opengl;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.example.camera1demo.utils.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author sunhee
 * @intro
 * @date 2020/5/11
 */
public class CameraRender implements GLSurfaceView.Renderer {
    private final String vertexShaderCode = "attribute vec4 vPosition;\n" +
            "    attribute vec2 vCoordinate;\n" +
            "    uniform mat4 vMatrix;\n" +
            "\n" +
            "    varying vec2 aCoordinate;\n" +
            "\n" +
            "    void main(){\n" +
            "        gl_Position=vMatrix*vPosition;\n" +
            "        aCoordinate=vCoordinate;\n" +
            "    }";

    private final String fragmentShaderCode = "#extension GL_OES_EGL_image_external : require\n" +
            "    precision mediump float;\n" +
            "    varying vec2 aCoordinate;\n" +
            "    uniform samplerExternalOES vTexture;\n" +
            "    void main() {\n" +
            "        gl_FragColor = texture2D( vTexture, aCoordinate );\n" +
            "    }\n";



    private final float[] sPos = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f
    };

    private final float[] sCoord = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };



    private FloatBuffer bPos;
    private FloatBuffer bCoord;

    private int mProgram;
    private int vPositionHandle;
    private int vTextureHandle;
    private int vCoordinateHandle;
    private int vMatrixHandle;

    private float[] matrix;

    public float[] getMatrix() {
        return matrix;
    }

    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }



    public CameraRender(){
        ByteBuffer bb = ByteBuffer.allocateDirect(sPos.length * 4);
        bb.order(ByteOrder.nativeOrder());
        bPos = bb.asFloatBuffer();
        bPos.put(sPos);
        bPos.position(0);
        ByteBuffer cc = ByteBuffer.allocateDirect(sCoord.length * 4);
        cc.order(ByteOrder.nativeOrder());
        bCoord = cc.asFloatBuffer();
        bCoord.put(sCoord);
        bCoord.position(0);
    }

    public int onSurfaceCreated(){
        int vertexShader = ShaderUtils.loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = ShaderUtils.loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

//创建一个空的OpenGLES程序
        mProgram = GLES20.glCreateProgram();
        //将顶点着色器加入到程序
        GLES20.glAttachShader(mProgram, vertexShader);
        //将片元着色器加入到程序中
        GLES20.glAttachShader(mProgram, fragmentShader);
        //连接到着色器程序
        GLES20.glLinkProgram(mProgram);

        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        vCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        vTextureHandle = GLES20.glGetUniformLocation(mProgram, "vTexture");
        vMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        return createTextureID();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {


    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
//将程序加入到OpenGLES2.0环境
        GLES20.glUseProgram(mProgram);
        //指定vMatrix的值

        GLES20.glUniformMatrix4fv(vMatrixHandle, 1, false, matrix, 0);
        GLES20.glUniform1i(vTextureHandle, 0);

        GLES20.glEnableVertexAttribArray(vPositionHandle);
        GLES20.glVertexAttribPointer(vPositionHandle, 2, GLES20.GL_FLOAT, false, 0, bPos);

        GLES20.glEnableVertexAttribArray(vCoordinateHandle);
        GLES20.glVertexAttribPointer(vCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, bCoord);

        //传入顶点坐标
        //传入纹理坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(vPositionHandle);
        GLES20.glDisableVertexAttribArray(vCoordinateHandle);
    }

    /**
     * 相机预览纹理
     * @return
     */
    private int createTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }
}
