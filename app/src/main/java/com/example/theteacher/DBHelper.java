package com.example.theteacher;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.ListView;

/**
 * Created by kimhj on 2018-02-08.
 */

// SQLite를 사용하기 위한 클래스입니다.
// LectureManageActivity에서 자신이 강의를 등록할 때, 자신이 등록한 강의를 불러올 때 사용합니다.
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

    // 기존에 저장해둔 강의를 불러와서 listview에 띄워주는 부분입니다.
    // LectureManageActivity에는 일단 제목만 보이지만 Lecture 객체를 만들때는 값을 모두 넣어서 만듭니다.
    // 후에 listview 아이템을 클릭했을 때의 액션을 만들어야 할 경우 사용할 예정입니다.
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
