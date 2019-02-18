package com.boredream.videoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rance.library.ButtonData;
import com.rance.library.ButtonEventListener;
import com.rance.library.SectorMenuButton;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class MainActivity extends AppCompatActivity {

    private static Context context;
    private boolean isChooseReturn = false;
    public static Context getContext(){
        return context;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();

        setContentView(R.layout.activity_main);
        verifyStoragePermissions(MainActivity.this);
        initSectorMenuButton();

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_main);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this));
    }

    /**
     * 初始化菜单
     */
    private void initSectorMenuButton() {
        SectorMenuButton sectorMenuButton = (SectorMenuButton) findViewById(R.id.sector_menu);
        final List<ButtonData> buttonDatas = new ArrayList<>();
        int[] drawable = {R.drawable.menu_plus, R.drawable.menu_open, R.drawable.menu_search};
        for (int i = 0; i < 3; i++) {
            //最后一个参数表示padding
            ButtonData buttonData = ButtonData.buildIconButton(this, drawable[i], 10);
            buttonData.setBackgroundColorId(this, R.color.colorAccent);
            buttonDatas.add(buttonData);
        }
        sectorMenuButton.setButtonDatas(buttonDatas);
        setListener(sectorMenuButton);
    }

    /**
     * 菜单监听事件
     * @param button
     */
    private void setListener(final SectorMenuButton button) {
        button.setButtonEventListener(new ButtonEventListener() {
            @Override
            public void onButtonClicked(int index) {
                switch (index) {
                    case 1:
                        isChooseReturn = true;
                        verifyStoragePermissions(MainActivity.this);
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");//设置类型
                        intent.addCategory(Intent.CATEGORY_OPENABLE);//调用文件管理器
                        startActivityForResult(intent,1);
                        break;
                    case 2:
                        final EditText et = new EditText(MainActivity.this);
                        et.setHint("请输入网址");
                        et.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

                        new AlertDialog.Builder(MainActivity.this).setTitle("搜索网络视频")
                                .setIcon(R.drawable.alert_point)
                                .setView(et)
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        String video_path = et.getText().toString();
                                        if(video_path == null || video_path.length() == 0)
                                        {
                                            Toast toast = Toast.makeText(MainActivity.this, "请输入网址", Toast.LENGTH_SHORT);
                                            toast.show();
                                            return;
                                        }
                                        if(!MockUtils.IsVideo(video_path))
                                        {
                                            Toast toast = Toast.makeText(MainActivity.this, "不是视频文件或不支持的格式", Toast.LENGTH_SHORT);
                                            toast.show();
                                            return;
                                        }
                                        VideoDetailInfo info = MockUtils.mockData(VideoDetailInfo.class, video_path, false);
                                        VideoDetailActivity.start(MainActivity.this, info);
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                        break;
                }
            }
            @Override
            public void onExpand() {
            }
            @Override
            public void onCollapse() {
            }
        });
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MOUNT_UNMOUNT_FILESYSTEMS"};

    /**
     * 动态获取权限
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
            try {
                //检测是否有读写的权限
                int write = ActivityCompat.checkSelfPermission(activity,
                        "android.permission.WRITE_EXTERNAL_STORAGE");
                int read = ActivityCompat.checkSelfPermission(activity,
                        "android.permission.READ_EXTERNAL_STORAGE");
                if (write != PackageManager.PERMISSION_GRANTED || read != PackageManager.PERMISSION_GRANTED) {
                    // 没有读写的权限，去申请读写的权限，会弹出对话框
                    ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
                }
            } catch (Exception e) {
                e.printStackTrace();
             }
        }

    /**
     * 选择文件返回
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {//是否选择，没选择就不会继续
            isChooseReturn = false;
            Uri uri = data.getData();//得到uri
            String video_path = MockUtils.getPathByUri4kitkat(MainActivity.this, uri);
            if(!MockUtils.IsVideo(video_path))
            {
                Toast toast = Toast.makeText(MainActivity.this, "不是视频文件或不支持的格式", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            VideoDetailInfo info = MockUtils.mockData(VideoDetailInfo.class, video_path, true);
            VideoDetailActivity.start(MainActivity.this, info);
        }
    }

    private List<VideoDetailInfo> infos;
    private RecyclerView mRecyclerView;
    private adapter mAdapter;

    /**
     * 显示列表
     */
    private void ShowItems() {
        infos = MockUtils.readXml();
        mAdapter = new adapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setDatas(infos);
        ((DefaultItemAnimator)mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                // 查看源码可知State有三种状态：SCROLL_STATE_IDLE（静止）、SCROLL_STATE_DRAGGING（上升）、SCROLL_STATE_SETTLING（下落）
                if (newState == SCROLL_STATE_IDLE) { // 滚动静止时才加载图片资源，极大提升流畅度
                    mAdapter.setScrolling(false);
                    mAdapter.notifyItemRangeChanged(0,mAdapter.getItemCount()); // notify调用后onBindViewHolder会响应调用
                } else
                    mAdapter.setScrolling(true);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    private void ClearItems() {
        if(infos.size() > 0)
            infos.clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ClearItems();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ShowItems();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
