package com.example.vkeyboard_service;

import android.os.Bundle;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.graphics.BitmapFactory;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.os.Handler;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.security.*;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import android.os.Environment;
import android.media.MediaPlayer;
import android.app.Notification;
import android.app.NotificationManager;

public class VKeyboardService extends Service {
  VoiceKeyboardService ap;
 
    @Override
    public void onCreate() {
      super.onCreate();
      ap = new VoiceKeyboardService();
      ap.SERVICE = this;
    }
 
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ap.Start();
        return START_STICKY;
    }
 
    @Override
    public void onDestroy() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
class VoiceKeyboardService{
Service SERVICE;
int SERICE_ID=655;

// 変数
Handler handler = new Handler();
NotificationManager notifManager;
Notification notif;
double[] voice;      // 録音した音声
double[] hosei;      // 聴覚感度補正係数
int voice_no = 0;  // 音声番号
int tsize = 0;       // テンプレートのサイズ
boolean is_running = false; // 実行中フラグ
boolean is_active=false;  // キーボード有効

// 定数
static final int SAMPLING_RATE = 16000;
static final int HEARING_WIDTH = 40;
static final int HEARING_HEIGHT = 128;
static final int SOUND_DFT_SIZE = 256;
static final int TRIGGER_MARGIN = 2;

// デバッグオブジェクト
String DEBUG_FILE = "debug.txt";
FileWriter dbg;
boolean debug_mode = false;
public void dprint(String s){
  if(debug_mode){
    try{
      dbg.write(s);
      dbg.flush();
    } catch(Exception e){}
  }
}

// 各種パラメータ
int bright = 0;  // 声紋表示の輝度調節
int startup_time = 400;
boolean flog_scale = false;//周波数対数スケール
boolean alog_scale = false;//振幅対数スケール
boolean auto_learn = false; // 自動学習有効
double sound_filter = 200;
double thresh_trigger_on = 3;
double thresh_trigger_off = 3;
int thresh_count_on = 4;
int thresh_count_off = 10;
double thresh_recognize = 0.1;
double bias = 3; // ノイズ抑制用バイアス
double acompress = 0.3;  // 振幅圧縮係数
double learn_param_o = 8;   // 学習パラメータ
double learn_param_x =128; // 学習パラメータ
double limit_length = 1.2; // 音声の長さ比較用


// 音声テンプレート
Vector voice_template;
class VoiceTemplate{
  String text;    // 表示テキスト
  double weight; // 重み係数(音声認識の優先度)
  int code;        // キーコード
  double[] voice; // 音声データ
  VoiceTemplate( String t, double w, int c, double[] v ){
    text = t;
    weight = w;
    code = c;
    voice = v;  
  }
}


// 音声テンプレートファイル
File voice_data_file = new File( Environment.getExternalStorageDirectory(),"VoiceData.txt" );

// 音声キーボードサービスプログラム for android  ver 0.2.3
// 変更点：
// 学習アルゴリズムを改良
// 自動学習機能を追加
// 音声認識アルゴリズムを改良
// ADB接続でキー入力コマンドを実行するのでroot権限が必要でなくなった

public void Start(){
IFile_IO.load();
}
public void save(){
IFile_IO.save();
}
Control IControl;
class Control{
VoiceKeyboardService parent;
// 三角関数テーブル
double[][] sin_table, cos_table;

// 録音スレッド
RecordThread record_thread = null;
class RecordThread extends Thread {

  int state;
  static final int NEUTRAL = 0;
  static final int OFF = 1;
  static final int SENS_ON = 2;
  static final int ON = 3;
  static final int SENS_OFF = 4;
  int HEARING_BUFFER_SIZE = 65536;

