package apincer.android.vivohifi;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.ting.mp3.android.R;

import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
   Context context = this;
   SharedPreferences preferences;
   boolean serviceEnabled = false;
   boolean notifyToggled = true;
   private AudioManager mAudioMgr;

   static boolean isRunning(MainActivity var0, Class var1) {
      return var0.isRunning(var1);
   }

   private boolean isRunning(Class var1) {
      Iterator activities = ((ActivityManager)this.getSystemService(Service.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE).iterator();
      RunningServiceInfo var3;
      do {
         if(!activities.hasNext()) {
            return false;
         }

         var3 = (RunningServiceInfo)activities.next();
      } while(!var1.getName().equals(var3.service.getClassName()));

      return true;
   }

   public void savePreferences() {
      try {
         Editor pref = this.preferences.edit();
         pref.putBoolean("serviceEnabled", this.serviceEnabled);
         pref.putBoolean("notifyToggled", this.notifyToggled);
         pref.commit();
      } catch (Exception var3) {
         ;
      }
   }

   protected void onCreate(Bundle bundle) {
      super.onCreate(bundle);
      this.setContentView(R.layout.activity_main);
      this.mAudioMgr = (AudioManager)this.getSystemService(Service.AUDIO_SERVICE);
       this.preferences = this.getSharedPreferences("mainPreferences", 0);
      if(bundle == null) {
         this.serviceEnabled = this.preferences.getBoolean("serviceEnabled", this.serviceEnabled);
         this.notifyToggled = this.preferences.getBoolean("notifyToggled", this.notifyToggled);
      } else {
         this.serviceEnabled = bundle.getBoolean("serviceEnabled", this.serviceEnabled);
         this.notifyToggled = bundle.getBoolean("notifyToggled", this.notifyToggled);
      }

      final Toolbar var2 = (Toolbar)this.findViewById(R.id.toolbar);
      var2.setTitle("HI-FI on VIVO Music Phone");
      this.setSupportActionBar(var2);
      Switch btnServiceEnabled = (Switch)this.findViewById(R.id.btn_enable);
      final Switch btnNotify = (Switch)this.findViewById(R.id.btn_notify);
      if(this.serviceEnabled) {
         btnServiceEnabled.setChecked(true);
         btnNotify.setEnabled(true);
         if(!this.isRunning(DACService.class) && isWiredHeadsetOn()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               startForegroundService(new Intent(context, DACService.class));
            } else {
               startService(new Intent(context, DACService.class));
            }
         }
      } else {
         btnNotify.setEnabled(false);
      }

      btnServiceEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
         @Override
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
               btnNotify.setEnabled(true);
                serviceEnabled = true;

               if(!MainActivity.isRunning(MainActivity.this, DACService.class) && isWiredHeadsetOn()) {
                  startService(new Intent(context, DACService.class));
               }
            } else {
               btnNotify.setEnabled(false);
                serviceEnabled = false;
               if(MainActivity.isRunning(MainActivity.this, DACService.class)) {
                  stopService(new Intent(context, DACService.class));
               }
            }
            savePreferences();
         }
      });
      btnNotify.setChecked(this.notifyToggled);
      btnNotify.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
         @Override
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                notifyToggled = true;
            } else {
                notifyToggled = false;
            }
            savePreferences();
         }
      });
   }

   private boolean isWiredHeadsetOn() {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
         return mAudioMgr.isWiredHeadsetOn();
      } else {
         AudioDeviceInfo[] devices = mAudioMgr.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
         for (int i = 0; i < devices.length; i++) {
            AudioDeviceInfo device = devices[i];
            if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
               return true;
            }
         }
      }
      return false;
   }

   public boolean onCreateOptionsMenu(Menu var1) {
      this.getMenuInflater().inflate(R.menu.menu_main, var1);
      return true;
   }

   public boolean onOptionsItemSelected(MenuItem menu) {
      if(menu.getItemId() == R.id.menu_about) {
         this.startActivity(new Intent(context, AboutActivity.class));
         return true;
      } else {
         return super.onOptionsItemSelected(menu);
      }
   }

   public void onPause() {
      this.savePreferences();
      super.onPause();
   }
}
