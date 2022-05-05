package ..;
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
    intent_service.setClassName( "..", "...VKeyBoardService" ); 
    startService( intent_service );
    finish();
  }
}
