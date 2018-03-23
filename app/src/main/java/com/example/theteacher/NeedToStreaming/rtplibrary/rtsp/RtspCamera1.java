package com.example.theteacher.NeedToStreaming.rtplibrary.rtsp;

import android.media.MediaCodec;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.view.SurfaceView;
import android.view.TextureView;
import com.example.theteacher.NeedToStreaming.rtplibrary.base.Camera1Base;
import com.example.theteacher.NeedToStreaming.rtplibrary.view.LightOpenGlView;
import com.example.theteacher.NeedToStreaming.rtplibrary.view.OpenGlView;
import com.example.theteacher.NeedToStreaming.rtsp.rtsp.Protocol;
import com.example.theteacher.NeedToStreaming.rtsp.rtsp.RtspClient;
import com.example.theteacher.NeedToStreaming.rtsp.utils.ConnectCheckerRtsp;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * More documentation see:
 * {@link com.example.theteacher.NeedToStreaming.rtplibrary.base.Camera1Base}
 *
 * Created by pedro on 10/02/17.
 */

public class RtspCamera1 extends Camera1Base {

  private RtspClient rtspClient;
  RecordStartThread rst;

  public RtspCamera1(SurfaceView surfaceView, ConnectCheckerRtsp connectCheckerRtsp) {
    super(surfaceView);
    rtspClient = new RtspClient(connectCheckerRtsp);
  }

  public RtspCamera1(TextureView textureView, ConnectCheckerRtsp connectCheckerRtsp) {
    super(textureView);
    rtspClient = new RtspClient(connectCheckerRtsp);
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public RtspCamera1(OpenGlView openGlView, ConnectCheckerRtsp connectCheckerRtsp) {
    super(openGlView);
    rtspClient = new RtspClient(connectCheckerRtsp);
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public RtspCamera1(LightOpenGlView lightOpenGlView, ConnectCheckerRtsp connectCheckerRtsp) {
    super(lightOpenGlView);
    rtspClient = new RtspClient(connectCheckerRtsp);
  }

  /**
   * Internet protocol used.
   *
   * @param protocol Could be Protocol.TCP or Protocol.UDP.
   */
  public void setProtocol(Protocol protocol) {
    rtspClient.setProtocol(protocol);
  }

  @Override
  public void setAuthorization(String user, String password) {
    rtspClient.setAuthorization(user, password);
  }

  @Override
  protected void prepareAudioRtp(boolean isStereo, int sampleRate) {
    rtspClient.setIsStereo(isStereo);
    rtspClient.setSampleRate(sampleRate);
  }

  @Override
  protected void startStreamRtp(String url) {
    rtspClient.setUrl(url);
    if (!cameraManager.isPrepared()) {
      rtspClient.connect();
    }
    rst = new RecordStartThread(url);
    rst.start();
  }

  @Override
  protected void stopStreamRtp() {
    rtspClient.disconnect();
    stopRecord();
    if(rst.isAlive() || !rst.isInterrupted()){
      rst.interrupt();
    }
  }

  //
  public class RecordStartThread extends Thread{

    String url;
    String time;

    RecordStartThread(String u){
      url = u;

      Calendar cal = Calendar.getInstance();
      SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
      time = sdf1.format(cal.getTime());
    }

    @Override
    public void run() {
      super.run();
      while (true){
        if(rtspClient.isStreaming()){
          String[] urlArray = url.split("/");
          String fileName = urlArray[urlArray.length-1]+"_"+time+".mp4";
          File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
          System.out.println("RecordStartThread:"+file.getPath());
          try {
            startRecord(file.getPath());
            System.out.println("startSuccess");
            break;
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  @Override
  protected void getAacDataRtp(ByteBuffer aacBuffer, MediaCodec.BufferInfo info) {
    rtspClient.sendAudio(aacBuffer, info);
  }

  @Override
  protected void onSPSandPPSRtp(ByteBuffer sps, ByteBuffer pps) {
    ByteBuffer newSps = sps.duplicate();
    ByteBuffer newPps = pps.duplicate();
    rtspClient.setSPSandPPS(newSps, newPps);
    rtspClient.connect();
  }

  @Override
  protected void getH264DataRtp(ByteBuffer h264Buffer, MediaCodec.BufferInfo info) {
    rtspClient.sendVideo(h264Buffer, info);
  }
}
