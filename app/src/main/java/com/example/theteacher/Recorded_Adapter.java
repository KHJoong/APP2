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
 * Created by kimhj on 2018-03-24.
 */

public class Recorded_Adapter extends BaseAdapter {

    ViewHolderRecorded vhr;

    Context raContext;

    // 서버에서 받아온 Recorded Lecture를 담아두기 위한 ArrayList입니다.
    // 이곳에 담긴 Lecture를 Listview에 뿌려줍니다.
    ArrayList<Lecture> raItem;

    Recorded_Adapter(Context co){
        raContext = co;
        raItem = new ArrayList<Lecture>();
    }

    @Override
    public int getCount() {
        return raItem.size();
    }

    @Override
    public Object getItem(int i) {
        return raItem.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;

        if(view == null){
            view = ((LayoutInflater)raContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.recorded_lecture, null);
            vhr = new ViewHolderRecorded();

            vhr.userPic = (ImageView) view.findViewById(R.id.ivPic);
            vhr.recTitle = (TextView) view.findViewById(R.id.tvTitle);
            vhr.userId = (TextView) view.findViewById(R.id.tvId);

            view.setTag(vhr);
        } else {
            vhr = (ViewHolderRecorded) view.getTag();
        }

        Glide.with(raContext).load(raItem.get(i).getTeacherPicUrl()).into(vhr.userPic);
        vhr.recTitle.setText(raItem.get(i).getLecTitle());
        vhr.userId.setText(raItem.get(i).getTeacherId());

        return view;
    }

    // Listview에 아이템을 추가합니다.
    public void addItem(Lecture lec){
        raItem.add(lec);
    }
}
