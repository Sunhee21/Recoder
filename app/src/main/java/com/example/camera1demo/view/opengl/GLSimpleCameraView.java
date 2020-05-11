package com.example.camera1demo.view.opengl;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;

import com.blankj.utilcode.util.LogUtils;
import com.example.camera1demo.camera.CameraOne;
import com.example.camera1demo.camera.ICamera;
import com.example.camera1demo.camera.IShoot;
import com.example.camera1demo.utils.GL20Utils;

import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGL;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author sunhee
 * @intro
 * @date 2020/4/22
 */
public class GLSimpleCameraView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {


    private SurfaceTexture surfaceTexture;
    private int surfaceWidth;
    private int surfaceHeight;
    private float[] matrix;

    private CameraRender cameraRender;

    public GLSimpleCameraView(Context context) {
        this(context, null);
    }

    private ICamera mCamera;

    public GLSimpleCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        mCamera = new CameraOne((Activity) getContext());
        cameraRender = new CameraRender();
        try {
            MediaCodec mediaCodec = MediaCodec.createEncoderByType("video/avc");
            Surface s = mediaCodec.createInputSurface();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //<editor-fold desc="渲染相关 Render">
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int texture = cameraRender.onSurfaceCreated();
        surfaceTexture = new SurfaceTexture(texture);
        surfaceTexture.setOnFrameAvailableListener(this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LogUtils.d("GL onSurfaceChanged");
        surfaceWidth = width;
        surfaceHeight = height;
        cameraRender.onSurfaceChanged(gl, width, height);
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        if (matrix == null)return;
        LogUtils.d("GL onDrawFrame");
        surfaceTexture.updateTexImage();//这句必须在前面，不然切换摄像头会延迟一帧出现方向不对
        cameraRender.onDrawFrame(gl);
    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();//调用后会执行onDrawFrame
    }
    //</editor-fold>



    private Point previewSize;


    /**
     * openCamera 执行于 onSurfaceChanged 之后
     */
    public void openCamera() {
        LogUtils.d("GL openCamera");
        mCamera.opencamera(mCamera.getCameraId());
        previewSize = mCamera.startPreview(surfaceTexture, surfaceWidth, surfaceHeight);
        calculateMatrix();
    }


    public void switchCamera() {
        if (mCamera.getCameraId() != Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCamera.opencamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        } else {
            mCamera.opencamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
        previewSize = mCamera.startPreview(surfaceTexture, surfaceWidth, surfaceHeight);
        calculateMatrix();
        LogUtils.d("GL switchCamera");
    }


    public void stopShoot() {

    }

    public void startShoot(File file, IShoot iShoot) {

    }



    public void calculateMatrix(){
        //根据相机预览宽高，和surfaceView宽高，得到合适的展示
        matrix = GL20Utils.getShowMatrix(previewSize.x, previewSize.y, surfaceWidth, surfaceHeight);
        //根据前后置
        if (mCamera.getCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //前置左右镜像，旋转90度
            GL20Utils.flip(matrix, true, false);
            GL20Utils.rotate(matrix, 90);
        } else {
            //后置旋转270度
            GL20Utils.rotate(matrix, 270);
        }
        cameraRender.setMatrix(matrix);
    }
}