  public void run() {
    int i, j, k, len, width, height, state, offset, count;
    int start_point, end_point;
    double   a, u, v, x, y, trigger, pow;

dprint("start rec thread\n");

    // 音声データのバッファサイズ (byte)を設定
    int audio_buffer_size = android.media.AudioRecord.getMinBufferSize(
                     SAMPLING_RATE,
                     AudioFormat.CHANNEL_IN_MONO,
                     AudioFormat.ENCODING_PCM_16BIT
    );

    // AudioRecordの作成
     AudioRecord audioRec = new AudioRecord(
                     MediaRecorder.AudioSource.MIC,
                     SAMPLING_RATE,
                     AudioFormat.CHANNEL_IN_MONO,
                     AudioFormat.ENCODING_PCM_16BIT,
                     audio_buffer_size
     );
    audioRec.startRecording();	// 録音開始
    state = NEUTRAL;
    offset = 0;
    count = 0;
    start_point = end_point = 0;
    short[]  sound_buffer   = new short[SOUND_DFT_SIZE];
    double[] sound_av       = new double[HEARING_HEIGHT];
    double[] hearing_buffer = new double[HEARING_BUFFER_SIZE];

    while(is_running){       

      // 録音データを読み込む(SOUND_DFT_SIZEは配列要素の数)
      audioRec.read(sound_buffer, 0, SOUND_DFT_SIZE);

      // 取り込んだ聴覚データをDFTして開けておいたところにセットする
      // 振幅が対数スケールの場合
      if(alog_scale){
        trigger = 0.0;
        for( i = 0; i < HEARING_HEIGHT; i++ ){
          for( x = y = 0.0, j = 0; j < SOUND_DFT_SIZE; j++ ){
            a = (double)sound_buffer[j];
            x += a * cos_table[i][j];
            y += a * sin_table[i][j];
          }
          pow = Math.log((x * x + y * y) + bias);       // 微小なノイズを抑制するためバイアスをかける
          hearing_buffer[i + offset] = pow * hosei[i];  // 必要ならば人間の聴覚に合わせて補正をかける
          sound_av[i] = ((sound_filter - 1.0) * sound_av[i] + pow) / sound_filter;
          pow /= sound_av[i];
          if(pow > trigger) trigger = pow;
        }
      }

      // 振幅がリニアスケールの場合
      else{
        trigger = 0.0;
        for( i = 0; i < HEARING_HEIGHT; i++ ){
          for( x = y = 0.0, j = 0; j < SOUND_DFT_SIZE; j++ ){
            a = (double)sound_buffer[j];
            x += a * cos_table[i][j];
            y += a * sin_table[i][j];
          }
          pow = Math.pow( (x * x + y * y), acompress );
          hearing_buffer[i + offset] = pow * hosei[i];  // 必要ならば人間の聴覚に合わせて補正をかける
          sound_av[i] = ((sound_filter - 1.0) * sound_av[i] + pow) / sound_filter;
          pow /= sound_av[i];
          if(pow > trigger) trigger = pow;
        }
      }
      
      // 状態ごとの動作を以下に記述
      switch(state){
          
      // NEWTRAL状態：起動してしばらくは各フィルタ変数が安定するのを待つ
      case NEUTRAL:
        if(count > startup_time){
           state = OFF;
        }
        break;

      // OFF状態
      case OFF:
        if(trigger > thresh_trigger_on){
          state = SENS_ON;
          start_point = offset - TRIGGER_MARGIN*HEARING_HEIGHT;  // スレッショルドを越える直前をサンプリング開始位置とする
          if(start_point < 0) start_point += HEARING_BUFFER_SIZE;
          count = 0;
        }
        break;

      // OFFからONに遷移する状態
      case SENS_ON:
        if(trigger < thresh_trigger_off)  state = OFF;
        else{
          if(count > thresh_count_on) state = ON;
        }
        break;

      // ON状態
      case ON:
        if(trigger < thresh_trigger_off){
          state = SENS_OFF;
          end_point = offset + TRIGGER_MARGIN*HEARING_HEIGHT; // スレッショルドを下回った所をサンプリング終了位置とする
          if(end_point >= HEARING_BUFFER_SIZE) end_point -= HEARING_BUFFER_SIZE;
          count = 0;
        }
        break;

      // ONからOFFに遷移する状態
      case SENS_OFF:
        if(trigger > thresh_trigger_on)  state = ON;
        else{
          if(count > thresh_count_off){

            // サンプリング範囲内のデータを音声データ配列voiceに転送する
            len = end_point - start_point;
            if(len < 0) len += HEARING_BUFFER_SIZE;
            voice = new double[len];
            for(i = start_point, j = 0; j < len; i++, j++){
              if(i >= HEARING_BUFFER_SIZE) i -= HEARING_BUFFER_SIZE;
              voice[j] = hearing_buffer[i];
            }
            state = OFF;

            // 認識処理開始
            handler.post(new Runnable() {
              @Override
              public void run(){
                recognize(voice);
              }
            });
          }
        }
        break;

      }

      offset += HEARING_HEIGHT;
      if( offset >= HEARING_BUFFER_SIZE ) offset = 0;
      count++;
    }
    audioRec.stop();	// 録音終了
    audioRec.release();

  }//run()

}//RecordThread

// 2つの音声を比較して相関値を返す(offsetはずらす位置)
public double compare_voice( double[] ref, double[] voice, int offset ){

  int x, y, width, height;
  double a = 0.0, nn = 0.0, ar = 0.0, av = 0.0, pr = 0.0, pv = 0.0;

  if( voice == null || ref == null ) return 0.0;
  if( voice.length <= 0 || ref.length <=0 ) return 0.0;
    
  // 右側にずらす場合
  if( offset >=1 ){
    if( voice.length <= offset * HEARING_HEIGHT ) return 0.0;
    
    // ずらす voice->xvoice
    double[] xvoice = new double[voice.length - offset * HEARING_HEIGHT];
    for( int i = 0; i < xvoice.length; i++ ) xvoice[i] = voice[i + offset * HEARING_HEIGHT];

    width = ref.length / HEARING_HEIGHT;
    if( ( x= xvoice.length / HEARING_HEIGHT ) < width ) width = x;
    height  = HEARING_HEIGHT;
    nn = (double)( width * height );

    // ref[]の平均値を求める
    ar = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        ar += ref[ HEARING_HEIGHT * x + y ];
      }
    }
    ar /= nn; 

