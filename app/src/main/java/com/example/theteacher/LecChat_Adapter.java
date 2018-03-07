package com.example.theteacher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by kimhj on 2018-03-06.
 */

public class LecChat_Adapter extends BaseAdapter {

    ViewHolderLecChat vhlc;

    Context lcContext;
    ArrayList<LecChat> lcItem;

    LecChat_Adapter(Context c){
        lcContext = c;
        lcItem = new ArrayList<LecChat>();
    }

    @Override
    public int getCount() {
        return lcItem.size();
    }

    @Override
    public Object getItem(int position) {
        return lcItem.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;

        if(view==null){
            view = ((LayoutInflater)lcContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.lecture_chat, null);

            vhlc = new ViewHolderLecChat();

            vhlc.userId = (TextView) view.findViewById(R.id.userId);
            vhlc.chatContent = (TextView) view.findViewById(R.id.chatContent);

            view.setTag(vhlc);
        } else {
            vhlc = (ViewHolderLecChat) view.getTag();
        }

        vhlc.userId.setText(lcItem.get(i).getUserId());
        vhlc.chatContent.setText(lcItem.get(i).getContent());

        return view;
    }

    // Listview에 아이템을 추가합니다.
    public void addItem(LecChat lc){
        lcItem.add(lc);
    }
}
