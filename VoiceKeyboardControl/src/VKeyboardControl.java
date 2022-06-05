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
import android.os.Environment;

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
    public void onStop(){
      if( ap != null ){
         try{
           ap.dbg.close();
         } catch(Exception e){}
         ap.save();
         ap.exit();
      }
      super.onStop();
      finish();
      System.exit(0);
    }

}

class VoiceKeyboardControl{
Activity ACTIVITY;

// 音声テンプレート
Vector voice_template;
class VoiceTemplate{
  String text;    // 表示テキスト
  double weight; // 重み(優先順位)
  int code;        // キーコード
  double[] voice; // 音声データ
  VoiceTemplate( String t, double w, int c, double[] v ){

    text = t;
    weight = w;
    code = c;
    voice = v;  
  }
}


// 各種パラメータ
boolean flog_scale = false; // 周波数スケール
boolean alog_scale = false; // 振幅スケール
boolean auto_learn=false;
int startup_time = 300;
double sound_filter = 150.0;
double thresh_trigger_on = 2;
double thresh_trigger_off = 2;
int thresh_count_on = 4;
int thresh_count_off = 10;
double thresh_recognize = 0.1;
double bias = 1; // ノイズ抑制用バイアス
double acompress = 0.3;  // 振幅圧縮係数
double learn_param_o = 8;  // 学習パラメータ
double learn_param_x =128; // 学習パラメータ
double limit_length = 1.1; // 音声の長さ比較用


// 音声テンプレートファイル
File voice_data_file = new File( "/sdcard/VoiceData.txt" );


//File voice_data_file = new File( Environment.getDataDirectory(),"VoiceData.txt" );


// 定数
static final int SAMPLING_RATE = 16000;
static final int HEARING_HEIGHT = 128;
static final int SOUND_DFT_SIZE = 256;
static final int TRIGGER_MARGIN = 2;

// デバッグオブジェクト
boolean debug_mode = false;
String DEBUG_FILE="debug.txt";
FileWriter dbg;
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
      weight.setText( ""+((VoiceTemplate)(voice_template.get(voice_no))).weight );
      code.setText( ""+((VoiceTemplate)(voice_template.get(voice_no))).code );
    }
  });
}

// 変数
Handler handler = new Handler();
TextView seimon;   // 録音した音声の声紋
TextView seimon0; // テンプレートの声紋
EditText text;      // テキスト表示
EditText weight;   // 重み(優先順位) 
EditText code;     // キーコード
TextView result;   // 認識結果の表示
double[] voice;      // 録音した音声
double[] hosei;      // 周波数補正係数
int voice_no = 0;  // 音声番号
int tsize = 0;       // テンプレートのサイズ
int bright = 0;     // 声紋表示の輝度調節
boolean is_running = false; // 実行中フラグ