    // xvoice[]の平均値を求める
    av = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        av += xvoice[ HEARING_HEIGHT * x + y ];
      }
    }
    av /= nn; 

    // ref[]のパワーを求める
     pr = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        a = ref[ HEARING_HEIGHT * x + y ] - ar;
        pr += a * a;
      }
    }
    if( pr == 0.0 ) return 0.0;

    // xvoice[]のパワーを求める
    pv = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        a = xvoice[ HEARING_HEIGHT * x + y ] - av;
        pv += a * a;
      }
    }
    if( pv == 0.0 ) return 0.0;

    // ref[]とxvoice[]の相関値を求める
    a = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        a += ( ref[ HEARING_HEIGHT * x + y ] - ar ) * ( xvoice[ HEARING_HEIGHT * x + y ] - av ) ;
      }
    }
    return a / Math.sqrt( pv * pr );
  }


  // 左側にずらす場合
  else if( offset <= -1 ){
    if( ref.length <= -offset * HEARING_HEIGHT ) return 0.0;
    
    // ずらす ref->xref
    double[] xref = new double[ref.length + offset * HEARING_HEIGHT];
    for( int i = 0; i < xref.length; i++ ) xref[i] = ref[i - offset * HEARING_HEIGHT];

    width  = xref.length / HEARING_HEIGHT;
    if( ( x= voice.length / HEARING_HEIGHT ) < width ) width = x;
    height  = HEARING_HEIGHT;
    nn = (double)( width * height );

    // xref[]の平均値を求める
    ar = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        ar += xref[ HEARING_HEIGHT * x + y ];
      }
    }
    ar /= nn; 

    // voice[]の平均値を求める
    av = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        av += voice[ HEARING_HEIGHT * x + y ];
      }
    }
    av /= nn; 

    // xref[]のパワーを求める
    pr = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        a = xref[ HEARING_HEIGHT * x + y ] - ar;
        pr += a * a;
      }
    }
    if( pr == 0.0 ) return 0.0;

    // voice[]のパワーを求める
    pv = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        a = voice[ HEARING_HEIGHT * x + y ] - av;
        pv += a * a;
      }
    }
    if( pv == 0.0 ) return 0.0;

    // xref[]とvoice[]の相関値を求める
    a = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        a += ( xref[ HEARING_HEIGHT * x + y ] - ar ) * ( voice[ HEARING_HEIGHT * x + y ] - av ) ;
      }
    }
    return a / Math.sqrt( pv * pr );
  }

  // ずらさない場合
  else{

    width  = ref.length / HEARING_HEIGHT;
    if( ( x= voice.length / HEARING_HEIGHT ) < width ) width = x;
    height  = HEARING_HEIGHT;
    nn = (double)( width * height );

    // ref[]の平均値を求める
    ar = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        ar += ref[ HEARING_HEIGHT * x + y ];
      }
    }
    ar /= nn; 

    // voice[]の平均値を求める
    av = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        av += voice[ HEARING_HEIGHT * x + y ];
      }
    }
    av /= nn; 

    // ref[]のパワーを求める
    pr = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        a = ref[ HEARING_HEIGHT * x + y ] - ar;
        pr += a * a;
      }
    }
    if( pr == 0.0 ) return 0.0;

    // voice[]のパワーを求める
    pv = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        a = voice[ HEARING_HEIGHT * x + y ] - av;
        pv += a * a;
      }
    }
    if( pv == 0.0 ) return 0.0;

    // ref[]とvoice[]の相関値を求める
    a = 0.0;
    for( x = 0; x < width; x++ ){
      for( y = 0; y < height; y++ ){
        a += ( ref[ HEARING_HEIGHT * x + y ] - ar ) * ( voice[ HEARING_HEIGHT * x + y ] - av ) ;
      }
    }
    return a / Math.sqrt( pv * pr );
  }

}

// 音声の認識
public void recognize( double[] voice) {
    
  int maxi = 0;
  double r = 0.0, max = 0.0;
  for( int i = 0; i < voice_template.size(); i++ ){

    // テンプレートの音声
    double[] ref = ((VoiceTemplate)(voice_template.get(i))).voice;

    // 音声の長さを比べて範囲内なら比較する
    double p = (double)ref.length / voice.length;
    if(p < limit_length && 1/p < limit_length){

      // 重み(大きいほど優先順位が高くなる)
      double w = ((VoiceTemplate)(voice_template.get(i))).weight;

      // テンプレートと録音した音声の相関値を計算する(トリガタイミングの誤差を考慮して少しずつずらして比較して一番大きいのをとる)
      r = compare_voice( ref, voice, -2 ) * w;
      if(r > max){ max = r; maxi = i; }

      r = compare_voice( ref, voice, -1 ) * w;
      if(r > max){ max = r; maxi = i; }

      r = compare_voice( ref, voice, 0  ) * w;
      if(r > max){ max = r; maxi = i; }

      r = compare_voice( ref, voice, 1  ) * w; 
      if(r > max){ max = r; maxi = i; }

      r = compare_voice( ref, voice, 2  ) * w;
      if(r > max){ max = r; maxi = i; }

    }
  }
      
  // 認識が成功したとき
  if( max > thresh_recognize ){
    result(maxi, voice);
  }
  
  // 認識が失敗したとき
  else{
    result(-1, null);  
  }
}

