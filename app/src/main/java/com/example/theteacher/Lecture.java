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

    // 여기서 int i는 4개의 속성을 갖는 객체를 만들고 싶은데 위와 구분하기 위해서 넣어준 것입니다.
    // 생성할 때 1(아무 정수)을 넣어서 생성해서 사용하면 됩니다.
    Lecture(String tpu, String ti, String ltt, String ltm, int i){
        teacherPicUrl = tpu;
        teacherId = ti;
        lecTitle = ltt;
        lecTime = ltm;
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