public void Start(){
IGUI.Start();
}
public void exit(){
IControl.exit();
}
public void save(){
IFile_IO.save();
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
config Iconfig;
 class config extends Button{
 config(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 193, 189, 189 ));
 setText( "CONFIG" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {config_clicked();}} );
}
}
learn Ilearn;
 class learn extends Button{
 learn(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 5, 3, 0 ));
 setBackgroundColor( Color.rgb( 193, 189, 189 ));
 setText( "LEARN" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {learn_clicked();}} );
}
}
ins Iins;
 class ins extends Button{
 ins(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 193, 189, 189 ));
 setText( "INS" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {ins_clicked();}} );
}
}
del Idel;
 class del extends Button{
 del(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 193, 189, 189 ));
 setText( "DEL" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {del_clicked();}} );
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
voice_no Ivoice_no;
 class voice_no extends SeekBar{
 voice_no(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setProgress( 0 );
 setMax( 100 );
 setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
 public void onStopTrackingTouch(SeekBar seekBar) {}
 public void onStartTrackingTouch(SeekBar seekBar) {}
 public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { voice_no_changed( progress ); }
 });
}
}
bright Ibright;
 class bright extends SeekBar{
 bright(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setProgress( 0 );
 setMax( 100 );
 setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
 public void onStopTrackingTouch(SeekBar seekBar) {}
 public void onStartTrackingTouch(SeekBar seekBar) {}
 public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { bright_changed( progress ); }
 });
}
}
length Ilength;
 class length extends SeekBar{
 length(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 setProgress( 0 );
 setMax( 100 );
 setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
 public void onStopTrackingTouch(SeekBar seekBar) {}
 public void onStartTrackingTouch(SeekBar seekBar) {}
 public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { length_changed( progress ); }
 });
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
 class text extends EditText{
 text(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 text_created( this );
}
}
weight Iweight;
 class weight extends EditText{
 weight(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 weight_created( this );
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
result Iresult;
 class result extends TextView{
 result(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 20f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 204 ));
 setText( "" );
 result_created( this );
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
LABEL16 ILABEL16;
 class LABEL16 extends TextView{
 LABEL16(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "Text" );
}
}
LABEL17 ILABEL17;
 class LABEL17 extends TextView{
 LABEL17(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "Weight" );
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
 setText( "Length" );
}
}
 XGUI(){
 AbsoluteLayout layout=new AbsoluteLayout(ACTIVITY);
layout.setBackgroundColor(Color.rgb( 223, 253, 248));
ACTIVITY.setContentView(layout);
ACTIVITY.setTitle("音声キーボード設定");
 Iconfig = new config();
 Iconfig.setLayoutParams( new AbsoluteLayout.LayoutParams( 218,60,424,6 ) );
 layout.addView( Iconfig );
 Ilearn = new learn();
 Ilearn.setLayoutParams( new AbsoluteLayout.LayoutParams( 306,72,144,882 ) );
 layout.addView( Ilearn );
 Iins = new ins();
 Iins.setLayoutParams( new AbsoluteLayout.LayoutParams( 198,62,2,6 ) );
 layout.addView( Iins );
 Idel = new del();
 Idel.setLayoutParams( new AbsoluteLayout.LayoutParams( 202,62,212,6 ) );
 layout.addView( Idel );
 Iprev = new prev();
 Iprev.setLayoutParams( new AbsoluteLayout.LayoutParams( 74,280,2,76 ) );
 layout.addView( Iprev );
 Inext = new next();
 Inext.setLayoutParams( new AbsoluteLayout.LayoutParams( 78,282,566,76 ) );
 layout.addView( Inext );
 Ivoice_no = new voice_no();
 Ivoice_no.setLayoutParams( new AbsoluteLayout.LayoutParams( 512,70,132,368 ) );
 layout.addView( Ivoice_no );
 Ibright = new bright();
 Ibright.setLayoutParams( new AbsoluteLayout.LayoutParams( 512,68,130,442 ) );
 layout.addView( Ibright );
 Ilength = new length();
 Ilength.setLayoutParams( new AbsoluteLayout.LayoutParams( 518,62,124,514 ) );
 layout.addView( Ilength );
 Iseimon0 = new seimon0();
 Iseimon0.setLayoutParams( new AbsoluteLayout.LayoutParams( 238,280,80,76 ) );
 layout.addView( Iseimon0 );
 Iseimon = new seimon();
 Iseimon.setLayoutParams( new AbsoluteLayout.LayoutParams( 236,282,326,76 ) );
 layout.addView( Iseimon );
 Itext = new text();
 Itext.setLayoutParams( new AbsoluteLayout.LayoutParams( 502,64,140,582 ) );
 layout.addView( Itext );
 Iweight = new weight();
 Iweight.setLayoutParams( new AbsoluteLayout.LayoutParams( 500,64,140,654 ) );
 layout.addView( Iweight );
 Icode = new code();
 Icode.setLayoutParams( new AbsoluteLayout.LayoutParams( 498,68,142,728 ) );
 layout.addView( Icode );
 Iresult = new result();
 Iresult.setLayoutParams( new AbsoluteLayout.LayoutParams( 634,70,0,804 ) );
 layout.addView( Iresult );
 ILABEL10 = new LABEL10();
 ILABEL10.setLayoutParams( new AbsoluteLayout.LayoutParams( 116,66,8,370 ) );
 layout.addView( ILABEL10 );
 ILABEL11 = new LABEL11();
 ILABEL11.setLayoutParams( new AbsoluteLayout.LayoutParams( 118,68,4,442 ) );
 layout.addView( ILABEL11 );
 ILABEL18 = new LABEL18();
 ILABEL18.setLayoutParams( new AbsoluteLayout.LayoutParams( 130,66,0,728 ) );
 layout.addView( ILABEL18 );
 ILABEL16 = new LABEL16();
 ILABEL16.setLayoutParams( new AbsoluteLayout.LayoutParams( 130,64,0,580 ) );
 layout.addView( ILABEL16 );
 ILABEL17 = new LABEL17();
 ILABEL17.setLayoutParams( new AbsoluteLayout.LayoutParams( 126,70,0,650 ) );
 layout.addView( ILABEL17 );
 ILABEL15 = new LABEL15();
 ILABEL15.setLayoutParams( new AbsoluteLayout.LayoutParams( 130,62,0,514 ) );
 layout.addView( ILABEL15 );
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
parent.Iconfig.start(l);
}
public void config_clicked(){
STATE2 = STATE;
parent.Iconfig.config();
}
public void learn_clicked(){
STATE2 = STATE;
parent.IControl.learn();
}
public void ins_clicked(){
STATE2 = STATE;
parent.IControl.ins();
}
public void del_clicked(){
STATE2 = STATE;
parent.IControl.del();
}
public void prev_clicked(){
STATE2 = STATE;
parent.IDisplay.prev();
}
public void next_clicked(){
STATE2 = STATE;
parent.IDisplay.next();
}
public void voice_no_changed(int val){
STATE2 = STATE;
parent.IDisplay.voice_no_set(val);
}
public void bright_changed(int val){
STATE2 = STATE;
parent.IDisplay.bright_set(val);
}
public void length_changed(int val){
STATE2 = STATE;
parent.IDisplay.length_set(val);
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
public void text_created(EditText e){
STATE2 = STATE;
parent.ISetter.text_is(e);
}
public void weight_created(EditText e){
STATE2 = STATE;
parent.ISetter.weight_is(e);
}
public void code_created(EditText e){
STATE2 = STATE;
parent.ISetter.code_is(e);
}
public void result_created(TextView t){
STATE2 = STATE;
parent.ISetter.result_is(t);
}
private void _Ocreate_in(){
if( STATE2 != 1499670570 ) return;
// GUIを作成する
XGUI x = new XGUI();



//   InitState に遷移する
_SINIT();
}

//   InitState
private void _SINIT(){
STATE = 1499670570;
}
GUI( VoiceKeyboardControl pnt ){
 parent = pnt;
_SINIT();
}
}
Control IControl;
class Control{
VoiceKeyboardControl parent;
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
      
  // 認識結果を送る
  if( max > thresh_recognize ){
    voice_no0 = maxi;
    result( ((VoiceTemplate)(voice_template.get(maxi))).text+"("+max+")\n");
  }

  else{
    voice_no0 = -1;
    result("***("+max+")\n");
  }
}



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
           update_display();  // 落ち着いたら表示を更新する
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

// 三角関数テーブル
double[][] sin_table, cos_table;

// 認識結果(音声番号)
int voice_no0;

