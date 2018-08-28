package apincer.android.vivohifi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import com.ting.mp3.android.R;

public class DACService extends Service {
   private static MediaPlayer player;
   Context context = this;
   AudioManager audioManager;

    private NotificationHelper notificationHelper;

   public IBinder onBind(Intent var1) {
      return null;
   }

   public void onDestroy() {
      try {
          if(player!=null) {
              boolean notifyToggled = getNotifyToggledPreference();
              player.stop();
              player.release();
              if (notifyToggled) {
                  Toast.makeText(context, "HI-FI Disabled", Toast.LENGTH_SHORT).show();
                  return;
              }
          }
      } catch (Exception var2) {
         ;
      }
   }

   public int onStartCommand(Intent var1, int var2, int var3) {
      this.audioManager = (AudioManager)this.getSystemService(Service.AUDIO_SERVICE);
       enableHifiMode();
      return Service.START_STICKY;
   }

    private void enableHifiMode() {
        try {
            boolean notifyToggled = getNotifyToggledPreference();
            boolean serviceEnabled = getServiceEnabledPreference();
            boolean wiredHeadsetOn = isWiredHeadsetOn();
            if(player != null){
                player.release();
            }

            if(serviceEnabled && wiredHeadsetOn) {
                player = MediaPlayer.create(context, R.raw.silence_song);
                player.setVolume(0.0F, 0.0F);
                player.setLooping(true);
                player.start();
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        enableHifiMode();
                    }
                });
                if (notifyToggled) {
                    Toast.makeText(context, "HI-FI Enabled", Toast.LENGTH_SHORT).show();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if(notificationHelper == null) {
                        notificationHelper = new NotificationHelper(this);
                    }
                    android.app.Notification.Builder notificationBuilder = notificationHelper.getNotification("Hi-Fi output mode", "Hi-Fi output mode for all applications");
                   // notificationHelper.notify(20000, notificationBuilder);
                    stopForeground(true);
                    startForeground(2000, notificationBuilder.build());
                }
            }
        }catch (Exception ex) {
            if(player!=null) {
                player.release();
            }
        }
    }

    private boolean getServiceEnabledPreference() {
        return this.getSharedPreferences("mainPreferences", 0).getBoolean("serviceEnabled", true);
    }

    private boolean getNotifyToggledPreference() {
        return this.getSharedPreferences("mainPreferences", 0).getBoolean("notifyToggled", true);
    }


    private boolean isWiredHeadsetOn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return audioManager.isWiredHeadsetOn();
        } else {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (int i = 0; i < devices.length; i++) {
                AudioDeviceInfo device = devices[i];
                if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    return true;
                }
            }
        }
        return false;
    }
}
