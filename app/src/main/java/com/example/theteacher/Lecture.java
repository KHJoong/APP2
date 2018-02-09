package com.example.theteacher;

/**
 * Created by kimhj on 2018-02-08.
 */

public class Lecture {

    String teacherPicUrl;
    String teacherId;
    String lecTitle;
    String lecObject;
    String lecExplain;
    String lecTime;

    Lecture(String lt, String lo, String le, String lti){
        lecTitle = lt;
        lecObject = lo;
        lecExplain = le;
        lecTime = lti;
    }

    Lecture(String tpu, String ti, String lt, String lo, String le, String lti){
        teacherPicUrl = tpu;
        teacherId = ti;
        lecTitle = lt;
        lecObject = lo;
        lecExplain = le;
        lecTime = lti;
    }

    public String getTeacherPicUrl(){
        return teacherPicUrl;
    }

    public String getTeacherId(){
        return teacherId;
    }

    public String getLecTitle(){
        return lecTitle;
    }

    public String getLecObject(){
        return lecObject;
    }

    public String getLecExplain(){
        return lecExplain;
    }

    public String getLecTime() {
        return lecTime;
    }

}