public void start(){
_O89_in();
}
public void learn(){
_O93_in();
}
public void result(String s){
parent._O117_in(s);
}
public void ins(){
_O114_in();
}
public void del(){
_O116_in();
}
public void exit(){
_O118_in();
}
private void _O89_in(){
// 初期設定


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
private void _O93_in(){
// 録音した音声を学習する


if(tsize > 0){
  double[] v = new double[tsize];
  double[] u =((VoiceTemplate)(voice_template.get(voice_no))).voice;
  for(int i = 0; i < v.length; i++){
  if(i < u.length) v[i] = u[i]; else v[i] = 0;
  }
  for(int i = 0; i < v.length; i++){
    double d = 0;
    if(i < voice.length) d = voice[i];
    v[i] = ((learn_param_o - 1.0) * v[i] + d) / learn_param_o;
  }
  ((VoiceTemplate)(voice_template.get(voice_no))).voice = v;

  if(voice_no0 >= 0 && voice_no0 != voice_no){
    u =((VoiceTemplate)(voice_template.get(voice_no0))).voice;
    for(int i = 0; i < u.length; i++){
      double d = 0;
      if(i < tsize) d = voice[i];
      u[i] = ((learn_param_x - 1.0) * u[i] - d) / learn_param_x;
    }
  }

  update_display();
}

}
private void _O114_in(){
// 録音した音声をテンプレートに挿入する


if(voice == null) return;
double[] v = new double[voice.length];
for(int i = 0; i < voice.length; i++) v[i] = voice[i];
String t = ((SpannableStringBuilder)text.getText()).toString();
double w = Double.parseDouble(((SpannableStringBuilder)weight.getText()).toString());
int c = Integer.parseInt(((SpannableStringBuilder)code.getText()).toString());
VoiceTemplate vt = new VoiceTemplate(t, w, c, v);
voice_template.insertElementAt(vt, voice_no);

// 表示を更新
update_display();

}
private void _O116_in(){
// テンプレートから音声を削除する



voice_template.removeElementAt(voice_no);
int size = voice_template.size();
if(voice_no >= size) voice_no = size -1; 

// 表示を更新
update_display();

}
private void _O118_in(){
// 終了処理



try{
is_running = false;
record_thread.join();
} catch(Exception e){}

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
//音声テンプレートを読み込む(無い場合は作成する)


try{
  dbg = new FileWriter(new File(Environment.getExternalStorageDirectory(), DEBUG_FILE));
} catch(Exception e){}


try{

if(!voice_data_file.exists()){
  int cnt = 0;
  InputStream is = ACTIVITY.getResources().openRawResource(R.raw.voicedata);
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
  if( line.equals("auto_learn=true"))         auto_learn=true;
  if( line.equals("auto_learn=false"))        auto_learn=false;
  if( line.equals("flog_scale=true"))         flog_scale=true;
  if( line.equals("flog_scale=false"))        flog_scale=false;
  if( line.equals("alog_scale=true"))         alog_scale=true;
  if( line.equals("alog_scale=false"))        alog_scale=false;
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
  dout.write("auto_learn=" + auto_learn + "\n");
  dout.write("flog_scale=" + flog_scale + "\n");
  dout.write("alog_scale=" + alog_scale + "\n");
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
File_IO( VoiceKeyboardControl pnt ){
 parent = pnt;

}
}
Display IDisplay;
class Display{
VoiceKeyboardControl parent;
Paint paint = new Paint();

int HEARING_WIDTH = 64;

public void prev(){
_O9_in();
}
public void next(){
_O11_in();
}
public void voice_no_set(int v){
_O13_in(v);
}
public void paint(Canvas g){
_O21_in(g);
}
public void paint0(Canvas g){
_O6_in(g);
}
public void bright_set(int v){
_O28_in(v);
}
public void seimon0_is(TextView t){
_O36_in(t);
}
public void seimon_is(TextView t){
_O35_in(t);
}
public void length_set(int v){
_O42_in(v);
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

// 末尾を表示
float x = tsize / HEARING_HEIGHT * width / HEARING_WIDTH;
paint.setStyle(Paint.Style.STROKE); 
paint.setStrokeWidth(2.0f);
paint.setColor(Color.rgb( 255, 0, 0));
g.drawLine( x, 0, x, height-1, paint);


dprint("display template end\n");


}
private void _O9_in(){
// 番号を-１する


voice_no--;
if(voice_no < 0) voice_no = 0;
update_display();

}
private void _O11_in(){
// 番号を+１する



voice_no++;
if(voice_no >= voice_template.size()) voice_no = voice_template.size() - 1;
update_display();

}
private void _O13_in(int val){
// 番号をセットする


voice_no = val * (voice_template.size()-1) / 100;
update_display();

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

// 末尾を表示
float x = tsize / HEARING_HEIGHT * width / HEARING_WIDTH;
paint.setStyle(Paint.Style.STROKE); 
paint.setStrokeWidth(2.0f);
paint.setColor(Color.rgb( 255, 0, 0));
g.drawLine( x, 0, x, height-1, paint);

dprint("display voice end\n");


}
private void _O28_in(int v){
bright = v;


update_display();

}
private void _O36_in(TextView t){
seimon0 = t;

}
private void _O35_in(TextView t){
seimon = t;

}
private void _O42_in(int v){
// テンプレートのサイズをセット


tsize = v * HEARING_HEIGHT * HEARING_WIDTH / 100;
update_display();

}
Display( VoiceKeyboardControl pnt ){
 parent = pnt;

}
}
Setter ISetter;
class Setter{
VoiceKeyboardControl parent;
public void result_is(TextView t){
_O113_in(t);
}
public void code_is(EditText e){
_O145_in(e);
}
public void text_is(EditText t){
_O147_in(t);
}
public void weight_is(EditText t){
_O158_in(t);
}
private void _O113_in(TextView t){
result = t;

}
private void _O145_in(EditText e){
code =e;

}
private void _O147_in(EditText t){
text = t;

}
private void _O158_in(EditText t){
weight = t;

}
Setter( VoiceKeyboardControl pnt ){
 parent = pnt;

}
}
config Iconfig;
class config{
VoiceKeyboardControl parent;
// コンテナ
AbsoluteLayout main_container;
AbsoluteLayout config_container;
AbsoluteLayout equalizer_container;

// バックアップファイル
File backup_file = new File( Environment.getExternalStorageDirectory(),"VKeyboaed.bak" );

//各パラメータ
CheckBox cflog_scale;
CheckBox calog_scale;
CheckBox cauto_learn;
EditText csound_filter;
EditText cstartup_time;
EditText cthresh_trigger_on;
EditText cthresh_trigger_off;
EditText cthresh_count_on;
EditText cthresh_count_off;
EditText cthresh_recognize;
EditText cbias;
EditText cacompress;
EditText clearn_param_o;
EditText clearn_param_x;
EditText climit_length;

  // EditTextから数値を得る
  public int get_int( EditText e ){
    return Integer.parseInt( get_text(e) ); 
  }

  // EditTextから数値を得る
  public double get_double( EditText e ){
    return Double.parseDouble( get_text(e) ); 
  }

  // EditTextから文字列を得る
  public String get_text( EditText e ){
    return ((SpannableStringBuilder)e.getText()).toString(); 
  }


public void start(Object l){
_O5_in(l);
}
public void config(){
_O12_in();
}
private void _O5_in(Object o){

// メインコンテナを取得



main_container = (AbsoluteLayout)o;

IGUI.Start();
}
private void _O10_in(Object o){
// コンテナを取得



config_container = (AbsoluteLayout)o;
ACTIVITY.setContentView(main_container);

}
private void _O12_in(){
// データをセットして表示




  cflog_scale.setChecked(flog_scale);
  calog_scale.setChecked(alog_scale);
  cauto_learn.setChecked(auto_learn);
  cstartup_time.setText("" + startup_time);
  csound_filter.setText("" + sound_filter);
  cthresh_trigger_on.setText("" + thresh_trigger_on);
  cthresh_trigger_off.setText("" + thresh_trigger_off);
  cthresh_count_on.setText("" + thresh_count_on);
  cthresh_count_off.setText("" + thresh_count_off);
  cthresh_recognize.setText("" + thresh_recognize);
  cbias.setText("" + bias);
  cacompress.setText("" + acompress);
  clearn_param_o.setText("" + learn_param_o);
  clearn_param_x.setText("" + learn_param_x);
  climit_length.setText("" + limit_length);
  ACTIVITY.setContentView(config_container);
  

}
private void _O19_in(){
// 変数をセットして閉じる


  flog_scale=cflog_scale.isChecked();
  alog_scale=calog_scale.isChecked();
  auto_learn=cauto_learn.isChecked();
  startup_time=get_int(cstartup_time);
  sound_filter=get_double(csound_filter);
  thresh_trigger_on=get_double(cthresh_trigger_on);
  thresh_trigger_off=get_double(cthresh_trigger_off);
  thresh_count_on=get_int(cthresh_count_on);
  thresh_count_off=get_int(cthresh_count_off);
  thresh_recognize=get_double(cthresh_recognize);
  bias=get_double(cbias);
  acompress=get_double(cacompress);
  learn_param_o=get_double(clearn_param_o);
  learn_param_x=get_double(clearn_param_x);
  limit_length=get_double(climit_length);

  ACTIVITY.setContentView(main_container);
  

}
private void _O21_in(){
//データを消去


try{

  int cnt = 0;
  InputStream is = ACTIVITY.getResources().openRawResource(R.raw.voicedata);
  while (is.read()!=-1) cnt++;
  byte[] b = new byte[cnt];
  is.reset();
  is.read(b);
  is.close();
  FileOutputStream os = new FileOutputStream(voice_data_file);
  os.write(b);
  os.close();

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
private void _O23_in(){
// SDカードにバックアップする


//音声テンプレートを保存する

try{

  String line = null;
  int cnt = 0;
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

  // バックアップファイルにコピーする
  FileInputStream is = new FileInputStream(voice_data_file);;
  while (is.read()!=-1) cnt++;
  byte[] b = new byte[cnt];
  is.reset();
  is.read(b);
  is.close();
  FileOutputStream os = new FileOutputStream(backup_file);
  os.write(b);
  os.close();

} catch( Exception e ){dprint(e+"\n");}

}
private void _O25_in(){
//バックアップしたデータを呼び出す


try{
  String line = null;
  int cnt = 0;

  // バックアップファイルからコピーする
  FileInputStream is = new FileInputStream(backup_file);;
  while (is.read()!=-1) cnt++;
  byte[] b = new byte[cnt];
  is.reset();
  is.read(b);
  is.close();
  FileOutputStream os = new FileOutputStream(voice_data_file);
  os.write(b);
  os.close();



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
GUI IGUI;
class GUI{
int STATE, STATE2;
config parent;
 class XGUI{
equalizer Iequalizer;
 class equalizer extends Button{
 equalizer(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 14f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "EQUALIZER" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {equalizer_clicked();}} );
}
}
close Iclose;
 class close extends Button{
 close(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 14f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "X" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {close_clicked();}} );
}
}
backup Ibackup;
 class backup extends Button{
 backup(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 14f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "BACKUP" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {backup_clicked();}} );
}
}
restore Irestore;
 class restore extends Button{
 restore(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 14f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "RESTORE" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {restore_clicked();}} );
}
}
clear Iclear;
 class clear extends Button{
 clear(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 14f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "CLEAR" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {clear_clicked();}} );
}
}
cflog_scale Icflog_scale;
 class cflog_scale extends CheckBox{
 cflog_scale(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 cflog_scale_created( this );
}
}
LABEL8 ILABEL8;
 class LABEL8 extends TextView{
 LABEL8(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "flog_scale" );
}
}
calog_scale Icalog_scale;
 class calog_scale extends CheckBox{
 calog_scale(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 calog_scale_created( this );
}
}
cauto_learn Icauto_learn;
 class cauto_learn extends CheckBox{
 cauto_learn(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 cauto_learn_created( this );
}
}
csound_filter Icsound_filter;
 class csound_filter extends EditText{
 csound_filter(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 csound_filter_created( this );
}
}
LABEL11 ILABEL11;
 class LABEL11 extends TextView{
 LABEL11(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "alog_scale" );
}
}
LABEL12 ILABEL12;
 class LABEL12 extends TextView{
 LABEL12(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "startup time" );
}
}
cstartup_time Icstartup_time;
 class cstartup_time extends EditText{
 cstartup_time(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 cstartup_time_created( this );
}
}
LABEL14 ILABEL14;
 class LABEL14 extends TextView{
 LABEL14(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "sound_filter" );
}
}
LABEL15 ILABEL15;
 class LABEL15 extends TextView{
 LABEL15(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "thresh_trigger_on" );
}
}
LABEL16 ILABEL16;
 class LABEL16 extends TextView{
 LABEL16(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "thresh_trigger_off" );
}
}
LABEL17 ILABEL17;
 class LABEL17 extends TextView{
 LABEL17(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "thresh_count_on" );
}
}
LABEL18 ILABEL18;
 class LABEL18 extends TextView{
 LABEL18(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "thresh_count_off" );
}
}
LABEL19 ILABEL19;
 class LABEL19 extends TextView{
 LABEL19(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "thresh_recognize" );
}
}
LABEL20 ILABEL20;
 class LABEL20 extends TextView{
 LABEL20(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "bias" );
}
}
LABEL21 ILABEL21;
 class LABEL21 extends TextView{
 LABEL21(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "acompress" );
}
}
LABEL22 ILABEL22;
 class LABEL22 extends TextView{
 LABEL22(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "learn_param_o" );
}
}
LABEL23 ILABEL23;
 class LABEL23 extends TextView{
 LABEL23(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "limit_length" );
}
}
cthresh_trigger_on Icthresh_trigger_on;
 class cthresh_trigger_on extends EditText{
 cthresh_trigger_on(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 cthresh_trigger_on_created( this );
}
}
cthresh_trigger_off Icthresh_trigger_off;
 class cthresh_trigger_off extends EditText{
 cthresh_trigger_off(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 cthresh_trigger_off_created( this );
}
}
cthresh_count_on Icthresh_count_on;
 class cthresh_count_on extends EditText{
 cthresh_count_on(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 cthresh_count_on_created( this );
}
}
cthresh_count_off Icthresh_count_off;
 class cthresh_count_off extends EditText{
 cthresh_count_off(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 cthresh_count_off_created( this );
}
}
cthresh_recognize Icthresh_recognize;
 class cthresh_recognize extends EditText{
 cthresh_recognize(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 cthresh_recognize_created( this );
}
}
cbias Icbias;
 class cbias extends EditText{
 cbias(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 cbias_created( this );
}
}
cacompress Icacompress;
 class cacompress extends EditText{
 cacompress(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 cacompress_created( this );
}
}
clearn_param_o Iclearn_param_o;
 class clearn_param_o extends EditText{
 clearn_param_o(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 clearn_param_o_created( this );
}
}
clearn_param_x Iclearn_param_x;
 class clearn_param_x extends EditText{
 clearn_param_x(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 clearn_param_x_created( this );
}
}
climit_length Iclimit_length;
 class climit_length extends EditText{
 climit_length(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 climit_length_created( this );
}
}
LABEL31 ILABEL31;
 class LABEL31 extends TextView{
 LABEL31(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "auto_learn" );
}
}
LABEL33 ILABEL33;
 class LABEL33 extends TextView{
 LABEL33(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "learn_param_x" );
}
}
 XGUI(){
 AbsoluteLayout layout=new AbsoluteLayout(ACTIVITY);
layout.setBackgroundColor(Color.rgb( 217, 255, 253));
ACTIVITY.setContentView(layout);
ACTIVITY.setTitle("VKeyboard設定");
 Iequalizer = new equalizer();
 Iequalizer.setLayoutParams( new AbsoluteLayout.LayoutParams( 218,76,232,80 ) );
 layout.addView( Iequalizer );
 Iclose = new close();
 Iclose.setLayoutParams( new AbsoluteLayout.LayoutParams( 130,152,462,2 ) );
 layout.addView( Iclose );
 Ibackup = new backup();
 Ibackup.setLayoutParams( new AbsoluteLayout.LayoutParams( 216,74,4,0 ) );
 layout.addView( Ibackup );
 Irestore = new restore();
 Irestore.setLayoutParams( new AbsoluteLayout.LayoutParams( 218,72,232,0 ) );
 layout.addView( Irestore );
 Iclear = new clear();
 Iclear.setLayoutParams( new AbsoluteLayout.LayoutParams( 214,74,4,82 ) );
 layout.addView( Iclear );
 Icflog_scale = new cflog_scale();
 Icflog_scale.setLayoutParams( new AbsoluteLayout.LayoutParams( 52,48,296,164 ) );
 layout.addView( Icflog_scale );
 ILABEL8 = new LABEL8();
 ILABEL8.setLayoutParams( new AbsoluteLayout.LayoutParams( 288,46,0,162 ) );
 layout.addView( ILABEL8 );
 Icalog_scale = new calog_scale();
 Icalog_scale.setLayoutParams( new AbsoluteLayout.LayoutParams( 52,46,296,218 ) );
 layout.addView( Icalog_scale );
 Icauto_learn = new cauto_learn();
 Icauto_learn.setLayoutParams( new AbsoluteLayout.LayoutParams( 54,46,298,268 ) );
 layout.addView( Icauto_learn );
 Icsound_filter = new csound_filter();
 Icsound_filter.setLayoutParams( new AbsoluteLayout.LayoutParams( 294,52,300,376 ) );
 layout.addView( Icsound_filter );
 ILABEL11 = new LABEL11();
 ILABEL11.setLayoutParams( new AbsoluteLayout.LayoutParams( 288,48,0,214 ) );
 layout.addView( ILABEL11 );
 ILABEL12 = new LABEL12();
 ILABEL12.setLayoutParams( new AbsoluteLayout.LayoutParams( 290,54,0,320 ) );
 layout.addView( ILABEL12 );
 Icstartup_time = new cstartup_time();
 Icstartup_time.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,54,298,320 ) );
 layout.addView( Icstartup_time );
 ILABEL14 = new LABEL14();
 ILABEL14.setLayoutParams( new AbsoluteLayout.LayoutParams( 290,50,0,378 ) );
 layout.addView( ILABEL14 );
 ILABEL15 = new LABEL15();
 ILABEL15.setLayoutParams( new AbsoluteLayout.LayoutParams( 292,48,0,432 ) );
 layout.addView( ILABEL15 );
 ILABEL16 = new LABEL16();
 ILABEL16.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,48,0,484 ) );
 layout.addView( ILABEL16 );
 ILABEL17 = new LABEL17();
 ILABEL17.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,48,0,536 ) );
 layout.addView( ILABEL17 );
 ILABEL18 = new LABEL18();
 ILABEL18.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,48,0,588 ) );
 layout.addView( ILABEL18 );
 ILABEL19 = new LABEL19();
 ILABEL19.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,44,0,640 ) );
 layout.addView( ILABEL19 );
 ILABEL20 = new LABEL20();
 ILABEL20.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,42,0,688 ) );
 layout.addView( ILABEL20 );
 ILABEL21 = new LABEL21();
 ILABEL21.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,44,0,734 ) );
 layout.addView( ILABEL21 );
 ILABEL22 = new LABEL22();
 ILABEL22.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,46,0,782 ) );
 layout.addView( ILABEL22 );
 ILABEL23 = new LABEL23();
 ILABEL23.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,46,0,880 ) );
 layout.addView( ILABEL23 );
 Icthresh_trigger_on = new cthresh_trigger_on();
 Icthresh_trigger_on.setLayoutParams( new AbsoluteLayout.LayoutParams( 294,52,300,430 ) );
 layout.addView( Icthresh_trigger_on );
 Icthresh_trigger_off = new cthresh_trigger_off();
 Icthresh_trigger_off.setLayoutParams( new AbsoluteLayout.LayoutParams( 294,52,302,484 ) );
 layout.addView( Icthresh_trigger_off );
 Icthresh_count_on = new cthresh_count_on();
 Icthresh_count_on.setLayoutParams( new AbsoluteLayout.LayoutParams( 294,48,302,538 ) );
 layout.addView( Icthresh_count_on );
 Icthresh_count_off = new cthresh_count_off();
 Icthresh_count_off.setLayoutParams( new AbsoluteLayout.LayoutParams( 298,48,300,588 ) );
 layout.addView( Icthresh_count_off );
 Icthresh_recognize = new cthresh_recognize();
 Icthresh_recognize.setLayoutParams( new AbsoluteLayout.LayoutParams( 298,48,300,638 ) );
 layout.addView( Icthresh_recognize );
 Icbias = new cbias();
 Icbias.setLayoutParams( new AbsoluteLayout.LayoutParams( 298,46,300,686 ) );
 layout.addView( Icbias );
 Icacompress = new cacompress();
 Icacompress.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,46,302,734 ) );
 layout.addView( Icacompress );
 Iclearn_param_o = new clearn_param_o();
 Iclearn_param_o.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,46,302,782 ) );
 layout.addView( Iclearn_param_o );
 Iclearn_param_x = new clearn_param_x();
 Iclearn_param_x.setLayoutParams( new AbsoluteLayout.LayoutParams( 298,46,300,832 ) );
 layout.addView( Iclearn_param_x );
 Iclimit_length = new climit_length();
 Iclimit_length.setLayoutParams( new AbsoluteLayout.LayoutParams( 300,48,300,880 ) );
 layout.addView( Iclimit_length );
 ILABEL31 = new LABEL31();
 ILABEL31.setLayoutParams( new AbsoluteLayout.LayoutParams( 288,46,0,268 ) );
 layout.addView( ILABEL31 );
 ILABEL33 = new LABEL33();
 ILABEL33.setLayoutParams( new AbsoluteLayout.LayoutParams( 296,44,0,832 ) );
 layout.addView( ILABEL33 );
 GUI_created( layout );
}
}

