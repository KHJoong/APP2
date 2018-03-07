package com.example.theteacher;

/**
 * Created by kimhj on 2018-03-06.
 */

public class LecChat {

    String userId;
    String content;

    public LecChat(String u, String c){
        userId = u;
        content = c;
    }

    public String getUserId(){
        return userId;
    }

    public String getContent(){
        return content;
    }
}
