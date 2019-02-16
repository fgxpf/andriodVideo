package com.boredream.videoplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.support.v4.os.LocaleListCompat.create;

public class adapter extends RecyclerView.Adapter {
    private List<VideoDetailInfo> mDatas;
    private Context mContext;
    protected boolean isScrolling = false;
    private Map<Integer, Bitmap> bitmapMap;

    public adapter(Context context) {
        mDatas = new ArrayList<>();
        bitmapMap = new HashMap<>();
        mContext = context;
    }

    public void setDatas(List<VideoDetailInfo> datas) {
        mDatas.clear();
        mDatas.addAll(datas);
        notifyDataSetChanged();
    }

    public void setScrolling(boolean scrolling) {
        isScrolling = scrolling;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        return new ItemBaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        ItemBaseViewHolder baseViewHolder = (ItemBaseViewHolder) holder;
        baseViewHolder.bind(mDatas.get(position));
        baseViewHolder.mViewContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VideoDetailInfo info = mDatas.get(holder.getAdapterPosition());
                VideoDetailActivity.start(mContext, info);
            }
        });
        baseViewHolder.mActionViewDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doDelete(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    private void doDelete(final int adapterPosition) {
        if(mDatas.get(adapterPosition).isSD().equals("true"))
        {
            final Boolean[] ischecked = new Boolean[]{false};
            AlertDialog dialog = new AlertDialog.Builder(mContext).setTitle("删除视频")
                    .setIcon(R.drawable.oval_gray_solid)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //处理确认按钮的点击事件
                            if(ischecked[0])
                            {
                                MockUtils.deleteFile(mDatas.get(adapterPosition).getVideoPath());
                            }
                            mDatas.remove(adapterPosition);
                            new Thread() {
                                @Override
                                public void run() {
                                    MockUtils.saveXml(mDatas);
                                }
                            }.start();
                            notifyItemRemoved(adapterPosition);
                        }
                    })
                    .setMultiChoiceItems(new String[]{"是否从本地删除？"}, null, new DialogInterface.OnMultiChoiceClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            ischecked[0] = isChecked;
                        }
                    }).create();
            dialog.show();
        }
        else
        {
            mDatas.remove(adapterPosition);
            new Thread() {
                @Override
                public void run() {
                    MockUtils.saveXml(mDatas);
                }
            }.start();
            notifyItemRemoved(adapterPosition);
        }
    }

    class ItemBaseViewHolder extends RecyclerView.ViewHolder{
        TextView mTextTitle;
        TextView mTextDuration;
        ImageView mImage;
        View mViewContent;
        View mActionViewDelete;

        public ItemBaseViewHolder(View itemView) {
            super(itemView);
            mTextTitle = (TextView) itemView.findViewById(R.id.item_name);
            mTextDuration = (TextView) itemView.findViewById(R.id.item_duration);
            mImage = (ImageView) itemView.findViewById(R.id.imageView);
            mViewContent = itemView.findViewById(R.id.item);
            mActionViewDelete = itemView.findViewById(R.id.delete);
        }

        public void bind(final VideoDetailInfo info) {
            mTextTitle.setText(info.getVideoTitle());
            mTextDuration.setText(info.getDuration() != null ? info.getDuration() : "时长未知");
            if(bitmapMap.containsKey(getAdapterPosition())){
                Bitmap bm = bitmapMap.get(getAdapterPosition());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
//                byte[] bytes = baos.toByteArray();
//                Glide.with(mContext).load(bytes).centerCrop().into(mImage);   //这个东西在刷新的时候会闪一下，不知道为什么
                mImage.setImageBitmap(bm);
            }
            else
            {
                new Thread(){
                    public void run() {
                        Bitmap bm = MockUtils.GetBitmap(info);
                        if(bm != null)
                        {
                            bitmapMap.put(getAdapterPosition(), bm);
                            Message message = handler.obtainMessage();//用handler发送通知
                            message.what = 1;
                            message.obj = bm;
                            handler.sendMessage(message);
                        }
                    }
                }.start();
            }
        }

        @SuppressLint("HandlerLeak")
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Bitmap bm = (Bitmap) msg.obj;
//                        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
//                        byte[] bytes = baos.toByteArray();
//                        Glide.with(mContext).load(bytes).centerCrop().into(mImage);
                        mImage.setImageBitmap(bm);
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }
}
