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

public class VKeyBoardService extends Service {
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

// 変数
Handler handler = new Handler();
double[] voice;      // 録音した音声
double[] hosei;      // 聴覚感度補正係数
double[] filter;      // 平滑フィルタ

// 定数
static final int SAMPLING_RATE = 16000;
static final int HEARING_WIDTH = 40;
static final int HEARING_HEIGHT = 128;
static final int SOUND_DFT_SIZE = 256;

// デバッグファイル
String DEBUG_FILE = "/sdcard/debug.txt";

// 与えられたコマンドを実行する
public void exec( String s ){

dprint("execute: "+s+"\n");

  Process p=null;
  try{
    p = java.lang.Runtime.getRuntime().exec(s);
  } catch( Exception ie ){ dprint("can not execute: "+s+"\n"); }
}

// デバッグ表示
FileWriter dbg;
boolean debug_mode = true;
public void dprint(String s){
  if(debug_mode){
    try{
      dbg.write(s);
      dbg.flush();
    } catch(Exception e){}
  }
}

// 各種パラメータ
boolean is_running = false;
int bright = 0;  // 声紋表示の輝度調節
int startup_time = 300;
double sound_filter = 150.0;
double thresh_trigger_on = 4;
double thresh_trigger_off = 4;
int thresh_count_on = 4;
int thresh_count_off = 10;
double thresh_recognize = 0.1;
double rcompress = 0.3; // レンジ圧縮用
int filter_size = 1; // 平滑フィルタのサイズ
int vstart = 1; // 音声の先頭
int vend   = 1; // 音声の末尾
int voice_no = 0; // 音声番号
double kpenalty = 1.0; //音声の長さ比較用

// 音声テンプレート
Vector voice_template;
class VoiceTemplate{
  String text;    // 表示テキスト
  int code;        // キーコード
  int length;      // 音声の長さ
  double weight; // 重み係数(音声認識の優先度)
  double[] voice; // 音声データ
  VoiceTemplate( String t, int c, int l, double w, double[] v ){
    text = t;
    code = c;
    length = l;
    weight = w;
    voice = v;  
  }
}


// 端末に送るコマンド文字列(OSや端末によって変わるかも)
String INIT_COMMAND = "adb connect localhost";
String KEY_COMMAND = "adb -s localhost:5555 shell input keyevent ";

// 音声テンプレートファイル
File voice_data_file = new File( "/sdcard/VoiceData.txt" );

// 音声キーボードサービスプログラム for android

public void Start(){
IFile_IO.load();
}
private void _O25_in(int key){
// キーコードを発行する
exec( KEY_COMMAND + key );

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
//音声テンプレートを読み込む


try{
  dbg = new FileWriter(DEBUG_FILE);
} catch(Exception e){}


try{
String line = null;
BufferedReader din = new BufferedReader( new FileReader(voice_data_file ) );

// 変数を読み込む
while(true){
  if( ( line=din.readLine() ) == null ) return;
  if( line.equals( "" ) ) break;
  if( line.startsWith("startup_time="))        startup_time=Integer.parseInt(line.substring(13));
  if( line.startsWith("sound_filter="))          sound_filter=Double.parseDouble(line.substring(13));
  if( line.startsWith("thresh_trigger_on=")) thresh_trigger_on=Double.parseDouble(line.substring(18));
  if( line.startsWith("thresh_trigger_off=")) thresh_trigger_off=Double.parseDouble(line.substring(19));
  if( line.startsWith("thresh_count_on="))   thresh_count_on=Integer.parseInt(line.substring(16));
  if( line.startsWith("thresh_count_off="))   thresh_count_off=Integer.parseInt(line.substring(17));
  if( line.startsWith("thresh_recognize="))  thresh_recognize=Double.parseDouble(line.substring(17));
  if( line.startsWith("rcompress="))              rcompress=Double.parseDouble(line.substring(10));
  if( line.startsWith("filter_size="))               filter_size=Integer.parseInt(line.substring(12));
  if( line.startsWith("kpenalty="))                 kpenalty=Double.parseDouble(line.substring(9));
}

// フィルタ係数を読み込む
int size = filter_size *2 + 1, i = 0;
filter = new double[ size * size ];
while(true){
  if( ( line=din.readLine() ) == null ) return;
  if( line.equals( "" ) ) break;
  filter[ i ++] = Double.parseDouble(line);
}

// 聴覚感度補正データを読み込む
hosei = new double[HEARING_HEIGHT];
i = 0;
while(true){
  if( ( line=din.readLine() ) == null ) return;
  if( line.equals( "" ) ) break;
  hosei[ i ++] = Double.parseDouble(line);
}

// 音声データを読み込む
voice_template = new Vector();
while(true){
  if( ( line=din.readLine() ) == null ) return;
  if( line.equals( "END" ) ) break;
  String t = line;
  if( ( line=din.readLine() ) == null ) return;
  int c = Integer.parseInt(line);
  if( ( line=din.readLine() ) == null ) return;
  int l = Integer.parseInt(line);
  if( ( line=din.readLine() ) == null ) return;
  double w = Double.parseDouble(line);
  if( ( line=din.readLine() ) == null ) return;
  int s = Integer.parseInt(line);
  double[] v = new double[ s ];
  for( i = 0; i < s; i++ ){
    if( ( line=din.readLine() ) == null ) return;
    v[i] = Double.parseDouble(line);
  }
  voice_template.add( new VoiceTemplate( t, c, l, w, v ) );
}

din.close();
} catch( Exception e ){}

voice_no = 0;

}
private void _O14_in(){
//音声テンプレートを保存する

try{
  BufferedWriter dout = new BufferedWriter( new FileWriter( voice_data_file ) );
  
  // 変数を保存する
  dout.write("startup_time="+ startup_time + "\n");
  dout.write("sound_filter="+ sound_filter + "\n");
  dout.write("thresh_trigger_on="+ thresh_trigger_on + "\n");
  dout.write("thresh_trigger_off="+ thresh_trigger_off + "\n");
  dout.write("thresh_count_on="+ thresh_count_on + "\n");
  dout.write("thresh_count_off="+ thresh_count_off + "\n");
  dout.write("thresh_recognize="+ thresh_recognize + "\n");
  dout.write("rcompress="+ rcompress + "\n");
  dout.write("filter_size="+ filter_size + "\n");
  dout.write("kpenalty="+ kpenalty + "\n");
  dout.write("\n");
  
  // フィルタ係数を保存する
  int size = filter_size * 2 + 1;
  for( int i = 0; i < size * size; i++ ) dout.write( filter[ i ] + "\n" );
  dout.write("\n");
  
  // 聴覚感度補正データを保存する
  for( int i = 0; i < HEARING_HEIGHT; i++ ) dout.write( hosei[ i ] + "\n" );
  dout.write("\n");
  
  // 音声データを保存する
  for( int i = 0; i < voice_template.size(); i++ ){
    dout.write( ((VoiceTemplate)voice_template.get(i)).text + "\n" );
    dout.write( ((VoiceTemplate)voice_template.get(i)).code + "\n" );
    dout.write( ((VoiceTemplate)voice_template.get(i)).length + "\n" );
    dout.write( ((VoiceTemplate)voice_template.get(i)).weight + "\n" );
    double[] v = ((VoiceTemplate)(voice_template.get(i))).voice;
    dout.write(v.length + "\n");
    for ( int j = 0; j <  v.length; j++ ){
      dout.write( v[j] + "\n");
    }
  }
  dout.write("END\n");

  // キーボードデータを保存する

  dout.close();
} catch( Exception e ){}

}
File_IO( VoiceKeyboardService pnt ){
 parent = pnt;

}
}
Control IControl;
class Control{
VoiceKeyboardService parent;
// 音声の認識スレッド
RecognizeThread recognize_thread = null;
boolean is_action; // セットすると認識処理を始める
class RecognizeThread extends Thread {

