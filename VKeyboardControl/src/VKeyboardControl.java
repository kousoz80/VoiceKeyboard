package com.example.vkeyboard_control;

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
import android.content.Intent;
import android.graphics.Point;
import android.view.Window;
import android.app.ActivityManager;
import android.content.Context;

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
        if(!ap.service_start){
          ap.save();
          ap.purge();
        }
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
  String command; // コマンド
  double[] voice; // 音声データ
  VoiceTemplate( String t, String c, double[] v ){

    text = t;
    command = c;
    voice = v;  
  }
}


// 各種パラメータ
boolean auto_learn=false; // 自動学習
boolean writable=false; // 書き込み可能
int startup_time = 300;
double sound_filter = 150.0;
double thresh_trigger_on = 3;
double thresh_trigger_off = 3;
int thresh_count_on = 4;
int thresh_count_off = 10;
double thresh_recognize = 0.5;
double level = 4;  // 声紋のレベル調節
double acompress = 0.32;  // 振幅圧縮係数
double learn_param_o = 8;  // 学習パラメータ
double learn_param_x =128; // 学習パラメータ
double limit_length = 1.2; // 音声の長さ比較用
double enhance = 0;  // 強調パラメータ
int trigger_margin = 3;// トリガタイミング余裕

// 音声テンプレートファイル
File voice_data_file = new File( "/sdcard/VoiceData.txt" );


//File voice_data_file = new File( Environment.getDataDirectory(),"VoiceData.txt" );


// 定数
static final int SAMPLING_RATE = 16000;
static final int HEARING_HEIGHT = 128;
static final int SOUND_DFT_SIZE = 256;

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
      if( voice_no < voice_template.size()){
        text.setText(((VoiceTemplate)(voice_template.get(voice_no))).text );
        command.setText(((VoiceTemplate)(voice_template.get(voice_no))).command );
      }
    }
  });
}

// 変数
Handler handler = new Handler();
TextView seimon;   // 録音した音声の声紋
TextView seimon0; // テンプレートの声紋
EditText text;      // テキスト表示
EditText command;// コマンド
TextView result;   // 認識結果の表示
double[] voice;      // 録音した音声
double[] hosei;      // 周波数補正係数
int voice_no = 0;  // 音声番号
int tsize = 0;       // テンプレートのサイズ
boolean is_running = false; // 実行中フラグ
boolean service_start = false;