public void Start(){
STATE2 = STATE;
_Ocreate_in();
parent.IEqualizer.start();
}
public void GUI_created(AbsoluteLayout l){
STATE2 = STATE;
parent._O10_in(l);
}
public void equalizer_clicked(){
STATE2 = STATE;
parent.IEqualizer.equalizer();
}
public void close_clicked(){
STATE2 = STATE;
parent._O19_in();
}
public void backup_clicked(){
STATE2 = STATE;
parent._O23_in();
}
public void restore_clicked(){
STATE2 = STATE;
parent._O25_in();
}
public void clear_clicked(){
STATE2 = STATE;
parent._O21_in();
}
public void cflog_scale_created(CheckBox c){
STATE2 = STATE;
parent.Isetter.cflog_scale_created(c);
}
public void calog_scale_created(CheckBox c){
STATE2 = STATE;
parent.Isetter.calog_scale_created(c);
}
public void cauto_learn_created(CheckBox c){
STATE2 = STATE;
parent.Isetter.cauto_learn_created(c);
}
public void csound_filter_created(EditText e){
STATE2 = STATE;
parent.Isetter.csound_filter_created(e);
}
public void cstartup_time_created(EditText e){
STATE2 = STATE;
parent.Isetter.cstartup_time_created(e);
}
public void cthresh_trigger_on_created(EditText e){
STATE2 = STATE;
parent.Isetter.cthresh_trigger_on_created(e);
}
public void cthresh_trigger_off_created(EditText e){
STATE2 = STATE;
parent.Isetter.cthresh_trigger_off_created(e);
}
public void cthresh_count_on_created(EditText e){
STATE2 = STATE;
parent.Isetter.cthresh_count_on_created(e);
}
public void cthresh_count_off_created(EditText e){
STATE2 = STATE;
parent.Isetter.cthresh_count_off_created(e);
}
public void cthresh_recognize_created(EditText e){
STATE2 = STATE;
parent.Isetter.cthresh_recognize_created(e);
}
public void cbias_created(EditText e){
STATE2 = STATE;
parent.Isetter.cbias_created(e);
}
public void cacompress_created(EditText e){
STATE2 = STATE;
parent.Isetter.cacompress_created(e);
}
public void clearn_param_o_created(EditText e){
STATE2 = STATE;
parent.Isetter.clearn_param_o_created(e);
}
public void clearn_param_x_created(EditText e){
STATE2 = STATE;
parent.Isetter.clearn_param_x_created(e);
}
public void climit_length_created(EditText e){
STATE2 = STATE;
parent.Isetter.climit_length_created(e);
}
private void _Ocreate_in(){
if( STATE2 != 1977056418 ) return;
// GUIを作成する
XGUI x = new XGUI();



//   InitState に遷移する
_SINIT();
}

