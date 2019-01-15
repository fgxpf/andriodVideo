package com.boredream.bdvideoplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import io.vov.vitamio.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import io.vov.vitamio.widget.VideoView;

import com.boredream.bdvideoplayer.bean.IVideoInfo;
import com.boredream.bdvideoplayer.listener.OnVideoControlListener;
import com.boredream.bdvideoplayer.listener.SimplePlayerCallback;
import com.boredream.bdvideoplayer.utils.NetworkUtils;
import com.boredream.bdvideoplayer.view.VideoBehaviorView;
import com.boredream.bdvideoplayer.view.VideoControllerView;
import com.boredream.bdvideoplayer.view.VideoProgressOverlay;
import com.boredream.bdvideoplayer.view.VideoSystemOverlay;

import java.util.logging.Logger;

import static io.vov.vitamio.utils.Log.TAG;

/**
 * 视频播放器View
 */
public class BDVideoView extends VideoBehaviorView{

    private VideoView mSurfaceView;
    private View mLoading;
    private VideoControllerView mediaController;
    private VideoSystemOverlay videoSystemOverlay;
    private VideoProgressOverlay videoProgressOverlay;
    private BDVideoPlayer mMediaPlayer;

    private int initWidth;
    private int initHeight;

    public boolean isLock() {
        return mediaController.isLock();
    }

    public BDVideoView(Context context) {
        super(context);
        init(context);
    }