public void Start(){
IGUI.Start();
}
public void purge(){
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
private void _O176_in(){
// サービス起動
// して終了


service_start = true;
save();
purge();
try{
Intent intent = new Intent(); 
intent.setClassName("com.example.vkeyboard_service", "com.example.vkeyboard_service.VKeyboardService"); 
ACTIVITY.startService(intent);
} catch(Exception e){dprint(e+"\n");}
ACTIVITY.finish();

}
private void _O178_in(){

// サービス終了
// チェック


String pkg_name = "com.example.vkeyboard_service";
String class_name = "com.example.vkeyboard_service.VKeyboardService";
try{
Intent intent = new Intent(); 
intent.setClassName(pkg_name, class_name); 
ACTIVITY.stopService(intent);

ActivityManager manager = (ActivityManager)ACTIVITY.getSystemService(Context.ACTIVITY_SERVICE);
for(ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)){
  while(class_name.equals(serviceInfo.service.getClassName())) {
    Thread.sleep(100);
  }
}
} catch(Exception e){dprint(e+"\n");}


IFile_IO.load();
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
command Icommand;
 class command extends EditText{
 command(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 command_created( this );
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
LABEL18 ILABEL18;
 class LABEL18 extends TextView{
 LABEL18(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 252, 252, 252 ));
 setText( "Command" );
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
start_service Istart_service;
 class start_service extends Button{
 start_service(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 16f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "START SERVICE" );
 setOnClickListener(new Button.OnClickListener(){ public void onClick(View v) {start_service_clicked();}} );
}
}
 XGUI(){
 AbsoluteLayout layout=new AbsoluteLayout(ACTIVITY);
 double scale=0.0;
layout.setBackgroundColor(Color.rgb( 223, 253, 248));
ACTIVITY.setContentView(layout);
 int content_top = 150;
 Point size = new Point();
 ACTIVITY.getWindowManager().getDefaultDisplay().getSize(size);
 double wscl=(double)size.x/337;
 double hscl=(double)(size.y-content_top)/415;
 scale = wscl<hscl? wscl:hscl;
ACTIVITY.setTitle("音声キーボード設定");
 Iconfig = new config();
 Iconfig.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*109),(int)(scale*30),(int)(scale*212),(int)(scale*3)));
 layout.addView( Iconfig );
 Ilearn = new learn();
 Ilearn.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*137),(int)(scale*38),(int)(scale*0),(int)(scale*370)));
 layout.addView( Ilearn );
 Iins = new ins();
 Iins.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*99),(int)(scale*31),(int)(scale*1),(int)(scale*3)));
 layout.addView( Iins );
 Idel = new del();
 Idel.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*101),(int)(scale*31),(int)(scale*106),(int)(scale*3)));
 layout.addView( Idel );
 Iprev = new prev();
 Iprev.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*37),(int)(scale*140),(int)(scale*1),(int)(scale*38)));
 layout.addView( Iprev );
 Inext = new next();
 Inext.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*39),(int)(scale*141),(int)(scale*283),(int)(scale*38)));
 layout.addView( Inext );
 Ivoice_no = new voice_no();
 Ivoice_no.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*248),(int)(scale*35),(int)(scale*83),(int)(scale*184)));
 layout.addView( Ivoice_no );
 Ilength = new length();
 Ilength.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*250),(int)(scale*31),(int)(scale*83),(int)(scale*224)));
 layout.addView( Ilength );
 Iseimon0 = new seimon0();
 Iseimon0.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*119),(int)(scale*140),(int)(scale*40),(int)(scale*38)));
 layout.addView( Iseimon0 );
 Iseimon = new seimon();
 Iseimon.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*118),(int)(scale*141),(int)(scale*163),(int)(scale*38)));
 layout.addView( Iseimon );
 Itext = new text();
 Itext.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*251),(int)(scale*32),(int)(scale*84),(int)(scale*259)));
 layout.addView( Itext );
 Icommand = new command();
 Icommand.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*251),(int)(scale*34),(int)(scale*84),(int)(scale*294)));
 layout.addView( Icommand );
 Iresult = new result();
 Iresult.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*334),(int)(scale*35),(int)(scale*0),(int)(scale*331)));
 layout.addView( Iresult );
 ILABEL10 = new LABEL10();
 ILABEL10.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*76),(int)(scale*33),(int)(scale*4),(int)(scale*185)));
 layout.addView( ILABEL10 );
 ILABEL18 = new LABEL18();
 ILABEL18.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*80),(int)(scale*33),(int)(scale*0),(int)(scale*291)));
 layout.addView( ILABEL18 );
 ILABEL16 = new LABEL16();
 ILABEL16.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*80),(int)(scale*32),(int)(scale*0),(int)(scale*256)));
 layout.addView( ILABEL16 );
 ILABEL15 = new LABEL15();
 ILABEL15.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*80),(int)(scale*31),(int)(scale*0),(int)(scale*222)));
 layout.addView( ILABEL15 );
 Istart_service = new start_service();
 Istart_service.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*191),(int)(scale*37),(int)(scale*142),(int)(scale*371)));
 layout.addView( Istart_service );
 GUI_created( layout );
}
}