//   InitState
private void _SINIT(){
STATE = 1977056418;
}
GUI( config pnt ){
 parent = pnt;
_SINIT();
}
}
Equalizer IEqualizer;
class Equalizer{
config parent;
// イコライザ用のシークバー
SeekBar e0,e1,e2,e3,e4,e5,e6,e7;

public void start(){
IGUI.Start();
}
public void equalizer(){
_O6_in();
}
private void _O4_in(Object o){
//  コンテナをセット



equalizer_container = (AbsoluteLayout)o;

}
private void _O6_in(){
// イコライザを表示


double a = 0;
int i=1;
a=0;
for(int j=1; j<HEARING_HEIGHT/8;i++, j++){
a+=hosei[i];
}
e0.setProgress((int)(a/(HEARING_HEIGHT/8-1)*100));
a=0;
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
a+=hosei[i];
}
e1.setProgress((int)(a/(HEARING_HEIGHT/8)*100));
a=0;
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
a+=hosei[i];
}
e2.setProgress((int)(a/(HEARING_HEIGHT/8)*100));
a=0;
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
a+=hosei[i];
}
e3.setProgress((int)(a/(HEARING_HEIGHT/8)*100));
a=0;
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
a+=hosei[i];
}
e4.setProgress((int)(a/(HEARING_HEIGHT/8)*100));
a=0;
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
a+=hosei[i];
}
e5.setProgress((int)(a/(HEARING_HEIGHT/8)*100));
a=0;
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
a+=hosei[i];
}
e6.setProgress((int)(a/(HEARING_HEIGHT/8)*100));
a=0;
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
a+=hosei[i];
}
e7.setProgress((int)(a/(HEARING_HEIGHT/8)*100));
ACTIVITY.setContentView(equalizer_container);

}
private void _O8_in(){
// イコライザを非表示


hosei[0]=0;
int i=1;
for(int j=1; j<HEARING_HEIGHT/8;i++, j++){
hosei[i] = (double)e0.getProgress()/100;
}
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
hosei[i] = (double)e1.getProgress()/100;
}
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
hosei[i] = (double)e2.getProgress()/100;
}
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
hosei[i] = (double)e3.getProgress()/100;
}
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
hosei[i] = (double)e4.getProgress()/100;
}
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
hosei[i] = (double)e5.getProgress()/100;
}
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
hosei[i] = (double)e6.getProgress()/100;
}
for(int j=0; j<HEARING_HEIGHT/8;i++, j++){
hosei[i] = (double)e7.getProgress()/100;
}
ACTIVITY.setContentView(config_container);

}
private void _O24_in(){
// メインコンテナを表示


ACTIVITY.setContentView(main_container);

}
GUI IGUI;
class GUI{
int STATE, STATE2;
Equalizer parent;
 class XGUI{
LABEL0 ILABEL0;
 class LABEL0 extends TextView{
 LABEL0(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 30f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 254, 254, 254 ));
 setText( "EQUALIZER" );
}
}
e0 Ie0;
 class e0 extends SeekBar{
 e0(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setProgress( 0 );
 setMax( 100 );
 e0_created( this );
}
}
e1 Ie1;
 class e1 extends SeekBar{
 e1(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setProgress( 0 );
 setMax( 100 );
 e1_created( this );
}
}
e2 Ie2;
 class e2 extends SeekBar{
 e2(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setProgress( 0 );
 setMax( 100 );
 e2_created( this );
}
}
e3 Ie3;
 class e3 extends SeekBar{
 e3(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setProgress( 0 );
 setMax( 100 );
 e3_created( this );
}
}
e4 Ie4;
 class e4 extends SeekBar{
 e4(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setProgress( 0 );
 setMax( 100 );
 e4_created( this );
}
}
e5 Ie5;
 class e5 extends SeekBar{
 e5(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setProgress( 0 );
 setMax( 100 );
 e5_created( this );
}
}
e6 Ie6;
 class e6 extends SeekBar{
 e6(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setProgress( 0 );
 setMax( 100 );
 e6_created( this );
}
}
e7 Ie7;
 class e7 extends SeekBar{
 e7(){
 super(ACTIVITY);
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setProgress( 0 );
 setMax( 100 );
 e7_created( this );
}
}
close Iclose;
 class close extends Button{
 close(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "X" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {close_clicked();}} );
}
}
 XGUI(){
 AbsoluteLayout layout=new AbsoluteLayout(ACTIVITY);
layout.setBackgroundColor(Color.rgb( 210, 252, 253));
ACTIVITY.setContentView(layout);
ACTIVITY.setTitle("VKeyboard設定");
 ILABEL0 = new LABEL0();
 ILABEL0.setLayoutParams( new AbsoluteLayout.LayoutParams( 562,88,0,8 ) );
 layout.addView( ILABEL0 );
 Ie0 = new e0();
 Ie0.setLayoutParams( new AbsoluteLayout.LayoutParams( 674,66,2,112 ) );
 layout.addView( Ie0 );
 Ie1 = new e1();
 Ie1.setLayoutParams( new AbsoluteLayout.LayoutParams( 672,72,0,194 ) );
 layout.addView( Ie1 );
 Ie2 = new e2();
 Ie2.setLayoutParams( new AbsoluteLayout.LayoutParams( 674,74,0,282 ) );
 layout.addView( Ie2 );
 Ie3 = new e3();
 Ie3.setLayoutParams( new AbsoluteLayout.LayoutParams( 674,72,0,370 ) );
 layout.addView( Ie3 );
 Ie4 = new e4();
 Ie4.setLayoutParams( new AbsoluteLayout.LayoutParams( 674,66,0,458 ) );
 layout.addView( Ie4 );
 Ie5 = new e5();
 Ie5.setLayoutParams( new AbsoluteLayout.LayoutParams( 674,68,0,544 ) );
 layout.addView( Ie5 );
 Ie6 = new e6();
 Ie6.setLayoutParams( new AbsoluteLayout.LayoutParams( 674,66,0,630 ) );
 layout.addView( Ie6 );
 Ie7 = new e7();
 Ie7.setLayoutParams( new AbsoluteLayout.LayoutParams( 674,74,0,714 ) );
 layout.addView( Ie7 );
 Iclose = new close();
 Iclose.setLayoutParams( new AbsoluteLayout.LayoutParams( 96,98,570,0 ) );
 layout.addView( Iclose );
 GUI_created( layout );
}
}

