package com.example.camera1demo.record;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

public class NV21ToBitmap {

    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;
    private int width;
    private int height;

    public NV21ToBitmap(Context context,int width,int height) {
        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        this.width = width;
        this.height = height;
    }


    public boolean checkSize(int width,int height){
        return  this.width == width && this.width == height;
    }

    public Bitmap nv21ToBitmap(byte[] nv21){
        if (yuvType == null){
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);

        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        out.copyTo(bmpout);

        return bmpout;

    }
//    public byte[] nv21ToBitmap(byte[] nv21, int width, int height){
//        if (yuvType == null){
//            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
//            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
//
//            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
//            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
//        }
//
//        in.copyFrom(nv21);
//
//        yuvToRgbIntrinsic.setInput(in);
//        yuvToRgbIntrinsic.forEach(out);
//
//        byte[] bmpout = new byte[out.getBytesSize()];
//
//        out.copyTo(bmpout);
//
//        return bmpout;
//
//    }

}