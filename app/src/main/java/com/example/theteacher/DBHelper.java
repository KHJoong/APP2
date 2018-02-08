package com.example.theteacher;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.ListView;

/**
 * Created by kimhj on 2018-02-08.
 */

public class DBHelper extends SQLiteOpenHelper{

    public static final String DB_NAME = "Lecture.db";
    public static final int DB_VERSION = 1;

    Context dbContext;

    public DBHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
        dbContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS lecture (title String, obj TEXT, exp TEXT, time String);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    // 강의 제목, 강의 목표, 강의 설명 저장하기
    // ti가 제목, obj가 목표, exp가 설명입니다.
    public void insertLecture(String ti, String obj, String exp, String tim){
        SQLiteDatabase db = getWritableDatabase();

        db.execSQL("INSERT INTO lecture VALUES('"+ti+"', '"+obj+"', '"+exp+"', '"+tim+"');");
    }

    // 강의 설명
    public void selectLecture(ListView lv, Lecture_Adapter la){
        SQLiteDatabase db = getWritableDatabase();

        String query = "SELECT * FROM lecture ORDER BY time ASC";
        Cursor c = db.rawQuery(query, null);
        if(c.moveToFirst()){
            do{
                String tit = c.getString(0);
                String ob = c.getString(1);
                String ex = c.getString(2);
                String tim = c.getString(3);

                Lecture lecture = new Lecture(tit, ob, ex, tim);
                la.addItem(lecture);
            }while(c.moveToNext());
        }
        lv.setAdapter(la);
        c.close();
        db.close();
    }
}
