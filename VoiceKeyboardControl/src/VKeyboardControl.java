package com.example.voice_keyboard_control;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.widget.Button;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.EditText;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.Editable;
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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.app.Activity;
import android.graphics.Color;

import android.widget.AbsoluteLayout;
import android.widget.*;
import android.view.Gravity;
import android.view.View.OnClickListener;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioTrack;
import android.media.AudioManager;

public class VKeyboardControl extends Activity {
  VoiceKeyboardControl ap;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    ap = new VoiceKeyboardControl();
    ap.ACTIVITY = this;
    ap.Start();
   }

   @Override
    public void onPause(){
      if( ap != null ){
         ap.is_running=false;
         try{
           ap.dbg.close();
         } catch(Exception e){}
      }
      super.onStop();
    }

}

class VoiceKeyboardControl{
Activity ACTIVITY;

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

// 音声テンプレートファイル
File voice_data_file = new File( "/sdcard/VoiceData.txt" );

// 定数
static final int SAMPLING_RATE = 16000;
static final int HEARING_WIDTH = 40;
static final int HEARING_HEIGHT = 128;
static final int SOUND_DFT_SIZE = 256;

// 変数
Handler handler = new Handler();
TextView seimon;   // 録音した音声の声紋表示
TextView seimon0; // テンプレートの声紋表示
TextView text;      // テキスト表示
EditText  code;     // キーコード
EditText  weight;   // 重み係数
TextView result;    // 認識結果の表示
double[] voice;      // 録音した音声
double[] hosei;      // 聴覚感度補正係数
double[] filter;      // 平滑フィルタ

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

// 表示を更新する
public void update_display(){

dprint("update display\n");

  handler.post(new Runnable() {
    @Override
    public void run() {
      seimon0.invalidate();
      seimon.invalidate();
      text.setText( ((VoiceTemplate)(voice_template.get(voice_no))).text );
      code.setText( ""+((VoiceTemplate)(voice_template.get(voice_no))).code );
      weight.setText( ""+((VoiceTemplate)(voice_template.get(voice_no))).weight );
    }
  });
}

// デバッグファイル
String DEBUG_FILE="/sdcard/debug.txt";

public void Start(){
IGUI.Start();
}
private void _O117_in(String s){
// 結果を表示する


handler.post(new Runnable() {
  @Override
  public void run() {
    result.setText(s);
  }
});


update_display();

}
GUI IGUI;
class GUI{
int STATE, STATE2;
VoiceKeyboardControl parent;
 class XGUI{
save Isave;
 class save extends Button{
 save(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 0, 0, 0 ));
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setText( "SAVE" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {save_clicked();}} );
}
}
store Istore;
 class store extends Button{
 store(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 255, 0, 0 ));
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setText( "STORE" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {store_clicked();}} );
}
}
ref_count Iref_count;
 class ref_count extends SeekBar{
 ref_count(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setProgress( 0 );
 setMax( 100 );
 setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
 public void onStopTrackingTouch(SeekBar seekBar) {}
 public void onStartTrackingTouch(SeekBar seekBar) {}
 public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { ref_count_changed( progress ); }
 });
}
}
prev Iprev;
 class prev extends Button{
 prev(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 0, 0, 0 ));
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setText( "<" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {prev_clicked();}} );
}
}
next Inext;
 class next extends Button{
 next(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 0, 0, 0 ));
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setText( ">" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {next_clicked();}} );
}
}
seimon0 Iseimon0;
 class seimon0 extends TextView{
 seimon0(){
 super(ACTIVITY);
 setGravity(Gravity.RIGHT|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 0, 0, 0 ));
 setText( "" );
 seimon0_created( this );
}
 protected void onDraw(Canvas g){ super.onDraw( g ); seimon0_paint(g); }
}
seimon Iseimon;
 class seimon extends TextView{
 seimon(){
 super(ACTIVITY);
 setGravity(Gravity.RIGHT|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 0, 0, 0 ));
 setText( "" );
 seimon_created( this );
}
 protected void onDraw(Canvas g){ super.onDraw( g ); seimon_paint(g); }
}
text Itext;
 class text extends TextView{
 text(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 24f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 232, 232, 232 ));
 setText( "" );
 text_created( this );
}
}
result Iresult;
 class result extends TextView{
 result(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 24f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 204 ));
 setText( "" );
 result_created( this );
}
}
xbright Ixbright;
 class xbright extends SeekBar{
 xbright(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setProgress( 0 );
 setMax( 100 );
 setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
 public void onStopTrackingTouch(SeekBar seekBar) {}
 public void onStartTrackingTouch(SeekBar seekBar) {}
 public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { xbright_changed( progress ); }
 });
}
}
LABEL10 ILABEL10;
 class LABEL10 extends TextView{
 LABEL10(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 14f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "No." );
}
}
LABEL11 ILABEL11;
 class LABEL11 extends TextView{
 LABEL11(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 14f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "Bright" );
}
}
LABEL12 ILABEL12;
 class LABEL12 extends TextView{
 LABEL12(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 14f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 252, 252, 252 ));
 setText( "V.Start" );
}
}
xvstart Ixvstart;
 class xvstart extends SeekBar{
 xvstart(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setProgress( 0 );
 setMax( 100 );
 setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
 public void onStopTrackingTouch(SeekBar seekBar) {}
 public void onStartTrackingTouch(SeekBar seekBar) {}
 public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { xvstart_changed( progress ); }
 });
}
}
xvend Ixvend;
 class xvend extends SeekBar{
 xvend(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setProgress( 0 );
 setMax( 100 );
 setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
 public void onStopTrackingTouch(SeekBar seekBar) {}
 public void onStartTrackingTouch(SeekBar seekBar) {}
 public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { xvend_changed( progress ); }
 });
}
}
LABEL15 ILABEL15;
 class LABEL15 extends TextView{
 LABEL15(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 252, 252, 252 ));
 setText( "V.End" );
}
}
LABEL16 ILABEL16;
 class LABEL16 extends TextView{
 LABEL16(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 252, 252, 252 ));
 setText( "WEIGHT" );
}
}
weight Iweight;
 class weight extends EditText{
 weight(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 248, 248, 248 ));
 setText( "1.0" );
 weight_created( this );
}
}
LABEL18 ILABEL18;
 class LABEL18 extends TextView{
 LABEL18(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 252, 252, 252 ));
 setText( "CODE" );
}
}
code Icode;
 class code extends EditText{
 code(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "0" );
 code_created( this );
}
}
 XGUI(){
 AbsoluteLayout layout=new AbsoluteLayout(ACTIVITY);
layout.setBackgroundColor(Color.rgb( 255, 255, 255));
ACTIVITY.setContentView(layout);
ACTIVITY.setTitle("音声キーボード");
 Isave = new save();
 Isave.setLayoutParams( new AbsoluteLayout.LayoutParams( 156,30,162,4 ) );
 layout.addView( Isave );
 Istore = new store();
 Istore.setLayoutParams( new AbsoluteLayout.LayoutParams( 157,31,0,4 ) );
 layout.addView( Istore );
 Iref_count = new ref_count();
 Iref_count.setLayoutParams( new AbsoluteLayout.LayoutParams( 256,35,66,184 ) );
 layout.addView( Iref_count );
 Iprev = new prev();
 Iprev.setLayoutParams( new AbsoluteLayout.LayoutParams( 37,140,1,38 ) );
 layout.addView( Iprev );
 Inext = new next();
 Inext.setLayoutParams( new AbsoluteLayout.LayoutParams( 39,143,283,36 ) );
 layout.addView( Inext );
 Iseimon0 = new seimon0();
 Iseimon0.setLayoutParams( new AbsoluteLayout.LayoutParams( 119,140,40,38 ) );
 layout.addView( Iseimon0 );
 Iseimon = new seimon();
 Iseimon.setLayoutParams( new AbsoluteLayout.LayoutParams( 118,141,163,38 ) );
 layout.addView( Iseimon );
 Itext = new text();
 Itext.setLayoutParams( new AbsoluteLayout.LayoutParams( 322,35,0,407 ) );
 layout.addView( Itext );
 Iresult = new result();
 Iresult.setLayoutParams( new AbsoluteLayout.LayoutParams( 323,35,0,448 ) );
 layout.addView( Iresult );
 Ixbright = new xbright();
 Ixbright.setLayoutParams( new AbsoluteLayout.LayoutParams( 256,34,65,224 ) );
 layout.addView( Ixbright );
 ILABEL10 = new LABEL10();
 ILABEL10.setLayoutParams( new AbsoluteLayout.LayoutParams( 58,33,4,185 ) );
 layout.addView( ILABEL10 );
 ILABEL11 = new LABEL11();
 ILABEL11.setLayoutParams( new AbsoluteLayout.LayoutParams( 59,34,2,221 ) );
 layout.addView( ILABEL11 );
 ILABEL12 = new LABEL12();
 ILABEL12.setLayoutParams( new AbsoluteLayout.LayoutParams( 57,34,3,262 ) );
 layout.addView( ILABEL12 );
 Ixvstart = new xvstart();
 Ixvstart.setLayoutParams( new AbsoluteLayout.LayoutParams( 256,32,66,261 ) );
 layout.addView( Ixvstart );
 Ixvend = new xvend();
 Ixvend.setLayoutParams( new AbsoluteLayout.LayoutParams( 259,31,62,298 ) );
 layout.addView( Ixvend );
 ILABEL15 = new LABEL15();
 ILABEL15.setLayoutParams( new AbsoluteLayout.LayoutParams( 65,31,0,293 ) );
 layout.addView( ILABEL15 );
 ILABEL16 = new LABEL16();
 ILABEL16.setLayoutParams( new AbsoluteLayout.LayoutParams( 73,32,0,369 ) );
 layout.addView( ILABEL16 );
 Iweight = new weight();
 Iweight.setLayoutParams( new AbsoluteLayout.LayoutParams( 242,32,76,370 ) );
 layout.addView( Iweight );
 ILABEL18 = new LABEL18();
 ILABEL18.setLayoutParams( new AbsoluteLayout.LayoutParams( 73,34,0,333 ) );
 layout.addView( ILABEL18 );
 Icode = new code();
 Icode.setLayoutParams( new AbsoluteLayout.LayoutParams( 243,34,76,332 ) );
 layout.addView( Icode );
 GUI_created( layout );
}
}

