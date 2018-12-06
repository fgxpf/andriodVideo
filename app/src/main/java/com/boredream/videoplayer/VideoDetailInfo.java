package com.boredream.videoplayer;

import com.boredream.bdvideoplayer.bean.IVideoInfo;

/**
 * 视频数据
 */
public class VideoDetailInfo implements IVideoInfo {

    public String title;
    public String videoPath;

    @Override
    public String getVideoTitle() {
        return title;
    }

    @Override
    public String getVideoPath() {
        return videoPath;
    }
}
