package com.example.theteacher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by kimhj on 2018-02-08.
 */

// 강의 관리 페이지의 등록한 강의 부분에 있는 ListView에 매핑될 Adapter입니다.
// LectureManageActivity에서 사용합니다.
public class Lecture_Adapter extends BaseAdapter {

    Context lContext;

    // Lecture를 담아두기 위한 ArrayList입니다.
    // 이곳에 담긴 Lecture를 Listview에 뿌려줍니다.
    ArrayList<Lecture> lItem;

    // 강의 작성자가 볼 것이기 때문에 제목만 보여주면 될 것 같습니다.
    TextView tvLecTitle;

    Lecture_Adapter(Context c){
        lContext = c;
        lItem = new ArrayList<Lecture>();
    }

    @Override
    public int getCount() {
        return lItem.size();
    }

    @Override
    public Object getItem(int position) {
        return lItem.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        view = ((LayoutInflater)lContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.lecture, null);

        tvLecTitle = (TextView)view.findViewById(R.id.tvLecTitle);
        tvLecTitle.setText(lItem.get(position).getLecTitle());
        return view;
    }

    // Listview에 아이템을 추가합니다.
    // 가장 마지막에 추가되는 아이템이 맨 위로 올라가도록 0번에 추가합니다.
    public void addItem(Lecture lec){
        lItem.add(0, lec);
    }
}
