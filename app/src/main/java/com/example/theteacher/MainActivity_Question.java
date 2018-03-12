package com.example.theteacher;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

/**
 * Created by kimhj on 2018-03-12.
 */

public class MainActivity_Question extends Fragment {

    // dialog 닫을 부분에서 qeDialog.dismiss(); 해주기
    Question_Adapter qAdapter;
    GridView gvQuestion;

    FloatingActionButton fabEnroll;

    QuestionEnroll_CustomDialog qeDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_question, container, false);

        fabEnroll = (FloatingActionButton)view.findViewById(R.id.fabEnroll);
        gvQuestion = (GridView) view.findViewById(R.id.gvQeestion);
        qAdapter = new Question_Adapter(getActivity().getApplicationContext());
        gvQuestion.setAdapter(qAdapter);

        fabEnroll.setOnClickListener(btnClickListener);

        return view;
    }

    View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.fabEnroll:
                    qeDialog = new QuestionEnroll_CustomDialog(getActivity().getApplicationContext(), goCameraLis, goGalleryLis);
                    qeDialog.setCancelable(true);
                    qeDialog.getWindow().setGravity(Gravity.CENTER);
                    qeDialog.show();
                    break;
            }
        }
    };

    View.OnClickListener goCameraLis = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    View.OnClickListener goGalleryLis = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };
}