public void Start(){
STATE2 = STATE;
_Ocreate_in();
parent._O24_in();
}
public void GUI_created(AbsoluteLayout l){
STATE2 = STATE;
parent._O4_in(l);
}
public void e0_created(SeekBar b){
STATE2 = STATE;
parent.Isetter.e0_is(b);
}
public void e1_created(SeekBar b){
STATE2 = STATE;
parent.Isetter.e1_is(b);
}
public void e2_created(SeekBar b){
STATE2 = STATE;
parent.Isetter.e2_is(b);
}
public void e3_created(SeekBar b){
STATE2 = STATE;
parent.Isetter.e3_is(b);
}
public void e4_created(SeekBar b){
STATE2 = STATE;
parent.Isetter.e4_is(b);
}
public void e5_created(SeekBar b){
STATE2 = STATE;
parent.Isetter.e5_is(b);
}
public void e6_created(SeekBar b){
STATE2 = STATE;
parent.Isetter.e6_is(b);
}
public void e7_created(SeekBar b){
STATE2 = STATE;
parent.Isetter.e7_is(b);
}
public void close_clicked(){
STATE2 = STATE;
parent._O8_in();
}
private void _Ocreate_in(){
if( STATE2 != 94675961 ) return;
// GUIを作成する
XGUI x = new XGUI();



//   InitState に遷移する
_SINIT();
}

