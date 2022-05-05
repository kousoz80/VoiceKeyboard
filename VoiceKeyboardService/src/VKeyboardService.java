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
String DEBUG_FILE = "debug.txt";

// デバッグ表示
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


// 音声テンプレートファイル
File voice_data_file = new File( Environment.getExternalStorageDirectory(),"VoiceData.txt" );

// 音声キーボードサービスプログラム for android  ver 0.2.1
// 変更点：
// ADB接続でキー入力コマンドを実行するのでroot権限が必要でなくなった

public void Start(){
IFile_IO.load();
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
  dbg = new FileWriter(new File(Environment.getExternalStorageDirectory(), DEBUG_FILE));
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
} catch( Exception e ){e.printStackTrace();}

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

    // 開始音を鳴らす
    MediaPlayer.create(SERVICE,R.raw.start).start();

Log.d("vkeyboard","start rec thread\n");

    // 例外が出たら即終了
    try{

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

      // 録音開始
      audioRec.startRecording();
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

    } catch(Exception e){
     is_running = false;
     e.printStackTrace();
    }

    // 終了音を鳴らす
    MediaPlayer.create(SERVICE,R.raw.stop).stop();

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
parent.IADB_keyboad.init();
}
public void result(int key){
parent.IADB_keyboad.type(key);
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
VoiceKeyboardService( ){
IFile_IO = new File_IO( this );
IControl = new Control( this );
IADB_keyboad = new ADB_keyboad( this );

}
}