public void Start(){
STATE2 = STATE;
_Ocreate_in();
parent._O178_in();
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
public void command_created(EditText e){
STATE2 = STATE;
parent.ISetter.command_is(e);
}
public void result_created(TextView t){
STATE2 = STATE;
parent.ISetter.result_is(t);
}
public void start_service_clicked(){
STATE2 = STATE;
parent._O176_in();
}
private void _Ocreate_in(){
if( STATE2 != 1358667354 ) return;
// GUIを作成する
XGUI x = new XGUI();



//   InitState に遷移する
_SINIT();
}

//   InitState
private void _SINIT(){
STATE = 1358667354;
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
  for(int i = 0; i < voice_template.size(); i++ ){

    // テンプレートの音声
    double[] ref = ((VoiceTemplate)(voice_template.get(i))).voice;

    // 音声の長さを比べて範囲内なら比較する
    double p = (double)ref.length / voice.length;
    if(p < limit_length && 1/p < limit_length){

      // テンプレートと録音した音声の相関値を計算する(トリガタイミングの誤差を考慮して少しずつずらして比較して一番大きい値をとる)
      for(int j = -trigger_margin; j <= trigger_margin; j++){
        r = compare_voice( ref, voice, j );
        if(r > max){ max = r; maxi = i; }
      }
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
    double   a, x, y, u, v, t, p, trigger;

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
      double pw  = 0;
      double pw0 = 1; //初期値は１なのはゼロ除算対策
      for( i = 0; i < HEARING_HEIGHT; i++ ){
        for( x = y = 0.0, j = 0; j < SOUND_DFT_SIZE; j++ ){
          a = (double)sound_buffer[j];
          x += a * cos_table[i][j];
          y += a * sin_table[i][j];
        }
        // パワー値を得る(同時に周波数補正をかける)
        u = hearing_buffer[i + offset] = Math.pow( (x * x + y * y), acompress ) * hosei[i];
        v = sound_av[i] = ((sound_filter - 1.0) * sound_av[i] + u) / sound_filter;
        pw  += (u - v) * (u - v); // DFTスペクトルをベクトルに見立てて距離を求める
        pw0 += v * v;             // 平均化されたDFTベクトルの長さを求める
      }
      trigger = pw / pw0; // 正規化したDFT距離をトリガ条件とする

      // トリガの大きさに応じて強調処理をかける
      a = Math.pow(trigger, enhance);
      for( i = 0; i < HEARING_HEIGHT; i++ ){
        hearing_buffer[i + offset] *= a;
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
          start_point = offset - trigger_margin*HEARING_HEIGHT;  // スレッショルドを越える直前をサンプリング開始位置とする
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
          end_point = offset + trigger_margin*HEARING_HEIGHT; // スレッショルドを下回った所をサンプリング終了位置とする
          if(end_point >= HEARING_BUFFER_SIZE) end_point -= HEARING_BUFFER_SIZE;
          count = 0;
        }
        break;

      // ONからOFFに遷移する状態
      case SENS_OFF:
        if(trigger > thresh_trigger_on)  state = ON;
        else{
          if(count > thresh_count_off){

            // サンプリング範囲内のデータをレベル調節して音声データ配列voiceに転送する
            len = end_point - start_point;
            if(len < 0) len += HEARING_BUFFER_SIZE;
            voice = new double[len];
            for(i = start_point, j = 0; j < len; i++, j++){
              if(i >= HEARING_BUFFER_SIZE) i -= HEARING_BUFFER_SIZE;
              if((a = level * hearing_buffer[i]) > 255) a = 255; // レベル調節＆クリップ処理(ここ重要)
              voice[j] = a;
            }
            state = OFF;

            // 認識処理開始
            new Thread(new Runnable() {
	          @Override
	          public void run() {
                recognize(voice);
	          }
	        }).start();

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
  for(int i = 0; i < HEARING_HEIGHT; i++){
    for(int j = 0; j < SOUND_DFT_SIZE; j++){
      double w = 0.5 - 0.5 * Math.cos(j * 6.2832 / (SOUND_DFT_SIZE-1)); 
      sin_table[i][j] = w * Math.sin(i * j * 6.2832 / SOUND_DFT_SIZE);
      cos_table[i][j] = w * Math.cos(i * j * 6.2832 / SOUND_DFT_SIZE);
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

  // 報酬学習
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

  // ペナルティ学習
  if(voice_no0 >= 0 && voice_no0 != voice_no){
    u =((VoiceTemplate)(voice_template.get(voice_no0))).voice;
    for(int i = 0; i < u.length; i++){
      double d = 0;
      if(i < voice.length && i < tsize) d = voice[i];
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
String c = ((SpannableStringBuilder)command.getText()).toString();
VoiceTemplate vt = new VoiceTemplate(t, c, v);
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
if(record_thread != null){
  record_thread.join();
  record_thread = null;
}
} catch(Exception e){dprint(e+"\n");}

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
  if( line.equals("writable=true"))           writable=true;
  if( line.equals("writable=false"))          writable=false;
  if( line.startsWith("startup_time="))       startup_time=Integer.parseInt(line.substring(13));
  if( line.startsWith("sound_filter="))       sound_filter=Double.parseDouble(line.substring(13));
  if( line.startsWith("thresh_trigger_on="))  thresh_trigger_on=Double.parseDouble(line.substring(18));
  if( line.startsWith("thresh_trigger_off=")) thresh_trigger_off=Double.parseDouble(line.substring(19));
  if( line.startsWith("thresh_count_on="))    thresh_count_on=Integer.parseInt(line.substring(16));
  if( line.startsWith("thresh_count_off="))   thresh_count_off=Integer.parseInt(line.substring(17));
  if( line.startsWith("thresh_recognize="))   thresh_recognize=Double.parseDouble(line.substring(17));
  if( line.startsWith("level="))              level=Double.parseDouble(line.substring(6));
  if( line.startsWith("acompress="))          acompress=Double.parseDouble(line.substring(10));
  if( line.startsWith("learn_param_o="))      learn_param_o=Double.parseDouble(line.substring(14));
  if( line.startsWith("learn_param_x="))      learn_param_x=Double.parseDouble(line.substring(14));
  if( line.startsWith("limit_length="))       limit_length=Double.parseDouble(line.substring(13));
  if( line.startsWith("enhance="))            enhance=Double.parseDouble(line.substring(8));
  if( line.startsWith("trigger_margin="))     trigger_margin=Integer.parseInt(line.substring(15));
}

hosei = new double[HEARING_HEIGHT];
for(int i = 0; true; i++){
  if((line=din.readLine()) == null){din.close(); return;}
  if( line.equals("") ) break;
  hosei[i%HEARING_HEIGHT] = Double.parseDouble(line);
}

// 音声データを読み込む
voice_template = new Vector();
while(true){
  if((line=din.readLine()) == null){din.close(); return;}
  if(line.equals("END")) break;
  String t = line;
  if( ( line=din.readLine() ) == null ){din.close(); return;}
  String c = line;
  if( ( line=din.readLine() ) == null ){din.close(); return;}
  int s = Integer.parseInt(line);
  double[] v = new double[s];
  for(int i = 0; i < s; i++){
    if((line=din.readLine()) == null){din.close(); return;}
    v[i] = Double.parseDouble(line);
  }
  voice_template.add(new VoiceTemplate(t, c, v));
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
  dout.write("writable=" + writable + "\n");
  dout.write("startup_time=" + startup_time + "\n");
  dout.write("sound_filter=" + sound_filter + "\n");
  dout.write("thresh_trigger_on=" + thresh_trigger_on + "\n");
  dout.write("thresh_trigger_off=" + thresh_trigger_off + "\n");
  dout.write("thresh_count_on=" + thresh_count_on + "\n");
  dout.write("thresh_count_off=" + thresh_count_off + "\n");
  dout.write("thresh_recognize=" + thresh_recognize + "\n");
  dout.write("level=" + level + "\n");
  dout.write("acompress=" + acompress + "\n");
  dout.write("learn_param_o=" + learn_param_o + "\n");
  dout.write("learn_param_x=" + learn_param_x + "\n");
  dout.write("limit_length=" + limit_length + "\n");
  dout.write("enhance=" + enhance + "\n");
  dout.write("trigger_margin=" + trigger_margin + "\n");
  dout.write("\n");

  for(int i = 0; i < HEARING_HEIGHT; i++){
    dout.write(hosei[i] + "\n");
  }
  dout.write("\n");
  
  // 音声データを保存する
  for( int i = 0; i < voice_template.size(); i++ ){
    dout.write(((VoiceTemplate)voice_template.get(i)).text + "\n" );
    dout.write(((VoiceTemplate)voice_template.get(i)).command + "\n" );
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

if(voice_no >= voice_template.size()) return;
double[] v = ((VoiceTemplate)(voice_template.get(voice_no))).voice;
if(v == null) return;
int xwidth = v.length / HEARING_HEIGHT;
int width   = seimon0.getWidth();
int height = seimon0.getHeight();
paint.setStyle(Style.FILL);
float dx = (float)width  / HEARING_WIDTH;
float dy = (float)height / HEARING_HEIGHT;
for(int x = 0; x < xwidth; x++){
  for(int y = 0; y < HEARING_HEIGHT; y++){
    int t = (int)v[HEARING_HEIGHT * x + y];
    if(t > 255) t = 255;
    if(t < 0  ) t = 0;
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
if(v == null) return;
int xwidth = v.length / HEARING_HEIGHT;
int width   = seimon.getWidth();
int height = seimon.getHeight();
paint.setStyle(Style.FILL);
float dx = (float)width  / HEARING_WIDTH;
float dy = (float)height / HEARING_HEIGHT;
for(int x = 0; x < xwidth; x++){
  for(int y = 0; y < HEARING_HEIGHT; y++){
    int t = (int)v[HEARING_HEIGHT * x + y];
    if(t > 255) t = 255;
    if(t < 0)   t = 0;
    paint.setColor(Color.rgb(t, t, t));
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
public void command_is(EditText e){
_O145_in(e);
}
public void text_is(EditText t){
_O147_in(t);
}
private void _O113_in(TextView t){
result = t;

}
private void _O145_in(EditText e){
command =e;

}
private void _O147_in(EditText t){
text = t;

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
CheckBox cauto_learn;
CheckBox cwritable;
EditText csound_filter;
EditText cstartup_time;
EditText cthresh_trigger_on;
EditText cthresh_trigger_off;
EditText cthresh_count_on;
EditText cthresh_count_off;
EditText cthresh_recognize;
EditText cacompress;
EditText clearn_param_o;
EditText clearn_param_x;
EditText climit_length;
EditText cenhance;
EditText clevel;
EditText ctrigger_margin;

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




  cauto_learn.setChecked(auto_learn);
  cwritable.setChecked(writable);
  cstartup_time.setText("" + startup_time);
  csound_filter.setText("" + sound_filter);
  cthresh_trigger_on.setText("" + thresh_trigger_on);
  cthresh_trigger_off.setText("" + thresh_trigger_off);
  cthresh_count_on.setText("" + thresh_count_on);
  cthresh_count_off.setText("" + thresh_count_off);
  cthresh_recognize.setText("" + thresh_recognize);
  cacompress.setText("" + acompress);
  clearn_param_o.setText("" + learn_param_o);
  clearn_param_x.setText("" + learn_param_x);
  climit_length.setText("" + limit_length);
  cenhance.setText("" + enhance);
  clevel.setText("" + level);
  ctrigger_margin.setText("" + trigger_margin);
  ACTIVITY.setContentView(config_container);
  

}
private void _O19_in(){
// 変数をセットして閉じる


  auto_learn=cauto_learn.isChecked();
  writable=cwritable.isChecked();
  startup_time=get_int(cstartup_time);
  sound_filter=get_double(csound_filter);
  thresh_trigger_on=get_double(cthresh_trigger_on);
  thresh_trigger_off=get_double(cthresh_trigger_off);
  thresh_count_on=get_int(cthresh_count_on);
  thresh_count_off=get_int(cthresh_count_off);
  thresh_recognize=get_double(cthresh_recognize);
  acompress=get_double(cacompress);
  learn_param_o=get_double(clearn_param_o);
  learn_param_x=get_double(clearn_param_x);
  limit_length=get_double(climit_length);
  enhance=get_double(cenhance);
  level=get_double(clevel);
  trigger_margin=get_int(ctrigger_margin);

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
  if( line.equals("auto_learn=true"))         auto_learn=true;
  if( line.equals("auto_learn=false"))        auto_learn=false;
  if( line.equals("writable=true"))           writable=true;
  if( line.equals("writable=false"))          writable=false;
  if( line.startsWith("startup_time="))       startup_time=Integer.parseInt(line.substring(13));
  if( line.startsWith("sound_filter="))       sound_filter=Double.parseDouble(line.substring(13));
  if( line.startsWith("thresh_trigger_on="))  thresh_trigger_on=Double.parseDouble(line.substring(18));
  if( line.startsWith("thresh_trigger_off=")) thresh_trigger_off=Double.parseDouble(line.substring(19));
  if( line.startsWith("thresh_count_on="))    thresh_count_on=Integer.parseInt(line.substring(16));
  if( line.startsWith("thresh_count_off="))   thresh_count_off=Integer.parseInt(line.substring(17));
  if( line.startsWith("thresh_recognize="))   thresh_recognize=Double.parseDouble(line.substring(17));
  if( line.startsWith("level="))              level=Double.parseDouble(line.substring(6));
  if( line.startsWith("acompress="))          acompress=Double.parseDouble(line.substring(10));
  if( line.startsWith("learn_param_o="))      learn_param_o=Double.parseDouble(line.substring(14));
  if( line.startsWith("learn_param_x="))      learn_param_x=Double.parseDouble(line.substring(14));
  if( line.startsWith("limit_length="))       limit_length=Double.parseDouble(line.substring(13));
  if( line.startsWith("enhance="))            enhance=Double.parseDouble(line.substring(8));
  if( line.startsWith("trigger_margin="))     trigger_margin=Integer.parseInt(line.substring(15));
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
  String c = line;
  if( ( line=din.readLine() ) == null ){din.close(); return;}
  int s = Integer.parseInt(line);
  double[] v = new double[s];
  for(int i = 0; i < s; i++){
    if((line=din.readLine()) == null){din.close(); return;}
    v[i] = Double.parseDouble(line);
  }
  voice_template.add(new VoiceTemplate(t, c, v));
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
  dout.write("auto_learn=" + auto_learn + "\n");
  dout.write("writable=" + writable + "\n");
  dout.write("startup_time=" + startup_time + "\n");
  dout.write("sound_filter=" + sound_filter + "\n");
  dout.write("thresh_trigger_on=" + thresh_trigger_on + "\n");
  dout.write("thresh_trigger_off=" + thresh_trigger_off + "\n");
  dout.write("thresh_count_on=" + thresh_count_on + "\n");
  dout.write("thresh_count_off=" + thresh_count_off + "\n");
  dout.write("thresh_recognize=" + thresh_recognize + "\n");
  dout.write("level=" + level + "\n");
  dout.write("acompress=" + acompress + "\n");
  dout.write("learn_param_o=" + learn_param_o + "\n");
  dout.write("learn_param_x=" + learn_param_x + "\n");
  dout.write("limit_length=" + limit_length + "\n");
  dout.write("enhance=" + enhance + "\n");
  dout.write("trigger_margin=" + trigger_margin + "\n");
  dout.write("\n");

  for(int i = 0; i < HEARING_HEIGHT; i++){
    dout.write(hosei[i] + "\n");
  }
  dout.write("\n");
  
  // 音声データを保存する
  for( int i = 0; i < voice_template.size(); i++ ){
    dout.write(((VoiceTemplate)voice_template.get(i)).text + "\n" );
    dout.write(((VoiceTemplate)voice_template.get(i)).command + "\n" );
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
  if( line.equals("auto_learn=true"))         auto_learn=true;
  if( line.equals("auto_learn=false"))        auto_learn=false;
  if( line.equals("writable=true"))           writable=true;
  if( line.equals("writable=false"))          writable=false;
  if( line.startsWith("startup_time="))       startup_time=Integer.parseInt(line.substring(13));
  if( line.startsWith("sound_filter="))       sound_filter=Double.parseDouble(line.substring(13));
  if( line.startsWith("thresh_trigger_on="))  thresh_trigger_on=Double.parseDouble(line.substring(18));
  if( line.startsWith("thresh_trigger_off=")) thresh_trigger_off=Double.parseDouble(line.substring(19));
  if( line.startsWith("thresh_count_on="))    thresh_count_on=Integer.parseInt(line.substring(16));
  if( line.startsWith("thresh_count_off="))   thresh_count_off=Integer.parseInt(line.substring(17));
  if( line.startsWith("thresh_recognize="))   thresh_recognize=Double.parseDouble(line.substring(17));
  if( line.startsWith("level="))              level=Double.parseDouble(line.substring(6));
  if( line.startsWith("acompress="))          acompress=Double.parseDouble(line.substring(10));
  if( line.startsWith("learn_param_o="))      learn_param_o=Double.parseDouble(line.substring(14));
  if( line.startsWith("learn_param_x="))      learn_param_x=Double.parseDouble(line.substring(14));
  if( line.startsWith("limit_length="))       limit_length=Double.parseDouble(line.substring(13));
  if( line.startsWith("enhance="))            enhance=Double.parseDouble(line.substring(8));
  if( line.startsWith("trigger_margin="))     trigger_margin=Integer.parseInt(line.substring(15));
}

hosei = new double[HEARING_HEIGHT];
for(int i = 0; true; i++){
  if((line=din.readLine()) == null){din.close(); return;}
  if( line.equals("") ) break;
  hosei[i%HEARING_HEIGHT] = Double.parseDouble(line);
}

// 音声データを読み込む
voice_template = new Vector();
while(true){
  if((line=din.readLine()) == null){din.close(); return;}
  if(line.equals("END")) break;
  String t = line;
  if( ( line=din.readLine() ) == null ){din.close(); return;}
  String c = line;
  if( ( line=din.readLine() ) == null ){din.close(); return;}
  int s = Integer.parseInt(line);
  double[] v = new double[s];
  for(int i = 0; i < s; i++){
    if((line=din.readLine()) == null){din.close(); return;}
    v[i] = Double.parseDouble(line);
  }
  voice_template.add(new VoiceTemplate(t, c, v));
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
cwritable Icwritable;
 class cwritable extends CheckBox{
 cwritable(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 238, 238, 238 ));
 cwritable_created( this );
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
LABEL35 ILABEL35;
 class LABEL35 extends TextView{
 LABEL35(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "enhance" );
}
}
cenhance Icenhance;
 class cenhance extends EditText{
 cenhance(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 cenhance_created( this );
}
}
LABEL37 ILABEL37;
 class LABEL37 extends TextView{
 LABEL37(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "writable" );
}
}
LABEL36 ILABEL36;
 class LABEL36 extends TextView{
 LABEL36(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "level" );
}
}
clevel Iclevel;
 class clevel extends EditText{
 clevel(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 clevel_created( this );
}
}
LABEL35x ILABEL35x;
 class LABEL35x extends TextView{
 LABEL35x(){
 super(ACTIVITY);
 setGravity(Gravity.CENTER|Gravity.CENTER);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 192, 192, 192 ));
 setText( "trigger_margin" );
}
}
ctrigger_margin Ictrigger_margin;
 class ctrigger_margin extends EditText{
 ctrigger_margin(){
 super(ACTIVITY);
 setPadding(1, 1, 1, 1);
 setTextSize( 12f ); setTextColor( Color.rgb( 51, 51, 51 ));
 setBackgroundColor( Color.rgb( 255, 255, 255 ));
 setText( "" );
 ctrigger_margin_created( this );
}
}
 XGUI(){
 AbsoluteLayout layout=new AbsoluteLayout(ACTIVITY);
 double scale=0.0;
layout.setBackgroundColor(Color.rgb( 217, 255, 253));
ACTIVITY.setContentView(layout);
 int content_top = 150;
 Point size = new Point();
 ACTIVITY.getWindowManager().getDefaultDisplay().getSize(size);
 double wscl=(double)size.x/300;
 double hscl=(double)(size.y-content_top)/504;
 scale = wscl<hscl? wscl:hscl;
ACTIVITY.setTitle("VKeyboard設定");
 Iequalizer = new equalizer();
 Iequalizer.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*109),(int)(scale*38),(int)(scale*116),(int)(scale*40)));
 layout.addView( Iequalizer );
 Iclose = new close();
 Iclose.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*65),(int)(scale*76),(int)(scale*231),(int)(scale*1)));
 layout.addView( Iclose );
 Ibackup = new backup();
 Ibackup.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*108),(int)(scale*37),(int)(scale*2),(int)(scale*0)));
 layout.addView( Ibackup );
 Irestore = new restore();
 Irestore.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*109),(int)(scale*36),(int)(scale*116),(int)(scale*0)));
 layout.addView( Irestore );
 Iclear = new clear();
 Iclear.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*107),(int)(scale*37),(int)(scale*2),(int)(scale*41)));
 layout.addView( Iclear );
 Icauto_learn = new cauto_learn();
 Icauto_learn.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*27),(int)(scale*23),(int)(scale*149),(int)(scale*80)));
 layout.addView( Icauto_learn );
 Icwritable = new cwritable();
 Icwritable.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*28),(int)(scale*23),(int)(scale*149),(int)(scale*106)));
 layout.addView( Icwritable );
 Icsound_filter = new csound_filter();
 Icsound_filter.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*147),(int)(scale*26),(int)(scale*150),(int)(scale*159)));
 layout.addView( Icsound_filter );
 ILABEL12 = new LABEL12();
 ILABEL12.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*145),(int)(scale*27),(int)(scale*0),(int)(scale*130)));
 layout.addView( ILABEL12 );
 Icstartup_time = new cstartup_time();
 Icstartup_time.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*27),(int)(scale*149),(int)(scale*131)));
 layout.addView( Icstartup_time );
 ILABEL14 = new LABEL14();
 ILABEL14.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*146),(int)(scale*25),(int)(scale*0),(int)(scale*159)));
 layout.addView( ILABEL14 );
 ILABEL15 = new LABEL15();
 ILABEL15.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*147),(int)(scale*24),(int)(scale*0),(int)(scale*186)));
 layout.addView( ILABEL15 );
 ILABEL16 = new LABEL16();
 ILABEL16.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*24),(int)(scale*0),(int)(scale*212)));
 layout.addView( ILABEL16 );
 ILABEL17 = new LABEL17();
 ILABEL17.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*24),(int)(scale*0),(int)(scale*238)));
 layout.addView( ILABEL17 );
 ILABEL18 = new LABEL18();
 ILABEL18.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*24),(int)(scale*0),(int)(scale*264)));
 layout.addView( ILABEL18 );
 ILABEL19 = new LABEL19();
 ILABEL19.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*22),(int)(scale*0),(int)(scale*290)));
 layout.addView( ILABEL19 );
 ILABEL21 = new LABEL21();
 ILABEL21.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*22),(int)(scale*0),(int)(scale*314)));
 layout.addView( ILABEL21 );
 ILABEL22 = new LABEL22();
 ILABEL22.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*23),(int)(scale*0),(int)(scale*338)));
 layout.addView( ILABEL22 );
 ILABEL23 = new LABEL23();
 ILABEL23.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*23),(int)(scale*0),(int)(scale*387)));
 layout.addView( ILABEL23 );
 Icthresh_trigger_on = new cthresh_trigger_on();
 Icthresh_trigger_on.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*147),(int)(scale*26),(int)(scale*150),(int)(scale*185)));
 layout.addView( Icthresh_trigger_on );
 Icthresh_trigger_off = new cthresh_trigger_off();
 Icthresh_trigger_off.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*147),(int)(scale*26),(int)(scale*151),(int)(scale*212)));
 layout.addView( Icthresh_trigger_off );
 Icthresh_count_on = new cthresh_count_on();
 Icthresh_count_on.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*147),(int)(scale*24),(int)(scale*151),(int)(scale*239)));
 layout.addView( Icthresh_count_on );
 Icthresh_count_off = new cthresh_count_off();
 Icthresh_count_off.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*149),(int)(scale*24),(int)(scale*150),(int)(scale*264)));
 layout.addView( Icthresh_count_off );
 Icthresh_recognize = new cthresh_recognize();
 Icthresh_recognize.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*149),(int)(scale*24),(int)(scale*150),(int)(scale*289)));
 layout.addView( Icthresh_recognize );
 Icacompress = new cacompress();
 Icacompress.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*23),(int)(scale*151),(int)(scale*314)));
 layout.addView( Icacompress );
 Iclearn_param_o = new clearn_param_o();
 Iclearn_param_o.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*23),(int)(scale*151),(int)(scale*338)));
 layout.addView( Iclearn_param_o );
 Iclearn_param_x = new clearn_param_x();
 Iclearn_param_x.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*149),(int)(scale*23),(int)(scale*150),(int)(scale*362)));
 layout.addView( Iclearn_param_x );
 Iclimit_length = new climit_length();
 Iclimit_length.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*150),(int)(scale*24),(int)(scale*150),(int)(scale*386)));
 layout.addView( Iclimit_length );
 ILABEL31 = new LABEL31();
 ILABEL31.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*144),(int)(scale*23),(int)(scale*0),(int)(scale*81)));
 layout.addView( ILABEL31 );
 ILABEL33 = new LABEL33();
 ILABEL33.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*148),(int)(scale*22),(int)(scale*0),(int)(scale*363)));
 layout.addView( ILABEL33 );
 ILABEL35 = new LABEL35();
 ILABEL35.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*147),(int)(scale*22),(int)(scale*0),(int)(scale*412)));
 layout.addView( ILABEL35 );
 Icenhance = new cenhance();
 Icenhance.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*150),(int)(scale*23),(int)(scale*150),(int)(scale*412)));
 layout.addView( Icenhance );
 ILABEL37 = new LABEL37();
 ILABEL37.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*145),(int)(scale*22),(int)(scale*0),(int)(scale*106)));
 layout.addView( ILABEL37 );
 ILABEL36 = new LABEL36();
 ILABEL36.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*146),(int)(scale*20),(int)(scale*0),(int)(scale*436)));
 layout.addView( ILABEL36 );
 Iclevel = new clevel();
 Iclevel.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*151),(int)(scale*21),(int)(scale*148),(int)(scale*436)));
 layout.addView( Iclevel );
 ILABEL35x = new LABEL35x();
 ILABEL35x.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*146),(int)(scale*20),(int)(scale*0),(int)(scale*458)));
 layout.addView( ILABEL35x );
 Ictrigger_margin = new ctrigger_margin();
 Ictrigger_margin.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*151),(int)(scale*22),(int)(scale*148),(int)(scale*458)));
 layout.addView( Ictrigger_margin );
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
public void cauto_learn_created(CheckBox c){
STATE2 = STATE;
parent.Isetter.cauto_learn_created(c);
}
public void cwritable_created(CheckBox c){
STATE2 = STATE;
parent.Isetter.cwritable_created(c);
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
public void cenhance_created(EditText e){
STATE2 = STATE;
parent.Isetter.cenhance_created(e);
}
public void clevel_created(EditText e){
STATE2 = STATE;
parent.Isetter.clevel_created(e);
}
public void ctrigger_margin_created(EditText e){
STATE2 = STATE;
parent.Isetter.ctrigger_margin_created(e);
}
private void _Ocreate_in(){
if( STATE2 != 1729529841 ) return;
// GUIを作成する
XGUI x = new XGUI();



//   InitState に遷移する
_SINIT();
}

//   InitState
private void _SINIT(){
STATE = 1729529841;
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
 double scale=0.0;
layout.setBackgroundColor(Color.rgb( 210, 252, 253));
ACTIVITY.setContentView(layout);
 int bars_height=100;
 Point size = new Point();
 ACTIVITY.getWindowManager().getDefaultDisplay().getRealSize(size);
 double wscl=(double)size.x/335;
 double hscl=(double)(size.y-bars_height)/400;
 scale = wscl<hscl? wscl:hscl;
ACTIVITY.setTitle("VKeyboard設定");
 ILABEL0 = new LABEL0();
 ILABEL0.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*281),(int)(scale*44),(int)(scale*0),(int)(scale*4)));
 layout.addView( ILABEL0 );
 Ie0 = new e0();
 Ie0.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*337),(int)(scale*33),(int)(scale*1),(int)(scale*56)));
 layout.addView( Ie0 );
 Ie1 = new e1();
 Ie1.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*336),(int)(scale*36),(int)(scale*0),(int)(scale*97)));
 layout.addView( Ie1 );
 Ie2 = new e2();
 Ie2.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*337),(int)(scale*37),(int)(scale*0),(int)(scale*141)));
 layout.addView( Ie2 );
 Ie3 = new e3();
 Ie3.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*337),(int)(scale*36),(int)(scale*0),(int)(scale*185)));
 layout.addView( Ie3 );
 Ie4 = new e4();
 Ie4.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*337),(int)(scale*33),(int)(scale*0),(int)(scale*229)));
 layout.addView( Ie4 );
 Ie5 = new e5();
 Ie5.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*337),(int)(scale*34),(int)(scale*0),(int)(scale*272)));
 layout.addView( Ie5 );
 Ie6 = new e6();
 Ie6.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*337),(int)(scale*33),(int)(scale*0),(int)(scale*315)));
 layout.addView( Ie6 );
 Ie7 = new e7();
 Ie7.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*337),(int)(scale*37),(int)(scale*0),(int)(scale*357)));
 layout.addView( Ie7 );
 Iclose = new close();
 Iclose.setLayoutParams( new AbsoluteLayout.LayoutParams( (int)(scale*48),(int)(scale*49),(int)(scale*285),(int)(scale*0)));
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
if( STATE2 != 112512981 ) return;
// GUIを作成する
XGUI x = new XGUI();



//   InitState に遷移する
_SINIT();
}

//   InitState
private void _SINIT(){
STATE = 112512981;
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
public void cenhance_created(EditText e){
_O47_in(e);
}
public void cwritable_created(CheckBox c){
_O49_in(c);
}
public void clevel_created(EditText e){
_O53_in(e);
}
public void ctrigger_margin_created(EditText e){
_O56_in(e);
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
private void _O47_in(EditText e){
cenhance=e;

}
private void _O49_in(CheckBox c){
cwritable=c;

}
private void _O53_in(EditText e){
clevel=e;

}
private void _O56_in(EditText e){
ctrigger_margin=e;

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
