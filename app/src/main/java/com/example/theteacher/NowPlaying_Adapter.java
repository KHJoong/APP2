package com.example.theteacher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Created by kimhj on 2018-03-01.
 */

public class NowPlaying_Adapter extends BaseAdapter {

    ViewHolderNowPlaying vhnp;
    Context npContext;

    // 서버에서 받아온 강의중인 목록을 담아두기 위한 ArrayList입니다.
    // 이곳에 담긴 Lecture를 Listview에 뿌려줍니다.
    ArrayList<Lecture> npItem;

    NowPlaying_Adapter(Context context){
        npContext = context;
        npItem = new ArrayList<Lecture>();
    }

    @Override
    public int getCount() {
        return npItem.size();
    }

    @Override
    public Object getItem(int position) {
        return npItem.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;

        if(view==null){
            view = ((LayoutInflater)npContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.nowplaying_lecture, null);

            vhnp = new ViewHolderNowPlaying();

            vhnp.userPic = (ImageView) view.findViewById(R.id.ivPic);
            vhnp.lecTitle = (TextView) view.findViewById(R.id.tvTitle);
            vhnp.userId = (TextView) view.findViewById(R.id.tvId);
            vhnp.startTime = (TextView) view.findViewById(R.id.tvTime);

            view.setTag(vhnp);
        } else {
            vhnp = (ViewHolderNowPlaying) view.getTag();
        }

        Glide.with(npContext).load(npItem.get(i).getTeacherPicUrl()).into(vhnp.userPic);
        vhnp.lecTitle.setText(npItem.get(i).getLecTitle());
        vhnp.userId.setText(npItem.get(i).getTeacherId());
        vhnp.startTime.setText(npItem.get(i).getLecTime());

        return view;
    }

    // Listview에 아이템을 추가합니다.
    public void addItem(Lecture lec){
        npItem.add(lec);
    }
}
