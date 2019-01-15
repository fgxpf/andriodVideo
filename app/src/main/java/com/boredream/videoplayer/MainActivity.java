package com.boredream.videoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        findViewById(R.id.btn_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyStoragePermissions(MainActivity.this);
                isChooseReturn = true;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型
                intent.addCategory(Intent.CATEGORY_OPENABLE);//调用文件管理器
                startActivityForResult(intent,1);
            }
        });

        list = (ListView) findViewById(R.id.listView);
        EditText etOne= (EditText) findViewById(R.id.editText);
        etOne.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_DONE) {//EditorInfo.IME_ACTION_SEARCH、EditorInfo.IME_ACTION_SEND等分别对应EditText的imeOptions属性
                    if(v == null || v.length() == 0)
                    {
                        Toast toast = Toast.makeText(MainActivity.this, "请输入网址", Toast.LENGTH_SHORT);
                        toast.show();
                        return false;
                    }
                    String video_path = v.getText().toString();
                    Log.d("edit", "onEditorAction: "+video_path);
                    if(!MockUtils.IsVideo(video_path))
                    {
                        Toast toast = Toast.makeText(MainActivity.this, "不是视频文件或不支持的格式", Toast.LENGTH_SHORT);
                        toast.show();
                        return false;
                    }
                    VideoDetailInfo info = MockUtils.mockData(VideoDetailInfo.class, video_path, false);
                    VideoDetailActivity.start(MainActivity.this, info);
//                }
                return true;
            }
        });
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

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
    private ListView list;

    /**
     * 显示列表
     */
    private void ShowItems() {
        infos = MockUtils.readXml();
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        Map<String, Object> map;
        for(VideoDetailInfo info : infos)
        {
            map = new HashMap<>();
            map.put("name", info.getVideoTitle());
            map.put("duration", info.getDuration() != null ? info.getDuration() : "时长未知");
            map.put("image", MockUtils.GetBitmap(info));
            data.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, data,
                R.layout.item,new String[] { "image", "name", "duration" },
                new int[] { R.id.imageView,R.id.item_name,R.id.item_duration });
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (view.getId()) {
                    case R.id.close:
                        break;
                    default:
                        VideoDetailInfo info = infos.get(position);
                        VideoDetailActivity.start(MainActivity.this, info);
                        break;
                }
            }
        });
        adapter.setViewBinder(new SimpleAdapter.ViewBinder(){
            @Override
            public boolean setViewValue(View view,Object data,String textRepresentation){
                if(view instanceof ImageView && data instanceof Bitmap){
                    ImageView iv=(ImageView)view;
                    iv.setImageBitmap((Bitmap)data);
                    return true;
                }
                else return false;
            }
        });
    }

    private void ClearItems() {
//        if(infos.size() > 0)
//            infos.clear();
//        if(list.getChildCount() > 0)
//            list.getAdapter().();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ClearItems();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        new Thread() {
//            @Override
//            public void run() {
                ShowItems();
//            }
//        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
