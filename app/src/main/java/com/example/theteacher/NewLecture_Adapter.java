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
 * Created by kimhj on 2018-02-09.
 */

public class NewLecture_Adapter extends BaseAdapter {

    ViewHolderNewLecture vhnl;

    Context nlContext;

    // 서버에서 받아온 NewLecture를 담아두기 위한 ArrayList입니다.
    // 이곳에 담긴 Lecture를 Listview에 뿌려줍니다.
    ArrayList<Lecture> nlItem;

    NewLecture_Adapter(Context c){
        nlContext = c;
        nlItem = new ArrayList<Lecture>();
    }


    @Override
    public int getCount() {
        return nlItem.size();
    }

    @Override
    public Object getItem(int i) {
        return nlItem.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;

        if(view == null){
            view = ((LayoutInflater)nlContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.new_lecture, null);
            vhnl = new ViewHolderNewLecture();

            vhnl.userPic = (ImageView) view.findViewById(R.id.ivPic);
            vhnl.lecTitle = (TextView) view.findViewById(R.id.tvTitle);
            vhnl.userId = (TextView) view.findViewById(R.id.tvId);

            view.setTag(vhnl);
        } else {
            vhnl = (ViewHolderNewLecture) view.getTag();
        }

        Glide.with(nlContext).load(nlItem.get(i).getTeacherPicUrl()).into(vhnl.userPic);
        vhnl.lecTitle.setText(nlItem.get(i).getLecTitle());
        vhnl.userId.setText(nlItem.get(i).getTeacherId());

        return view;
    }

    // Listview에 아이템을 추가합니다.
    public void addItem(Lecture lec){
        nlItem.add(lec);
    }
}
