package com.example.theteacher;

import android.app.ProgressDialog;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by kimhj on 2018-03-23.
 */

// Socket을 이용하여 서버로 비디오 파일을 전송하는 Class입니다.
// 업로드하여 저장할 파일이름과 파일의 path를 받습니다.
public class VideoSocketTransfer extends Thread {

    String ip = "115.71.232.230";
    int port = 5555;

    String reTitle;
    String rePath;

    ProgressDialog progressDialog;

    public VideoSocketTransfer(ProgressDialog pd, String ti, String pa){
        reTitle = ti;
        rePath = pa;

        progressDialog = pd;
    }

    @Override
    public void run() {
        super.run();
        try {
            InetAddress server = InetAddress.getByName(ip);
            Socket socket = new Socket(server, port);

            // 서버에 저장될 파일의 이름과 파일의 크기를 전송합니다.
//            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
//            out.println(reTitle);
            long size = new File(rePath).length();
//            out.println(size);
//            out.flush();

            // 비디오를 전송하는 부분입니다.
            byte[] buffer = new byte[1024];
            FileInputStream fis = new FileInputStream(rePath);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            int bytesRead = 0;
            int count = 0;

            dos.writeUTF(reTitle);              //
            dos.writeUTF(String.valueOf(size)); //
            dos.flush();                        //

            while((bytesRead = fis.read(buffer, 0, 1024)) != -1){
                count += bytesRead;
                Log.i("bytesRead : ", String.valueOf(count));
                dos.write(buffer);
                dos.flush();
            }

//            out.close();
            dos.close();

            socket.close();
            progressDialog.dismiss();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
