package com.boredream.videoplayer;

import com.boredream.bdvideoplayer.bean.IVideoInfo;

/**
 * 视频数据
 */
public class VideoDetailInfo implements IVideoInfo {

    private String title;
    private String type;
    private String duration;
    private String size;
    private String videoPath;
    private String isSD;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String isSD() {
        return isSD;
    }

    public void setSD(String SD) {
        isSD = SD;
    }

    @Override
    public String getVideoTitle() {
        return title;
    }

    @Override
    public String getVideoPath() {
        return videoPath;
    }

    @Override
    public String getSize() {
        return size;
    }

    @Override
    public String getDuration() {
        return duration;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
