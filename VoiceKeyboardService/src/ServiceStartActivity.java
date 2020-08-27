package com.example.vkeyboard_service;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
public class ServiceStartActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent_service = new Intent(); 
    intent_service.setClassName( "com.example.vkeyboard_service", "com.example.vkeyboard_service.VKeyBoardService" ); 
    startService( intent_service );
    finish();
  }
}