// 学習用の音声
int learn_voice_no = -1;
double[] learn_voice = null;

public void start(){
_O89_in();
parent.IADB_keyboad.init();
}
public void result(int i, double[] v){
_O112_in(i,v);
}
public void type(int key){
parent.IADB_keyboad.type(key);
}
private void _O89_in(){
// 初期設定


// 通知用変数を取得
Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
//intent.setData(Uri.parse("http://www.google.com/"));

PendingIntent pendingIntent
	= PendingIntent.getActivity(
		SERVICE,
		0,
		intent,
		0);

notifManager = (NotificationManager)SERVICE.getSystemService(Context.NOTIFICATION_SERVICE);
notif = new Notification(R.drawable.ic_launcher, "音声キーボードが有効です", System.currentTimeMillis());

// 通知を選択した時に自動的に通知が消えるための設定
notif.flags = Notification.FLAG_AUTO_CANCEL;

// "Latest Event" レイアウトの設定
notif.setLatestEventInfo(
		SERVICE,
		"音声キーボード",
		"有効",
		pendingIntent);


// 音声認識用三角関数テーブルを作成
  sin_table = new double[HEARING_HEIGHT][SOUND_DFT_SIZE];
  cos_table = new double[HEARING_HEIGHT][SOUND_DFT_SIZE];

// 対数スケールの場合
if(flog_scale){
  int[] dist  = new int[HEARING_HEIGHT+1]; // 周波数分布テーブル
  double p = Math.pow( HEARING_HEIGHT-1.0, 1.0 / HEARING_HEIGHT );
  for( int i = 0; i <= HEARING_HEIGHT; i++ ){
    int d = (int)(Math.pow( p, (double)i ));
    dist[i] = i+1 >= d? i+1 : d;
  }
  for(int i = 0; i < HEARING_HEIGHT; i++){
    for(int j = 0; j < SOUND_DFT_SIZE; j++){
      sin_table[i][j] = 0;
      cos_table[i][j] = 0;
      double r = 1 / Math.sqrt(dist[i+1] - dist[i]);
      for(int k = dist[i]; k < dist[i+1]; k++){
        sin_table[i][j] += r * Math.sin(k * j * 6.2832 / SOUND_DFT_SIZE);
        cos_table[i][j] += r * Math.cos(k * j * 6.2832 / SOUND_DFT_SIZE);
      }
    }
  }
}

// リニアスケールの場合
else{
  for(int i = 0; i < HEARING_HEIGHT; i++){
    for(int j = 0; j < SOUND_DFT_SIZE; j++){
      sin_table[i][j] = Math.sin(i * j * 6.2832 / SOUND_DFT_SIZE);
      cos_table[i][j] = Math.cos(i * j * 6.2832 / SOUND_DFT_SIZE);
    }
  }
}


// 録音スレッドを開始
is_running = true;
record_thread = new RecordThread();
record_thread.start();


}
private void _O112_in(int i, double[] v){
int key;
// 音声キーボードが有効なら
// 通知してタイプする
// 自動学習が有効なら学習する



// 認識に失敗したときは学習データを破棄して終了
if(v == null){
  learn_voice_no = -1;
  learn_voice = null;
  return;
}


// 認識に成功したとき
else{
  VoiceTemplate vt = (VoiceTemplate)(voice_template.get(i));

  // 認識結果が"モード切替"のとき
  if(vt.text.equals("モード切替")){

    // 音声キーボードの有効/無効の切り替え
    is_active = !is_active;

    // 有効になったら通知ON
    if(is_active){
      notifManager.notify(R.string.app_name, notif);
    }

    // 無効になったら通知OFFしてデータを保存する
    else{
      notifManager.cancel(R.string.app_name);
      save();
    }
    
    // 学習データを破棄して終了
    learn_voice_no = -1;
    learn_voice = null;
    return;
  }

  // 音声認識が有効でない場合
  if(!is_active){

    // 学習データを破棄して終了
    learn_voice_no = -1;
    learn_voice = null;
    return;
  }

  // 音声認識が有効な場合
  else{

    // 自動学習が有効な場合
    if(auto_learn){
  
      // 認識結果が"違う"ときはペナルティ付きの学習をして学習データを破棄する
      if(vt.text.equals("違う")){
        if(learn_voice_no >= 0 && learn_voice != null){
          double[] w =((VoiceTemplate)(voice_template.get(learn_voice_no))).voice;
          for(int j = 0; j < w.length; j++){
            double d = 0;
            if(j < learn_voice.length) d = learn_voice[j];
            w[j] = ((learn_param_x - 1.0) * w[j] - d) / learn_param_x;
          }
        }
        learn_voice_no = -1;
        learn_voice = null;
      }

      // そうでない場合は報酬付きの学習をして次回の学習のためのデータを用意する
      else{
        if(learn_voice_no >= 0 && learn_voice != null){
          double[] w =((VoiceTemplate)(voice_template.get(learn_voice_no))).voice;
          for(int j = 0; j < w.length; j++){
            double d = 0;
            if(j < learn_voice.length) d = learn_voice[j];
            w[j] = ((learn_param_o - 1.0) * w[j] + d) / learn_param_o;
          }
        }
        learn_voice_no = i;
        learn_voice = new double[v.length];
        for(int j = 0; j < v.length;j++){
          learn_voice[j] = v[j];
        }
      }
    }

    // タイプするキーコードをセットする
    key = vt.code;
  }
}


type(key);
}
Control( VoiceKeyboardService pnt ){
 parent = pnt;

}
}
ADB_keyboad IADB_keyboad;
class ADB_keyboad{
VoiceKeyboardService parent;
byte recv[] = new byte[256];
byte close[] ={
(byte)0x43 ,(byte)0x4c ,(byte)0x53 ,(byte)0x45,
(byte)0x23 ,(byte)0x0e ,(byte)0x00 ,(byte)0x00,
(byte)0xaa ,(byte)0x00 ,(byte)0x00 ,(byte)0x00,
(byte)0x00 ,(byte)0x00 ,(byte)0x00 ,(byte)0x00,
(byte)0x00 ,(byte)0x00 ,(byte)0x00 ,(byte)0x00,
(byte)0xbc ,(byte)0xb3 ,(byte)0xac ,(byte)0xba
};

byte open[] = {
(byte)0x4f ,(byte)0x50 ,(byte)0x45 ,(byte)0x4e,
(byte)0x0b ,(byte)0x00 ,(byte)0x00 ,(byte)0x00,
(byte)0x00 ,(byte)0x00 ,(byte)0x00 ,(byte)0x00,
(byte)0x18 ,(byte)0x00 ,(byte)0x00 ,(byte)0x00,
(byte)0x98 ,(byte)0x08 ,(byte)0x00 ,(byte)0x00,
(byte)0xb0 ,(byte)0xaf ,(byte)0xba ,(byte)0xb1
};

byte auth[] = {
(byte)0x41 ,(byte)0x55 ,(byte)0x54 ,(byte)0x48
,(byte)0x02 ,(byte)0x00 ,(byte)0x00 ,(byte)0x00
,(byte)0x00 ,(byte)0x00 ,(byte)0x00 ,(byte)0x00
,(byte)0x00 ,(byte)0x01 ,(byte)0x00 ,(byte)0x00
,(byte)0xa5 ,(byte)0x81 ,(byte)0x00 ,(byte)0x00
,(byte)0xbe ,(byte)0xaa ,(byte)0xab ,(byte)0xb7
};

byte connect[] = {
(byte)0x43 ,(byte)0x4e ,(byte)0x58 ,(byte)0x4e
,(byte)0x01 ,(byte)0x00 ,(byte)0x00 ,(byte)0x01
,(byte)0x00 ,(byte)0x00 ,(byte)0x10 ,(byte)0x00
,(byte)0xc9 ,(byte)0x00 ,(byte)0x00 ,(byte)0x00
,(byte)0x24 ,(byte)0x4e ,(byte)0x00 ,(byte)0x00
,(byte)0xbc ,(byte)0xb1 ,(byte)0xa7 ,(byte)0xb1
,(byte)0x68 ,(byte)0x6f ,(byte)0x73 ,(byte)0x74
,(byte)0x3a ,(byte)0x3a ,(byte)0x66 ,(byte)0x65
,(byte)0x61 ,(byte)0x74 ,(byte)0x75 ,(byte)0x72
,(byte)0x65 ,(byte)0x73 ,(byte)0x3d ,(byte)0x73
,(byte)0x68 ,(byte)0x65 ,(byte)0x6c ,(byte)0x6c
,(byte)0x5f ,(byte)0x76 ,(byte)0x32 ,(byte)0x2c
,(byte)0x63 ,(byte)0x6d ,(byte)0x64 ,(byte)0x2c
,(byte)0x73 ,(byte)0x74 ,(byte)0x61 ,(byte)0x74
,(byte)0x5f ,(byte)0x76 ,(byte)0x32 ,(byte)0x2c
,(byte)0x6c ,(byte)0x73 ,(byte)0x5f ,(byte)0x76
,(byte)0x32 ,(byte)0x2c ,(byte)0x66 ,(byte)0x69
,(byte)0x78 ,(byte)0x65 ,(byte)0x64 ,(byte)0x5f
,(byte)0x70 ,(byte)0x75 ,(byte)0x73 ,(byte)0x68
,(byte)0x5f ,(byte)0x6d ,(byte)0x6b ,(byte)0x64
,(byte)0x69 ,(byte)0x72 ,(byte)0x2c ,(byte)0x61
,(byte)0x70 ,(byte)0x65 ,(byte)0x78 ,(byte)0x2c
,(byte)0x61 ,(byte)0x62 ,(byte)0x62 ,(byte)0x2c
,(byte)0x66 ,(byte)0x69 ,(byte)0x78 ,(byte)0x65
,(byte)0x64 ,(byte)0x5f ,(byte)0x70 ,(byte)0x75
,(byte)0x73 ,(byte)0x68 ,(byte)0x5f ,(byte)0x73
,(byte)0x79 ,(byte)0x6d ,(byte)0x6c ,(byte)0x69
,(byte)0x6e ,(byte)0x6b ,(byte)0x5f ,(byte)0x74
,(byte)0x69 ,(byte)0x6d ,(byte)0x65 ,(byte)0x73
,(byte)0x74 ,(byte)0x61 ,(byte)0x6d ,(byte)0x70
,(byte)0x2c ,(byte)0x61 ,(byte)0x62 ,(byte)0x62
,(byte)0x5f ,(byte)0x65 ,(byte)0x78 ,(byte)0x65
,(byte)0x63 ,(byte)0x2c ,(byte)0x72 ,(byte)0x65
,(byte)0x6d ,(byte)0x6f ,(byte)0x75 ,(byte)0x6e
,(byte)0x74 ,(byte)0x5f ,(byte)0x73 ,(byte)0x68
,(byte)0x65 ,(byte)0x6c ,(byte)0x6c ,(byte)0x2c
,(byte)0x74 ,(byte)0x72 ,(byte)0x61 ,(byte)0x63
,(byte)0x6b ,(byte)0x5f ,(byte)0x61 ,(byte)0x70
,(byte)0x70 ,(byte)0x2c ,(byte)0x73 ,(byte)0x65
,(byte)0x6e ,(byte)0x64 ,(byte)0x72 ,(byte)0x65
,(byte)0x63 ,(byte)0x76 ,(byte)0x5f ,(byte)0x76
,(byte)0x32 ,(byte)0x2c ,(byte)0x73 ,(byte)0x65
,(byte)0x6e ,(byte)0x64 ,(byte)0x72 ,(byte)0x65
,(byte)0x63 ,(byte)0x76 ,(byte)0x5f ,(byte)0x76
,(byte)0x32 ,(byte)0x5f ,(byte)0x62 ,(byte)0x72
,(byte)0x6f ,(byte)0x74 ,(byte)0x6c ,(byte)0x69
,(byte)0x2c ,(byte)0x73 ,(byte)0x65 ,(byte)0x6e
,(byte)0x64 ,(byte)0x72 ,(byte)0x65 ,(byte)0x63
,(byte)0x76 ,(byte)0x5f ,(byte)0x76 ,(byte)0x32
,(byte)0x5f ,(byte)0x6c ,(byte)0x7a ,(byte)0x34
,(byte)0x2c ,(byte)0x73 ,(byte)0x65 ,(byte)0x6e
,(byte)0x64 ,(byte)0x72 ,(byte)0x65 ,(byte)0x63
,(byte)0x76 ,(byte)0x5f ,(byte)0x76 ,(byte)0x32
,(byte)0x5f ,(byte)0x64 ,(byte)0x72 ,(byte)0x79
,(byte)0x5f ,(byte)0x72 ,(byte)0x75 ,(byte)0x6e
,(byte)0x5f ,(byte)0x73 ,(byte)0x65 ,(byte)0x6e
,(byte)0x64

};

// DigestInfoデータ
byte digestInfo[] = {

(byte)0x30,(byte)0x21,(byte)0x30,(byte)0x09,
(byte)0x06,(byte)0x05,(byte)0x2b,(byte)0x0e,
(byte)0x03,(byte)0x02,(byte)0x1a,(byte)0x05,
(byte)0x00,(byte)0x04,(byte)0x14
};

RSAPrivateKey privkey = null ;
// チェックサムを計算する
public int check_sum(byte[] payload) {

  int checksum = 0;
  for(byte b : payload) {
    checksum += (int)b & 0xff;
  }
  return checksum;
}

Cipher encipher = null;

public void type(int key){
_O35_in(key);
}
public void init(){
_O45_in();
}
private void _O35_in(int key){
 // キー入力コマンドを実行



Log.d("vkeyboard","recognise out key="+key);
   String cmd = "input keyevent "+key;
   try {
      int n,s;
      
      // ポートを開く
Log.d("vkeyboard","open localhost port 5555");
      Socket socket = new Socket("127.0.0.1", 5555);
      InputStream in  = socket.getInputStream();
      OutputStream out = socket.getOutputStream();
      socket.setTcpNoDelay(true); // 小さなパケットなのですぐ応答するようにしておく

      // ADB接続要求を送信
Log.d("vkeyboard","cnxn");
      out.write(connect);
      out.flush();

      // トークンを受信
Log.d("vkeyboard","recv token");
      in.read( recv, 0,  24);
      n = ((int)recv[12]&0xff)+((int)recv[13]&0xff)*0x100;
Log.d("vkeyboard","size="+n);
      byte[] token = new byte[n];
      in.read( token );

      // トークンから署名を生成
Log.d("vkeyboard","create private key");
      encipher.init(Cipher.ENCRYPT_MODE, privkey);
      encipher.update(digestInfo);
      byte[] enc = encipher.doFinal(token);
      n = enc.length;
      s = check_sum(enc);

       // 署名を送信
Log.d("vkeyboard","send auth");
      auth[12]= (byte)(n & 0xff);
      auth[13]= (byte)((n>>8) & 0xff);
      auth[14]= (byte)((n>>16) & 0xff);
      auth[15]= (byte)((n>>24) & 0xff);
      auth[16]= (byte)(s & 0xff);
      auth[17]= (byte)((s>>8) & 0xff);
      auth[18]= (byte)((s>>16) & 0xff);
      auth[19]= (byte)((s>>24) & 0xff);
      out.write(auth);
      out.write(enc);
      out.flush();
  
      // バナーを読み込む
Log.d("vkeyboard","read banner");
      in.read( recv, 0, 24 );
      n = ((int)recv[12]&0xff)+((int)recv[13]&0xff)*0x100;
      in.read( recv, 24, n );

      // キー入力コマンドを作成
Log.d("vkeyboard","create keycommand");
      String cm = "shell:"+cmd;
      n = cm.length();
      byte[] command = new byte[n+1];
      for(int i = 0; i < n; i++) { command[i] = (byte)cm.charAt(i); }
      command[n] = (byte)'\0';
      n = command.length;
      s = check_sum(command);
      open[12]= (byte)(n & 0xff);
      open[13]= (byte)((n>>8) & 0xff);
      open[14]= (byte)((n>>16) & 0xff);
      open[15]= (byte)((n>>24) & 0xff);
      open[16]= (byte)(s & 0xff);
      open[17]= (byte)((s>>8) & 0xff);
      open[18]= (byte)((s>>16) & 0xff);
      open[19]= (byte)((s>>24) & 0xff);
      
      // キー入力コマンドを送信
Log.d("vkeyboard","send keycommand");
      out.write(open);
      out.write(command);
      out.flush();

      // クローズコマンドを送信
Log.d("vkeyboard","read close command");
      in.read( recv, 0, 24 );
Log.d("vkeyboard","send close command");
      in.read( recv, 24, 24 );
      close[4]  = recv[8];
      close[5]  = recv[9];
      close[6]  = recv[10];
      close[7]  = recv[11];
      close[8]  = recv[4];
      close[9]  = recv[5];
      close[10] = recv[6];
      close[11] = recv[7];
      out.write(close);
      out.flush();

      // ポートを閉じる
Log.d("vkeyboard","close port");
      socket.close();
    } catch(Exception e){
      is_running = false;
      e.printStackTrace();
    }

}
private void _O45_in(){
// 初期化 


      // 秘密鍵の生成
      byte[] privateKeyAsByteArray = null;
      try {
        int i = 0, cnt = 0;
		android.content.res.Resources resource = SERVICE.getResources();
        java.io.InputStream is = resource.openRawResource(R.raw.adbkey);
        while (is.read()!=-1) cnt++;
        privateKeyAsByteArray = new byte[cnt];
        is.reset();
        is.read(privateKeyAsByteArray);
        is.close();
        encipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyAsByteArray);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        privkey = (RSAPrivateKey) kf.generatePrivate(keySpec);
      }catch(Exception e){}


}
ADB_keyboad( VoiceKeyboardService pnt ){
 parent = pnt;

}
}
File_IO IFile_IO;
class File_IO{
VoiceKeyboardService parent;
public void save(){
_O14_in();
}
public void load(){
_O13_in();
parent.IControl.start();
}
private void _O13_in(){
//音声テンプレートを読み込む(無い場合は作成する)


try{
  dbg = new FileWriter(new File(Environment.getExternalStorageDirectory(), DEBUG_FILE));
} catch(Exception e){}


try{

if(!voice_data_file.exists()){
  int cnt = 0;
  InputStream is = SERVICE.getResources().openRawResource(R.raw.voicedata);
  while (is.read()!=-1) cnt++;
  byte[] b = new byte[cnt];
  is.reset();
  is.read(b);
  is.close();
  FileOutputStream os = new FileOutputStream(voice_data_file);
  os.write(b);
  os.close();
}

String line = null;
BufferedReader din = new BufferedReader( new FileReader(voice_data_file));

// 変数を読み込む
while(true){
  if( ( line=din.readLine() ) == null ) return;
  if( line.equals( "" ) ) break;
  if( line.equals("debug_mode=true"))         debug_mode=true;
  if( line.equals("debug_mode=false"))        debug_mode=false;
  if( line.equals("flog_scale=true"))         flog_scale=true;
  if( line.equals("flog_scale=false"))        flog_scale=false;
  if( line.equals("alog_scale=true"))         alog_scale=true;
  if( line.equals("alog_scale=false"))        alog_scale=false;
  if( line.equals("auto_learn=true"))         auto_learn=true;
  if( line.equals("auto_learn=false"))        auto_learn=false;
  if( line.startsWith("startup_time="))       startup_time=Integer.parseInt(line.substring(13));
  if( line.startsWith("sound_filter="))       sound_filter=Double.parseDouble(line.substring(13));
  if( line.startsWith("thresh_trigger_on="))  thresh_trigger_on=Double.parseDouble(line.substring(18));
  if( line.startsWith("thresh_trigger_off=")) thresh_trigger_off=Double.parseDouble(line.substring(19));
  if( line.startsWith("thresh_count_on="))    thresh_count_on=Integer.parseInt(line.substring(16));
  if( line.startsWith("thresh_count_off="))   thresh_count_off=Integer.parseInt(line.substring(17));
  if( line.startsWith("thresh_recognize="))   thresh_recognize=Double.parseDouble(line.substring(17));
  if( line.startsWith("bias="))               bias=Double.parseDouble(line.substring(5));
  if( line.startsWith("acompress="))          acompress=Double.parseDouble(line.substring(10));
  if( line.startsWith("learn_param_o="))      learn_param_o=Double.parseDouble(line.substring(14));
  if( line.startsWith("learn_param_x="))      learn_param_x=Double.parseDouble(line.substring(14));
  if( line.startsWith("limit_length="))       limit_length=Double.parseDouble(line.substring(13));
}

hosei = new double[HEARING_HEIGHT];
for(int i = 0; true; i++){
  if((line=din.readLine()) == null){din.close(); return;}
  if( line.equals( "" ) ) break;
  hosei[i%HEARING_HEIGHT] = Double.parseDouble(line);
}

// 音声データを読み込む
voice_template = new Vector();
while(true){
  if((line=din.readLine()) == null){din.close(); return;}
  if(line.equals("END")) break;
  String t = line;
  if( ( line=din.readLine() ) == null ){din.close(); return;}
  double w = Double.parseDouble(line);
  if( ( line=din.readLine() ) == null ){din.close(); return;}
  int c = Integer.parseInt(line);
  if( ( line=din.readLine() ) == null ){din.close(); return;}
  int s = Integer.parseInt(line);
  double[] v = new double[s];
  for(int i = 0; i < s; i++){
    if((line=din.readLine()) == null){din.close(); return;}
    v[i] = Double.parseDouble(line);
  }
  voice_template.add(new VoiceTemplate(t, w, c, v));
}

din.close();
} catch( Exception e ){ dprint(e+"\n"); }

voice_no = 0;

}
private void _O14_in(){
//音声テンプレートを保存する

try{
  BufferedWriter dout = new BufferedWriter( new FileWriter(voice_data_file) );
  
  // 変数を保存する
  dout.write("flog_scale=" + flog_scale + "\n");
  dout.write("alog_scale=" + alog_scale + "\n");
  dout.write("auto_learn=" + auto_learn + "\n");
  dout.write("startup_time=" + startup_time + "\n");
  dout.write("sound_filter=" + sound_filter + "\n");
  dout.write("thresh_trigger_on=" + thresh_trigger_on + "\n");
  dout.write("thresh_trigger_off=" + thresh_trigger_off + "\n");
  dout.write("thresh_count_on=" + thresh_count_on + "\n");
  dout.write("thresh_count_off=" + thresh_count_off + "\n");
  dout.write("thresh_recognize=" + thresh_recognize + "\n");
  dout.write("bias=" + bias + "\n");
  dout.write("acompress=" + acompress + "\n");
  dout.write("learn_param_o=" + learn_param_o + "\n");
  dout.write("learn_param_x=" + learn_param_x + "\n");
  dout.write("limit_length=" + limit_length + "\n");
  dout.write("\n");

  for(int i = 0; i < HEARING_HEIGHT; i++){
    dout.write(hosei[i] + "\n");
  }
  dout.write("\n");
  
  // 音声データを保存する
  for( int i = 0; i < voice_template.size(); i++ ){
    dout.write(((VoiceTemplate)voice_template.get(i)).text + "\n" );
    dout.write(((VoiceTemplate)voice_template.get(i)).weight + "\n" );
    dout.write(((VoiceTemplate)voice_template.get(i)).code + "\n" );
    double[] v = ((VoiceTemplate)(voice_template.get(i))).voice;
    dout.write(v.length + "\n");
    for (int j = 0; j <  v.length; j++){
      dout.write(v[j] + "\n");
    }
  }
  dout.write("END\n");
  dout.close();
} catch( Exception e ){dprint(e+"\n");}

}
File_IO( VoiceKeyboardService pnt ){
 parent = pnt;

}
}
VoiceKeyboardService( ){
IControl = new Control( this );
IADB_keyboad = new ADB_keyboad( this );
IFile_IO = new File_IO( this );

}
}
