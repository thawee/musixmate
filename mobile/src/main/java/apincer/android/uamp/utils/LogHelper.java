/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package apincer.android.uamp.utils;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class LogHelper {

    private static final String LOG_PREFIX = "uamp_";
    private static final int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static final int MAX_LOG_TAG_LENGTH = 23;

    public static String makeLogTag(String str) {
        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1);
        }

        return LOG_PREFIX + str;
    }

    /**
     * Don't use this when obfuscating class names!
     */
    public static String makeLogTag(Class cls) {
        return makeLogTag(cls.getSimpleName());
    }


    public static void v(String tag, Object... messages) {
        // Only log VERBOSE if build type is DEBUG
        //if (BuildConfig.DEBUG) {
        //    log(tag, Log.VERBOSE, null, messages);
        //}
        Timber.v(tag, messages);
        log(tag, null, messages);
    }

    public static void d(String tag, Object... messages) {
        // Only log DEBUG if build type is DEBUG
        //if (BuildConfig.DEBUG) {
        //   log(tag, Log.DEBUG, null, messages);
        //}
        Timber.d(tag, messages);
        log(tag, null, messages);
    }

    public static void i(String tag, Object... messages) {
        //log(tag, Log.INFO, null, messages);
        Timber.i(tag, messages);
        log(tag, null, messages);
    }

    public static void w(String tag, Object... messages) {
        //log(tag, Log.WARN, null, messages);
        Timber.w(tag, messages);
        log(tag, null, messages);
    }

    public static void w(String tag, Throwable t, Object... messages) {
        //log(tag, Log.WARN, t, messages);
        Timber.w(t, tag, messages);
        log(tag, null, messages);
    }

    public static void e(String tag, Object... messages) {
        //log(tag, Log.ERROR, null, messages);
        Timber.e(tag, messages);
        log(tag, null, messages);
    }

    public static void e(String tag, Throwable t, Object... messages) {
        //log(tag, Log.ERROR, t, messages);
        Timber.e(t, tag, messages);
        log(tag, t, messages);
    }

    public static void log(String tag, Throwable t, Object... messages) {

        String message;
        if (t == null && messages != null && messages.length == 1) {
            // handle this common case without the extra cost of creating a stringbuffer:
            message = messages[0].toString();
        } else {
            StringBuilder sb = new StringBuilder();
            if (messages != null) for (Object m : messages) {
                sb.append(m);
            }
            if (t != null) {
                sb.append("\n").append(Log.getStackTraceString(t));
            }
            message = sb.toString();
        }
        if(t!=null) {
            logToFile(tag, message);
        }
    }

    private static String getDateTimeStamp() {
        Date dateNow = Calendar.getInstance().getTime();
        // My locale, so all the log files have the same date and time format
        return (DateFormat.getDateTimeInstance
                (DateFormat.SHORT, DateFormat.MEDIUM, Locale.CANADA_FRENCH).format(dateNow));
    }

    public static void logToFile(String logMessageTag, String logMessage) {
        try {
            // Gets the log file from the root of the primary storage. If it does
            // not exist, the file is created.
            File logFile = new File(Environment.getExternalStorageDirectory(),
                    "MusicMate.txt");
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            // Write the message to the log with a timestamp
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.write(String.format("%1s [%2s]:%3s\r\n",
                    getDateTimeStamp(), logMessageTag, logMessage));
            writer.close();
        } catch(RuntimeException ex) {
            System.out.println(logMessageTag +" "+ logMessage);
        }catch (IOException e) {
            Log.e("LogHelper", "Unable to log exception to file.");
        }
    }
}
