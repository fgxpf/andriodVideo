package com.boredream.videoplayer;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.boredream.bdvideoplayer.utils.StringUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static io.vov.vitamio.utils.Log.TAG;

/**
 * 数据工具类
 */
public class MockUtils {

    public static VideoDetailInfo mockData(Class<VideoDetailInfo> clazz, String path, Boolean isSD) {
        VideoDetailInfo t = null;
        try {
            t = clazz.newInstance();
            SetMockValue(t, path, isSD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    /**
     * 设置数据
     */
    private static void SetMockValue(VideoDetailInfo object, final String path, Boolean isSD) {
        File file = new File(path);
        String s = file.getName();
        String[] a = s.split("\\.", 2);
        object.setTitle(a[0]);
        int index = a[1].indexOf("?");
        if(index!=-1)
            object.setType(a[1].substring(0,index));
        else
            object.setType(a[1]);
        object.setVideoPath(path);
        object.setSD(isSD ? "true" : "false");
        MediaMetadataRetriever retriever = null;
        try {
            //MediaMetadataRetriever 是android中定义好的一个类，提供了统一
            //的接口，用于从输入的媒体文件中取得帧和元数据；
            retriever = new MediaMetadataRetriever();
            if(isSD)
            {
                object.setSize(formatSize(file.length()));
                retriever.setDataSource(path);
            }
            else
            {
                retriever.setDataSource(path, new HashMap());
            }
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            object.setDuration(StringUtils.stringForTime(Integer.valueOf(duration)));
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            if(retriever != null)
                retriever.release();
        }
    }

    public static Bitmap GetBitmap(VideoDetailInfo info) {
        MediaMetadataRetriever retriever = null;
        Bitmap bm = null;
        try {
            //MediaMetadataRetriever 是android中定义好的一个类，提供了统一
            //的接口，用于从输入的媒体文件中取得帧和元数据；
            retriever = new MediaMetadataRetriever();
            if(info.isSD().equals("true"))
                retriever.setDataSource(info.getVideoPath());
            else
                retriever.setDataSource(info.getVideoPath(), new HashMap());

            bm = retriever.getFrameAtTime();
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            if(retriever != null)
                retriever.release();
        }
        return bm;
    }

    /**
     * 向XML写入一条信息
     *
     * @param info
     */
    public static void saveOneToXml(VideoDetailInfo info) {
        List<VideoDetailInfo> infos = readXml();
        boolean isContain = false;
        for (VideoDetailInfo copyInfo : infos) {
            if(copyInfo.getVideoPath().equals(info.getVideoPath()))
                isContain = true;
        }
        if(!isContain)
        {
            infos.add(info);
        }
        saveXml(infos);
    }

    public static void saveXml(List<VideoDetailInfo> infos) {
        try {
            // 获得一个序列化工具
            XmlSerializer serializer = Xml.newSerializer();
            // 指定流目录
            OutputStream os = MainActivity.getContext().openFileOutput("videoInfo.xml", Context.MODE_PRIVATE);
            serializer.setOutput(os, "utf-8");
            // 设置文件头
            serializer.startDocument("utf-8", true);
            // 开始根标签
            // 参数一：命名空间   参数二：标签名称
            serializer.startTag(null, "items");
            for (VideoDetailInfo copyInfo : infos) {
                // 开始子标签
                serializer.startTag(null, "item");
                // 设置属性
                serializer.attribute(null, "title", copyInfo.getVideoTitle());
                serializer.attribute(null, "type", copyInfo.getType());
                serializer.attribute(null, "duration", copyInfo.getDuration() != null ? copyInfo.getDuration() : "null");
                serializer.attribute(null, "size", copyInfo.getSize() != null ? copyInfo.getSize() : "null");
                serializer.attribute(null, "path", copyInfo.getVideoPath());
                serializer.attribute(null, "isSD", copyInfo.isSD());
                //结束子标签
                serializer.endTag(null, "item");
            }
            //结束根标签
            serializer.endTag(null, "items");
            serializer.endDocument();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 删除单个文件
     * @param path 要删除的文件的路径
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String path) {
        File file = new File(path);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            Log.d(TAG, "deleteFile: "+file.getPath());
            Log.d(TAG, "deleteFile: "+file.getAbsolutePath());
            System.gc();
            if (file.getAbsoluteFile().delete()) {
                Toast.makeText(MainActivity.getContext(), "删除文件成功！", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(MainActivity.getContext(), "删除文件失败！", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Toast.makeText(MainActivity.getContext(), "文件不存在！", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 读取XML文件,使用pull解析
     */
    public static List<VideoDetailInfo> readXml() {
        List<VideoDetailInfo> infos = new ArrayList<>();
        try {
            FileInputStream is = MainActivity.getContext().openFileInput("videoInfo.xml");

            // 获得pull解析器对象
            XmlPullParser parser = Xml.newPullParser();
            // 指定解析的文件和编码格式
            parser.setInput(is, "utf-8");

            int eventType = parser.getEventType(); // 获得事件类型

            VideoDetailInfo info;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName(); // 获得当前节点的名称
                switch (eventType) {
                    case XmlPullParser.START_TAG: // 当前等于开始节点
                        if ("items".equals(tagName)) { // <items>
                        } else if ("item".equals(tagName)) { // <item>
                            info = new VideoDetailInfo();
                            info.setTitle( parser.getAttributeValue(null, "title"));
                            info.setType( parser.getAttributeValue(null, "type"));
                            if(!parser.getAttributeValue(null, "duration").equals("null"))
                                info.setDuration( parser.getAttributeValue(null, "duration"));
                            if(!parser.getAttributeValue(null, "size").equals("null"))
                                info.setSize( parser.getAttributeValue(null, "size"));
                            info.setVideoPath( parser.getAttributeValue(null, "path"));
                            info.setSD( parser.getAttributeValue(null, "isSD"));
                            infos.add(info);
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next(); // 获得下一个事件类型
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return infos;
    }
        /**
         * 显示视频信息
         * @param object
         * @return
         */
    public static String ShowTextByInfo(VideoDetailInfo object) {
        Field[] fields = VideoDetailInfo.class.getDeclaredFields();
        String str = "";
        for (Field field : fields) {
            if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            switch (field.getName())
            {
                case "title":
                    str += "<b>名称：</b>"+object.getVideoTitle()+"<br/>";
                    break;
                case "type":
                    str += "<b>格式：</b>"+object.getType()+"<br/>";
                    break;
                case "videoPath":
                    str += "<b>路径：</b>"+object.getVideoPath()+"<br/>";
                    break;
                case "size":
                    if(object.getSize() != null)
                        str += "<b>大小：</b>"+object.getSize()+"<br/>";
                    break;
                case "duration":
                    if(object.getDuration() != null)
                        str += "<b>时长：</b>"+object.getDuration()+"<br/>";
                    break;
            }
        }
        return  str;
    }

    /**
     * 解析文件路径
     * @param context
     * @param uri
     * @return
     */
    @SuppressLint("NewApi")
    public static String getPathByUri4kitkat(final Context context, final Uri uri) {
        //版本是否大于4.4
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {// ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {// DownloadsProvider
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {// MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {// MediaStore
            // (and
            // general)
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {// File
            return uri.getPath();
        }
        return null;
    }

    /**
     * @param context
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = android.provider.MediaStore.Audio.Media.DATA;
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            外部存储
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            下载
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            媒体
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static final String[] VIDEO_EXTENSIONS = {  "3gp", "amv",
            "avb", "avd", "avi", "flh", "fli", "flv", "flx", "gvi", "gvp",
            "hdmov", "hkm", "ifo", "imovi", "imovi", "iva", "ivf", "ivr",
            "m4v", "m75", "meta", "mgv", "mj2", "mjp", "mjpg", "mkv", "mmv",
            "mnv", "mod", "modd", "moff", "moi", "moov", "mov", "movie",
            "mp21", "mp21", "mp2v", "mp4", "mp4v", "mpe", "mpeg", "mpeg4",
            "mpf", "mpg", "mpg2", "mpgin", "mpl", "mpls", "mpv", "mpv2", "mqv",
            "msdvd", "msh", "mswmm", "mts", "mtv", "mvb", "mvc", "mvd", "mve",
            "mvp", "mxf", "mys", "ncor", "nsv", "nvc", "ogm", "ogv", "ogx",
            "osp", "par", "pds", "pgi", "piv", "playlist", "pmf", "prel",
            "pro", "prproj", "psh", "pva", "pvr", "pxv", "qt", "qtch", "qtl",
            "qtm", "qtz", "rcproject", "rdb", "rec", "rm", "rmd", "rmp", "rms",
            "rmvb", "roq", "rp", "rts", "rts", "rum", "rv", "sbk", "sbt",
            "scm", "scm", "scn", "sec", "seq", "sfvidcap", "smil", "smk",
            "sml", "smv", "spl", "ssm", "str", "stx", "svi", "swf", "swi",
            "swt", "tda3mt", "tivo", "tix", "tod", "tp", "tp0", "tpd", "tpr",
            "trp", "ts", "tvs", "vc1", "vcr", "vcv", "vdo", "vdr", "veg",
            "vem", "vf", "vfw", "vfz", "vgz", "vid", "viewlet", "viv", "vivo",
            "wma"
    };

    //集合放置所有支持视频格式
    private static final List<String> listvideo = new ArrayList<>(
            Arrays.asList(VIDEO_EXTENSIONS));

    // 检测是否是视频文件
    public static boolean IsVideo(String path) {
        path=getFileExtension(path);
        return listvideo.contains(path);
    }

    //   获取文件后缀名
    public static String getFileExtension(String path) {
        if (null != path) {
            // 后缀点 的位置
            int start = path.lastIndexOf(".");
            int last = path.lastIndexOf("?");
            // 截取后缀名
            if(last > -1)
            {
                start = path.lastIndexOf(".", last);
                return path.substring(start + 1, last);
            }
            else
                return path.substring(start + 1);
        }
        return null;
    }

    private static String formatSize(long target_size) {
        return Formatter.formatFileSize(MainActivity.getContext(), target_size);
    }
}
