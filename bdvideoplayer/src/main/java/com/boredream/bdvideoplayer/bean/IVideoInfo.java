package com.boredream.bdvideoplayer.bean;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * 持久化视频数据类
 */
public interface IVideoInfo extends Serializable {

    /**
     * 视频标题
     */
    String getVideoTitle();

    /**
     * 视频播放路径 url / file path
     */
    String getVideoPath();

    /**
     * 视频大小
     */
    String getSize();

    /**
     * 视频时长
     */
    String getDuration();
}