public void Start(){
STATE2 = STATE;
_Ocreate_in();
parent.IFile_IO.load();
}
public void GUI_created(AbsoluteLayout l){
STATE2 = STATE;
}
public void save_clicked(){
STATE2 = STATE;
parent.IFile_IO.save();
}
public void store_clicked(){
STATE2 = STATE;
parent.IControl.store();
}
public void ref_count_changed(int val){
STATE2 = STATE;
parent.IDisplay.set(val);
}
public void prev_clicked(){
STATE2 = STATE;
parent.IDisplay.prev();
}
public void next_clicked(){
STATE2 = STATE;
parent.IDisplay.next();
}
public void seimon0_created(TextView t){
STATE2 = STATE;
parent.IDisplay.seimon0_is(t);
}
public void seimon0_paint(Canvas g){
STATE2 = STATE;
parent.IDisplay.paint0(g);
}
public void seimon_created(TextView t){
STATE2 = STATE;
parent.IDisplay.seimon_is(t);
}
public void seimon_paint(Canvas g){
STATE2 = STATE;
parent.IDisplay.paint(g);
}
public void text_created(TextView t){
STATE2 = STATE;
parent.IDisplay.text_is(t);
}
public void result_created(TextView t){
STATE2 = STATE;
parent.ISetter.result_is(t);
}
public void xbright_changed(int val){
STATE2 = STATE;
parent.ISetter.bright_is(val);
}
public void xvstart_changed(int val){
STATE2 = STATE;
parent.ISetter.vstart_is(val);
}
public void xvend_changed(int val){
STATE2 = STATE;
parent.ISetter.vend_is(val);
}
public void weight_created(EditText e){
STATE2 = STATE;
parent.ISetter.weight_is(e);
}
public void code_created(EditText e){
STATE2 = STATE;
parent.ISetter.code_is(e);
}
private void _Ocreate_in(){
if( STATE2 != 1484673869 ) return;
// GUIを作成する
XGUI x = new XGUI();



//   InitState に遷移する
_SINIT();
}