//   InitState
private void _SINIT(){
STATE = 94675961;
}
GUI( Equalizer pnt ){
 parent = pnt;
_SINIT();
}
}
setter Isetter;
class setter{
Equalizer parent;
public void e0_is(SeekBar b){
_O8_in(b);
}
public void e1_is(SeekBar b){
_O10_in(b);
}
public void e2_is(SeekBar b){
_O11_in(b);
}
public void e3_is(SeekBar b){
_O12_in(b);
}
public void e4_is(SeekBar b){
_O13_in(b);
}
public void e5_is(SeekBar b){
_O14_in(b);
}
public void e6_is(SeekBar b){
_O15_in(b);
}
public void e7_is(SeekBar b){
_O16_in(b);
}
private void _O8_in(SeekBar b){
e0=b;

}
private void _O10_in(SeekBar b){
e1=b;

}
private void _O11_in(SeekBar b){
e2=b;

}
private void _O12_in(SeekBar b){
e3=b;

}
private void _O13_in(SeekBar b){
e4=b;

}
private void _O14_in(SeekBar b){
e5=b;

}
private void _O15_in(SeekBar b){
e6=b;

}
private void _O16_in(SeekBar b){
e7=b;

}
setter( Equalizer pnt ){
 parent = pnt;

}
}
Equalizer( config pnt ){
 parent = pnt;
IGUI = new GUI( this );
Isetter = new setter( this );

}
}
setter Isetter;
class setter{
config parent;
public void cflog_scale_created(CheckBox c){
_O13_in(c);
}
public void calog_scale_created(CheckBox c){
_O15_in(c);
}
public void csound_filter_created(EditText e){
_O17_in(e);
}
public void cstartup_time_created(EditText e){
_O19_in(e);
}
public void cthresh_trigger_on_created(EditText e){
_O21_in(e);
}
public void cthresh_trigger_off_created(EditText e){
_O23_in(e);
}
public void cthresh_count_on_created(EditText e){
_O25_in(e);
}
public void cthresh_count_off_created(EditText e){
_O27_in(e);
}
public void cthresh_recognize_created(EditText e){
_O29_in(e);
}
public void cbias_created(EditText e){
_O34_in(e);
}
public void cacompress_created(EditText e){
_O36_in(e);
}
public void clearn_param_o_created(EditText e){
_O38_in(e);
}
public void climit_length_created(EditText e){
_O31_in(e);
}
public void clearn_param_x_created(EditText e){
_O42_in(e);
}
public void cauto_learn_created(CheckBox c){
_O44_in(c);
}
private void _O13_in(CheckBox c){
cflog_scale=c;

}
private void _O15_in(CheckBox c){
calog_scale=c;

}
private void _O17_in(EditText e){
csound_filter=e;

}
private void _O19_in(EditText e){
cstartup_time=e;

}
private void _O21_in(EditText e){
cthresh_trigger_on=e;

}
private void _O23_in(EditText e){
cthresh_trigger_off=e;

}
private void _O25_in(EditText e){
cthresh_count_on=e;

}
private void _O27_in(EditText e){
cthresh_count_off=e;

}
private void _O29_in(EditText e){
cthresh_recognize=e;

}
private void _O31_in(EditText e){
climit_length=e;

}
private void _O34_in(EditText e){
cbias=e;

}
private void _O36_in(EditText e){
cacompress=e;

}
private void _O38_in(EditText e){
clearn_param_o=e;

}
private void _O42_in(EditText e){
clearn_param_x=e;

}
private void _O44_in(CheckBox c){
cauto_learn=c;

}
setter( config pnt ){
 parent = pnt;

}
}
config( VoiceKeyboardControl pnt ){
 parent = pnt;
IGUI = new GUI( this );
IEqualizer = new Equalizer( this );
Isetter = new setter( this );

}
}
VoiceKeyboardControl( ){
IGUI = new GUI( this );
IControl = new Control( this );
IFile_IO = new File_IO( this );
IDisplay = new Display( this );
ISetter = new Setter( this );
Iconfig = new config( this );

}
}
