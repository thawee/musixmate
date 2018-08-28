package apincer.android.vivohifi;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class AboutActivity extends AppCompatActivity {

   protected void onCreate(Bundle var1) {
      super.onCreate(var1);
      this.setContentView(com.ting.mp3.android.R.layout.activity_about);
      this.getIntent();

      this.setSupportActionBar((Toolbar)this.findViewById(com.ting.mp3.android.R.id.toolbar));

   }

   public boolean onCreateOptionsMenu(Menu var1) {
      return true;
   }

   public boolean onOptionsItemSelected(MenuItem var1) {
      switch(var1.getItemId()) {
      case 16908332:
         this.finish();
         return true;
      default:
         return super.onOptionsItemSelected(var1);
      }
   }

   public void onSaveInstanceState(Bundle var1) {
      super.onSaveInstanceState(var1);
   }
}
