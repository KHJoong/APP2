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
 * Created by kimhj on 2018-03-13.
 */

public class Question_Adapter extends BaseAdapter {

    ViewHolderQuestion vhq;

    Context qContext;

    ArrayList<Question> qItem;

    Question_Adapter(Context con){
        qContext = con;

        qItem = new ArrayList<Question>();
    }

    @Override
    public int getCount() {
        return qItem.size();
    }

    @Override
    public Object getItem(int position) {
        return qItem.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        View view = convertView;

        if(view == null){
            view = ((LayoutInflater)qContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.question, null);
            vhq = new ViewHolderQuestion();

            vhq.ivPic = (ImageView) view.findViewById(R.id.ivPic);
            vhq.tvTitle = (TextView) view.findViewById(R.id.tvTitle);

            view.setTag(vhq);
        } else {
            vhq = (ViewHolderQuestion) view.getTag();
        }

        Glide.with(qContext).load(qItem.get(i).getQuestionPicUrl()).into(vhq.ivPic);
        vhq.tvTitle.setText(qItem.get(i).getQuestionTitle());

        return view;
    }

    // Listview에 아이템을 추가합니다.
    public void addItem(Question que){
        qItem.add(que);
    }
}
