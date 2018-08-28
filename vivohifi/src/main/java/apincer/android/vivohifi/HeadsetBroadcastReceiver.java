package apincer.android.vivohifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

public class HeadsetBroadcastReceiver extends BroadcastReceiver {
   boolean started = true;

   public void onReceive(Context context, Intent intent) {
      if(intent.getAction().equals("android.intent.action.HEADSET_PLUG")) {
         if(this.started) {
            this.started = false;
            return;
         }

         SharedPreferences pref = context.getSharedPreferences("mainPreferences", 0);
         boolean serviceEnabled = pref.getBoolean("serviceEnabled", false);
         if(serviceEnabled) {
            switch(intent.getIntExtra("state", -1)) {
            case 0:
                context.stopService(new Intent(context, DACService.class));
               break;
            case 1:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, DACService.class));
                } else {
                    context.startService(new Intent(context, DACService.class));
                }
               return;
            }
         }
      }
   }
}