//   InitState
private void _SINIT(){
STATE = 1484673869;
}
GUI( VoiceKeyboardControl pnt ){
 parent = pnt;
_SINIT();
}
}
Control IControl;
class Control{
VoiceKeyboardControl parent;
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
      if( max > thresh_recognize ) result( ((VoiceTemplate)(voice_template.get(maxi))).text+"("+max+")\n");
      else result("("+max+")\n");

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
           update_display();  // 落ち着いたら表示を更新する
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
public void store(){
_O93_in();
}
public void result(String s){
parent._O117_in(s);
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
private void _O93_in(){
// 録音した音声をテンプレートにセットする


// キーコードをセット
int c = Integer.parseInt( ((SpannableStringBuilder)code.getText()).toString() );
((VoiceTemplate)(voice_template.get(voice_no))).code = c;

// 音声の長さをセット
((VoiceTemplate)(voice_template.get(voice_no))).length = voice.length;

// 重み係数をセット
double w = Double.parseDouble( ((SpannableStringBuilder)weight.getText()).toString() );
((VoiceTemplate)(voice_template.get(voice_no))).weight = w;

// 音声データをセット
int vs = ( vstart * HEARING_WIDTH / 100 ) * HEARING_HEIGHT;
int ve = ( vend  * HEARING_WIDTH / 100 ) * HEARING_HEIGHT;
if( vs > voice.length ) vs = voice.length;
if( ve > voice.length ) ve = voice.length;
if( vs >= ve ) return;
int vlength = ve - vs;
double[] v = new double[ vlength ];
for( int i = 0; i < vlength; i++ ) v[ i ] = voice[ i + vs ];
((VoiceTemplate)(voice_template.get(voice_no))).voice = v;

// 表示を更新
update_display();

}
Control( VoiceKeyboardControl pnt ){
 parent = pnt;

}
}
File_IO IFile_IO;
class File_IO{
VoiceKeyboardControl parent;
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
File_IO( VoiceKeyboardControl pnt ){
 parent = pnt;

}
}
Display IDisplay;
class Display{
VoiceKeyboardControl parent;
Paint paint = new Paint();

public void prev(){
_O9_in();
}
public void next(){
_O11_in();
}
public void set(int val){
_O13_in(val);
}
public void seimon_is(TextView t){
_O15_in(t);
}
public void paint(Canvas g){
_O21_in(g);
}
public void text_is(TextView t){
_O18_in(t);
}
public void paint0(Canvas g){
_O6_in(g);
}
public void seimon0_is(TextView t){
_O26_in(t);
}
private void _O6_in(Canvas g){
// テンプレートを描画


dprint("display template\n");

double[] v = ((VoiceTemplate)(voice_template.get(voice_no))).voice;
if( v == null ) return;
double e = Math.exp( (80.0 - (double)bright) / 4 );
int xwidth = v.length / HEARING_HEIGHT;
int width   = seimon0.getWidth();
int height = seimon0.getHeight();
paint.setStyle(Style.FILL);
float dx = (float)width  / HEARING_WIDTH;
float dy = (float)height / HEARING_HEIGHT;
for( int x = 0; x < xwidth; x++ ){
  for( int y = 0; y < HEARING_HEIGHT; y++ ){
    int t = (int)( v[ HEARING_HEIGHT * x + y ] / e );
    if( t > 255 ) t = 255;
    paint.setColor(Color.rgb( t, t, t));
    float fx = dx * (float)x;
    float fy = dy * (float)y;
    g.drawRect( fx, height - fy - dy, fx + dx, height - fy, paint);
  }
}

dprint("display template end\n");


}
private void _O9_in(){
// 番号を-１する


voice_no--;
if(voice_no <0 ) voice_no = 0;
update_display();

}
private void _O11_in(){
// 番号を+１する



voice_no++;
if( voice_no >= voice_template.size() ) voice_no = voice_template.size() - 1;
update_display();

}
private void _O13_in(int val){
// 番号をセットする


voice_no = val * voice_template.size() / 100;
update_display();

}
private void _O15_in(TextView t){
seimon = t;

}
private void _O18_in(TextView t){
text = t;

}
private void _O21_in(Canvas g){
// 音声を描画


dprint("display voice\n");
dprint("HEARING_HEIGHT="+HEARING_HEIGHT+"\n");


double[] v = voice;
if( v == null ) return;
double e = Math.exp( (80.0 - (double)bright) / 4 );
int xwidth = v.length / HEARING_HEIGHT;
int width   = seimon.getWidth();
int height = seimon.getHeight();
paint.setStyle(Style.FILL);
float dx = (float)width  / HEARING_WIDTH;
float dy = (float)height / HEARING_HEIGHT;
for( int x = 0; x < xwidth; x++ ){
  for( int y = 0; y < HEARING_HEIGHT; y++ ){
    int t = (int)( v[ HEARING_HEIGHT * x + y ] / e );
    if( t > 255 ) t = 255;
    paint.setColor(Color.rgb( t, t, t));
    float fx = dx * (float)x;
    float fy = dy * (float)y;
    g.drawRect( fx, height - fy - dy, fx + dx, height - fy, paint);
  }
}

// 先頭を表示
int x = vstart * width / 100;
paint.setStyle(Paint.Style.STROKE); 
paint.setStrokeWidth(2.0f);
paint.setColor(Color.rgb( 0, 255, 0));
g.drawLine( x, 0, x, height-1, paint);

// 末尾を表示
x = vend * width / 100;
paint.setStyle(Paint.Style.STROKE); 
paint.setStrokeWidth(2.0f);
paint.setColor(Color.rgb( 255, 0, 0));
g.drawLine( x, 0, x, height-1, paint);



dprint("display voice end\n");


}
private void _O26_in(TextView t){
seimon0 = t;

}
Display( VoiceKeyboardControl pnt ){
 parent = pnt;

}
}
Setter ISetter;
class Setter{
VoiceKeyboardControl parent;
public void bright_is(int val){
_O119_in(val);
}
public void result_is(TextView t){
_O113_in(t);
}
public void vstart_is(int val){
_O136_in(val);
}
public void vend_is(int val){
_O139_in(val);
}
public void weight_is(EditText e){
_O142_in(e);
}
public void code_is(EditText e){
_O145_in(e);
}
private void _O119_in(int val){
bright = val;


update_display();

}
private void _O113_in(TextView t){
result = t;

}
private void _O136_in(int val){
vstart = val;


update_display();

}
private void _O139_in(int val){
vend = val;


update_display();

}
private void _O142_in(EditText e){
weight =e;

}
private void _O145_in(EditText e){
code =e;

}
Setter( VoiceKeyboardControl pnt ){
 parent = pnt;

}
}
VoiceKeyboardControl( ){
IGUI = new GUI( this );
IControl = new Control( this );
IFile_IO = new File_IO( this );
IDisplay = new Display( this );
ISetter = new Setter( this );

}
}