  public void run() {
    is_action = false;
    while(is_running){
    
      // 処理の依頼を待つ
      while( !is_action ){
        try{
          Thread.sleep(10);
        } catch( Exception e ){}
      } 

      // 音声認識処理
      int maxi = 0;
      double r = 0.0, max = 0.0;
      for( int i = 0; i < voice_template.size(); i++ ){

        // テンプレートの音声
        double[] ref = ((VoiceTemplate)(voice_template.get(i))).voice;

         // 重み
        double w = ((VoiceTemplate)(voice_template.get(i))).weight;
        
        // 録音した音声とテンプレートの音声の長さを比較する(一致しないとペナルティとなる)
        double d = (double)voice.length / (double)((VoiceTemplate)(voice_template.get(i))).length - 1.0;
        double p = Math.exp( -kpenalty * d * d );

        // テンプレートと録音した音声の相関値を計算して重みとペナルティをかける
        r = compare_voice( ref, voice ) * w * p;
        if( r > max ){ max = r; maxi = i; }
      }
      
      // 認識結果を送る
      if( max > thresh_recognize ){
        int key = ((VoiceTemplate)(voice_template.get(maxi))).code;
         result( key );
      }
      else result(0);

      // 処理が終了したのでフラグをクリア
      is_action = false;
    }

  }//run()

}//HandlerThread

// 2つの音声を比較して相関値を返す
public double compare_voice( double[] ref, double[] voice ){
  int x, y, width, height;
  double a = 0.0, nn = 0.0, ar = 0.0, av = 0.0, pr = 0.0, pv = 0.0;

  if( voice == null || ref == null ) return 0.0;
  if( voice.length <= 0 || ref.length <=0 ) return 0.0;
    
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

// 録音スレッド
RecordThread record_thread = null;
class RecordThread extends Thread {
  int state;
  static final int NEUTRAL = 0;
  static final int OFF = 1;
  static final int SENS_ON = 2;
  static final int ON = 3;
  static final int SENS_OFF = 4;
  int HEARING_BUFFER_SIZE = SOUND_DFT_SIZE * HEARING_WIDTH * 4;

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
    short[]    sound_buffer   = new short[ SOUND_DFT_SIZE ];
    double[] sound_av         = new double[ HEARING_HEIGHT ];
    double[] hearing_buffer = new double[ HEARING_BUFFER_SIZE ];

    while(is_running){       

      // 録音データを読み込む(SOUND_DFT_SIZEは配列要素の数)
      audioRec.read( sound_buffer, 0, SOUND_DFT_SIZE );

      // 取り込んだ聴覚データをDFTして開けておいたところにセットする
      trigger = 0.0;
      for( i = 0; i < HEARING_HEIGHT; i++ ){
        for( x = y = 0.0, j = 0; j < SOUND_DFT_SIZE; j++ ){
          a = (double)sound_buffer[ j ];
          x += a * cos_table[ i ][ j ];
          y += a * sin_table[ i ][ j ];
        }
        hearing_buffer[ i + offset ] = pow = Math.pow( (x * x + y * y) * hosei[ i ], rcompress );
        sound_av[ i ] = ( (sound_filter - 1.0) * sound_av[ i ]   + pow ) / sound_filter;
        pow /= sound_av[ i ];
        if( pow > trigger ) trigger = pow;
      }

      // 状態ごとの動作を以下に記述
      switch( state ){
          
      // NEWTRAL状態：起動してしばらくは各フィルタ変数が安定するのを待つ
      case NEUTRAL:
        if( count > startup_time ){
           state = OFF;
        }
        break;

      // OFF状態
      case OFF:
        if( trigger > thresh_trigger_on ){
          state = SENS_ON;
          start_point = offset - HEARING_HEIGHT;  // スレッショルドを越える直前をサンプリング開始位置とする
          count = 0;
        }
        break;

      // OFFからONに遷移する状態
      case SENS_ON:
        if( trigger < thresh_trigger_off )  state = OFF;
        else{
          if( count > thresh_count_on ) state = ON;
        }
        break;

      // ON状態
      case ON:
        if( trigger < thresh_trigger_off ){
          state = SENS_OFF;
          end_point = offset + HEARING_HEIGHT; // スレッショルドを下回った所をサンプリング終了位置とする
          count = 0;
        }
        break;

      // ONからOFFに遷移する状態
      case SENS_OFF:
        if( trigger > thresh_trigger_on )  state = ON;
        else{
          if( count > thresh_count_off ){

            // サンプリング範囲内のデータを音声データ配列voiceに転送する
            len = end_point - start_point;
            if( len < 0 ) len += HEARING_BUFFER_SIZE;

            double[] xvoice = new double[ len ];
            double sum = 0;
            for( i = start_point, j = 0; j < len; i++, j++ ){

              // リングバッファ処理
              if( i < 0 )  i += HEARING_BUFFER_SIZE;
              if( i >= HEARING_BUFFER_SIZE ) i -=  HEARING_BUFFER_SIZE;

              sum += xvoice[ j ] = hearing_buffer[ i ];
            }
            voice = do_filter( xvoice ); // 平滑フィルタ処理
            state = OFF;
            is_action = true;  // 音節認識スレッドに処理を渡す
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

// 三角関数テーブル
double[][] sin_table, cos_table;

// 音声の平滑フィルタ処理
public double[] do_filter( double[] in ){
  int x, y, x0, y0, i, j, width, filter_height;
  double  a, out[];

  if( in == null ) return null;

  out = new double[ in.length ];
  filter_height = 2 * filter_size + 1;
  width = in.length / HEARING_HEIGHT;
  for( x =0; x < width; x++ ){
    for( y =0; y < HEARING_HEIGHT; y++ ){
      a = 0.0;
      for( i = - filter_size; i <= filter_size; i++ ){
        x0 = x + i;
        for( j = - filter_size; j <= filter_size; j++ ){
          y0 = y + j;
          if( x0 >= 0 && x0 < width && y0 >=0 && y0 < HEARING_HEIGHT ){
            a += in[ HEARING_HEIGHT * x0 + y0 ] * filter[ filter_height *( i + filter_size ) + ( j + filter_size ) ];
          }
        }
      }
      out[ HEARING_HEIGHT * x + y ] = a;
    }
  }
  return out;
}

public void start(){
_O89_in();
}
public void result(int key){
parent._O25_in(key);
}
private void _O89_in(){
// 初期設定



// 三角関数テーブルを作成
sin_table = new double[HEARING_HEIGHT][SOUND_DFT_SIZE];
cos_table = new double[HEARING_HEIGHT][SOUND_DFT_SIZE];
for(int i = 0; i < HEARING_HEIGHT; i++ ){
  for( int j = 0; j < SOUND_DFT_SIZE; j++ ){
    sin_table[ i ][ j ]  = Math.sin((double)(i  * j) * 6.2832 / (double)(SOUND_DFT_SIZE));
    cos_table[ i ][ j ] = Math.cos((double)(i  * j) * 6.2832 / (double)(SOUND_DFT_SIZE));
  }
}

// 初期化(端末にADB接続する)
exec( INIT_COMMAND );

// 各スレッドを生成
record_thread = new RecordThread();
recognize_thread = new RecognizeThread();

// 各スレッドをスタート
is_running = true;
record_thread.start();
recognize_thread.start();


}
Control( VoiceKeyboardService pnt ){
 parent = pnt;

}
}
VoiceKeyboardService( ){
IFile_IO = new File_IO( this );
IControl = new Control( this );

}
}
