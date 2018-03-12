package com.example.theteacher;

/**
 * Created by kimhj on 2018-03-13.
 */

public class Question {

    String questionPicUrl;
    String questionTitle;

    Question(String pu, String t){
        questionPicUrl = pu;
        questionTitle = t;
    }

    public String getQuestionPicUrl(){
        return questionPicUrl;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }
}