    public BDVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BDVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化控件
     */
    private void init(Context context) {
        //获取当前活动的xml布局文件的信息
        LayoutInflater inflater = LayoutInflater.from(getContext());
        //动态加载布局
        inflater.inflate(R.layout.video_view, this);

        mSurfaceView = (VideoView) findViewById(R.id.video_surface);
        mLoading = findViewById(R.id.video_loading);
        mediaController = (VideoControllerView) findViewById(R.id.video_controller);
        videoSystemOverlay = (VideoSystemOverlay) findViewById(R.id.video_system_overlay);
        videoProgressOverlay = (VideoProgressOverlay) findViewById(R.id.video_progress_overlay);

        initPlayer(context);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                holder.setFormat(PixelFormat.RGBX_8888);
                initWidth = getWidth();
                initHeight = getHeight();

                if (mMediaPlayer != null) {
                    mMediaPlayer.setDisplay(holder);
                    mMediaPlayer.openVideo();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        registerNetChangedReceiver();
    }

    /**
     * 初始化播放器
     */
    private void initPlayer(Context context) {
        mMediaPlayer = new BDVideoPlayer(context);
        mMediaPlayer.setCallback(new SimplePlayerCallback() {

            @Override
            public void onStateChanged(int curState) {
                switch (curState) {
                    case BDVideoPlayer.STATE_IDLE:
                        am.abandonAudioFocus(null);//释放音频焦点
                        break;
                    case BDVideoPlayer.STATE_PREPARING:
                        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);//获取音频焦点
                        break;
                }
            }

            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                ChangeSize(width, height);
            }

            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaController.updatePausePlay();
            }

            @Override
            public void onError(MediaPlayer mp, int what, int extra) {
                mediaController.checkShowError(false);
            }

            @Override
            public void onLoadingChanged(boolean isShow) {
                if (isShow) showLoading();
                else hideLoading();
            }

            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();
                mediaController.show();
                mediaController.hideErrorView();
            }
        });
        mediaController.setMediaPlayer(mMediaPlayer);
    }

    /**
     * 显示加载进度条
     */
    private void showLoading() {
        mLoading.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        mLoading.setVisibility(View.GONE);
    }

    private boolean isBackgroundPause;

    public void onStop() {
        if (mMediaPlayer.isPlaying()) {
            // 如果已经开始且在播放，则暂停同时记录状态
            isBackgroundPause = true;
            mMediaPlayer.pause();
        }
    }

    public void onStart() {
        if (isBackgroundPause) {
            // 如果切换到后台暂停，后又切回来，则继续播放
            isBackgroundPause = false;
            mMediaPlayer.start();
        }
    }

    public void onDestroy() {
        mMediaPlayer.stop();
        mediaController.release();
        unRegisterNetChangedReceiver();
    }

    /**
     * 开始播放
     */
    public void startPlayVideo(final IVideoInfo video) {
        if (video == null) {
            return;
        }

        mMediaPlayer.reset();

        String videoPath = video.getVideoPath();
        mediaController.setVideoInfo(video);
        mMediaPlayer.setVideoPath(videoPath);
    }

    /**
     * 抬起
     * @param e
     * @return
     */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        mediaController.toggleDisplay();
        return super.onSingleTapUp(e);
    }

    /**
     * 按下
     * @param e
     * @return
     */
    @Override
    public boolean onDown(MotionEvent e) {
        if (isLock()) {
            return false;
        }
        return super.onDown(e);
    }

    /**
     * 双击
     * @param motionEvent
     * @return
     */
    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        if (isLock())
            return false;
        mediaController.doPauseResume();
        return super.onDoubleTap(motionEvent);
    }

    /**
     * 滑动
     * @param e1
     * @param e2
     * @param distanceX
     * @param distanceY
     * @return
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (isLock()) {
            return false;
        }
        return super.onScroll(e1, e2, distanceX, distanceY);
    }

    /**
     * 结束手势
     * @param behaviorType
     */
    @Override
    protected void endGesture(int behaviorType) {
        switch (behaviorType) {
            case VideoBehaviorView.FINGER_BEHAVIOR_BRIGHTNESS:
            case VideoBehaviorView.FINGER_BEHAVIOR_VOLUME:
                Log.i("DDD", "endGesture: left right");
                videoSystemOverlay.hide();
                break;
            case VideoBehaviorView.FINGER_BEHAVIOR_PROGRESS:
                Log.i("DDD", "endGesture: bottom");
                mMediaPlayer.seekTo(videoProgressOverlay.getTargetProgress());
                videoProgressOverlay.hide();
                break;
        }
    }

    /**
     * 更新进度UI
     * @param delProgress
     */
    @Override
    protected void updateSeekUI(int delProgress) {
        videoProgressOverlay.show(delProgress, mMediaPlayer.getCurrentPosition(), mMediaPlayer.getDuration());
    }

    /**
     * 更新音量UI
     * @param max
     * @param progress
     */
    @Override
    protected void updateVolumeUI(int max, int progress) {
        videoSystemOverlay.show(VideoSystemOverlay.SystemType.VOLUME, max, progress);
    }

    /**
     * 更新亮度UI
     * @param max
     * @param progress
     */
    @Override
    protected void updateLightUI(int max, int progress) {
        videoSystemOverlay.show(VideoSystemOverlay.SystemType.BRIGHTNESS, max, progress);
    }

    /**
     * 设置视频控制监听器
     * @param onVideoControlListener
     */
    public void setOnVideoControlListener(OnVideoControlListener onVideoControlListener) {
        mediaController.setOnVideoControlListener(onVideoControlListener);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            getLayoutParams().width = initWidth;
            getLayoutParams().height = initHeight;
        } else {
            getLayoutParams().width = FrameLayout.LayoutParams.MATCH_PARENT;
            getLayoutParams().height = FrameLayout.LayoutParams.MATCH_PARENT;
        }
        ChangeSize(mMediaPlayer.getPlayer().getVideoWidth(), mMediaPlayer.getPlayer().getVideoHeight());
    }

    public void ChangeSize(int width, int height) {
        if (width == 0 || height == 0) {
            Log.e(TAG, "invalid video width(" + width + ") or height(" + height
                    + ")");
            return;
        }

        //播放器的尺寸
        int mSurfaceViewWidth;
        int mSurfaceViewHeight;

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        if(width < height)
        {
            if (getResources().getConfiguration().orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                //竖屏时，使用初始尺寸
                mSurfaceViewWidth = initWidth;
                mSurfaceViewHeight = initHeight;
            } else{
                //横屏时，获取屏幕尺寸
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                mSurfaceViewWidth = displayMetrics.widthPixels;
                mSurfaceViewHeight = displayMetrics.heightPixels;
            }
            int w = mSurfaceViewHeight * width / height;
            int margin = (mSurfaceViewWidth - w) / 2;
            lp.setMargins(margin, 0, margin, 0);
        }

        mSurfaceView.setLayoutParams(lp);
    }

    private NetChangedReceiver netChangedReceiver;
    /**
     * 注册接收器
     */
    public void registerNetChangedReceiver() {
        if (netChangedReceiver == null) {
            netChangedReceiver = new NetChangedReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            activity.registerReceiver(netChangedReceiver, filter);
        }
    }

    /**
     * 取消接收器
     */
    public void unRegisterNetChangedReceiver() {
        if (netChangedReceiver != null) {
            activity.unregisterReceiver(netChangedReceiver);
        }
    }

    /**
     * 接收器
     */
    private class NetChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Parcelable extra = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (extra != null && extra instanceof NetworkInfo) {
                NetworkInfo netInfo = (NetworkInfo) extra;

                if (NetworkUtils.isNetworkConnected(context) && netInfo.getState() != NetworkInfo.State.CONNECTED) {
                    // 网络连接的情况下只处理连接完成状态
                    return;
                }

                mediaController.checkShowError(true);
            }
        }
    }
}
