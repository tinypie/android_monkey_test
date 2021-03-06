package com.droidlogic.monkeytest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.Environment;
import android.content.Context;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.text.InputType;
import android.util.Log;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class MainActivity extends Activity implements OnClickListener {
    static final String TAG = "MonkeyTest";
    static final boolean DEBUG = true;
    private static final int queryReturnOk = 0;
    private int mRunningTime = 0;

    private String mEventSetting = "-s 1000 --pct-trackball 0 --pct-touch 0 --pct-motion 0 --pct-anyevent 0 --throttle 300 1200000000";
    private String mBlacklist = "--pkg-blacklist-file /cache/blacklist.txt";
    private String mWhitelist;
    private String mDebuggingSetting = "--ignore-crashes --ignore-timeouts --ignore-security-exceptions --kill-process-after-error";
    private String mGeneralSetting = "-v ";
    private String mOutputSetting = "> /dev/null 2>&1";
    private String mConstraints;
    static final String LOGTIME = "/storage/emulated/legacy/time.txt";
    private String mLogFile = "/cache/android.log";
    private String mLogSize = "2048";
    private int mLogNum = 10;
    private String mKLog= "/cache/kernel.log";
    static String mDefaultCmd;

    private Button mUseDefaultSetting;
    private Button mStartButton;
    private Button mStopButton;
    private Button mDebugButton;
    private Button mSelectButton;
    private Button mLogButton;
    private Button mEventButton;
    private Button mLogCpyButton;

    private TextView mBlacklistTextView;
    private TextView mGeneralTextView;
    private TextView mEventsTextView;
    private TextView mDebuggingTextView;
    private TextView mConstraintsTextView;
    private TextView mFullCmd;
    private TextView mMonkeyResult;
    private TextView mLogSetting;

    private StorageManager mStorageManager;

    MonkeyThread mMonkeyThread;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStorageManager = (StorageManager)getSystemService(Context.STORAGE_SERVICE);

        mStartButton = (Button) findViewById(R.id.button_start);
        mStopButton = (Button) findViewById(R.id.button_stop);
        mUseDefaultSetting = (Button) findViewById(R.id.button_default);
        mSelectButton = (Button) findViewById(R.id.button_blacklist);
        mLogButton = (Button) findViewById(R.id.button_log);
        mEventButton = (Button) findViewById(R.id.button_events);
        mLogCpyButton = (Button) findViewById(R.id.button_copyLog);

        mDebugButton = (Button) findViewById(R.id.button_debugging);
        mDebuggingTextView = (TextView) findViewById(R.id.debugging_textview);
        mFullCmd = (TextView) findViewById(R.id.monkey_cmd);
        mMonkeyResult = (TextView) findViewById(R.id.monkey_results);
        mBlacklistTextView = (TextView) findViewById(R.id.blacklist_textview);
        mEventsTextView = (TextView) findViewById(R.id.events_textview);
        mLogSetting = (TextView) findViewById(R.id.log_textview);

        // generate default cmd
        mDefaultCmd = "monkey " + mBlacklist + " " + mDebuggingSetting
            + " " + mGeneralSetting + " " + mEventSetting + " " + mOutputSetting;

        mFullCmd.setText(mDefaultCmd);
        mMonkeyThread = MonkeyThread.getInstance();
        mMonkeyThread.init(mDefaultCmd);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mDebugButton.setOnClickListener(this);
        mUseDefaultSetting.setOnClickListener(this);
        mSelectButton.setOnClickListener(this);
        mLogButton.setOnClickListener(this);
        mEventButton.setOnClickListener(this);
        mLogCpyButton.setOnClickListener(this);

        String time = SystemProperties.get("persist.monkey.test");

        if (mMonkeyThread.isRunning) {
            mStartButton.setVisibility(View.GONE);
            //mStopButton.setVisibility(View.GONE);
        }

        if (DEBUG)
            Log.d(TAG, "monkey runtime " + time + " minutes, get from property");
        if (time != null && !time.isEmpty()) {
            mMonkeyResult.setText("Previous Run time:" + time + " minutes");
        }

        try {
            copyBlacklist();
        } catch (IOException e) {
            Log.e(TAG, "copy black list error");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    private void useDefaultSetting() {
        if (DEBUG) {
            Log.v(TAG, "monkey test use default config");
            Log.v(TAG, "set all monkey setting to default");
        }

        mBlacklistTextView.setText(R.string.blacklist_file);
        mEventsTextView.setText(R.string.events_string);
        mDebuggingTextView.setText(R.string.debugging_string);

        mBlacklist = getResources().getString(R.string.blacklist_file);
        mDebuggingSetting = getResources().getString(R.string.debugging_string);
        mGeneralSetting = getResources().getString(R.string.general_string);
        mEventSetting = getResources().getString(R.string.events_string);

        mBlacklist = "--pkg-blacklist-file " + mBlacklist;
        mDefaultCmd = "monkey " + mBlacklist + " " + mDebuggingSetting
            + " " + mGeneralSetting + " " + mEventSetting + " " + mOutputSetting;

        mFullCmd.setText(mDefaultCmd);
    }

    @Override
    public void onClick (View v) {
        switch (v.getId()) {
        case R.id.button_blacklist:
            Intent intent = new Intent(this, FileSelectActivity.class);
            startActivityForResult(intent, 1);
            break;
        case R.id.button_start:
            if (!mMonkeyThread.isRunning) {
                mRunningTime = 0;
                mMonkeyThread.init(mDefaultCmd);
                MonkeyThread.getInstance().start();
                startLogThread();
               // startKernelLog();
                startTimeThread();
            } else {
                Log.d(TAG, "monkey is already runing");
            }

            this.finish();
            break;
        case R.id.button_debugging:
            onDebugButtonClicked();
            break;
        case R.id.button_stop:
            onStopButtonClicked();
            break;
        case R.id.button_default:
            useDefaultSetting();
            break;
        case R.id.button_log:
            showEditDialog();
            break;
        case R.id.button_events:
            showEventsDialog();
        case R.id.button_copyLog:
            doCopyLog();
        }

    }

    public void onDebugButtonClicked() {
        DebugSettingDialogFragment debugFragment = new DebugSettingDialogFragment();
        debugFragment.show(getFragmentManager(), "Debug Setting");
    }

    public void onUserSelectDebugValue(String value) {
        Log.d(TAG, "getDatafromDialog :" + value);
        mDebuggingSetting = value;
        mDebuggingTextView.setText(value);
        mDefaultCmd = "monkey " + mBlacklist + " " + mDebuggingSetting
            + " " + mGeneralSetting + " " + mEventSetting + " " + mOutputSetting;
        mFullCmd.setText(mDefaultCmd);
    }

    public void startLogThread() {
        new Thread() {
            public void run() {
                Log.d(TAG, "logcat save to :" + mLogFile);
                startLog();
            }
        }.start();
    }


    public void startKernelLog() {
        SystemProperties.set("logd.klogd.dump","true");
    }


    public void startLog() {
        try {
            String logFile;
            for (int i = 0; i < mLogNum; i++) {
                logFile = mLogFile + (i > 0 ? "."+i : "");
                File file = new File(logFile);
                if(file.exists() && file.delete()) {
                    Log.d(TAG, "previous log file: " + logFile + " is deleted");
                }
            }
        } catch(Exception e){
            Log.d(TAG, "delet previous log file error");
            //e.printStackTrace();
        }

        try {
            int status;
            Log.d(TAG, "exec logcat -c ");
            Process logcat = Runtime.getRuntime().exec("logcat -c");
            status = logcat.waitFor();

            String cmd = "logcat -b all -v time -f " + mLogFile + " -r " + mLogSize + " -n " + mLogNum + " &";

            Log.d(TAG, " logcat cmd:" + cmd);
            logcat = Runtime.getRuntime().exec(cmd);

            status = logcat.waitFor();
            Log.d(TAG, "cmd :" + cmd + " returned " + status);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException (e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            if ((requestCode == 1) && (resultCode == queryReturnOk)) {
                Bundle bundle = data.getExtras();
                String file = bundle.getString(FileSelectActivity.FILE);
                if (file != null) {
                    mBlacklistTextView.setText(file);
                    mBlacklist = "--pkg-blacklist-file " + file;
                    mDefaultCmd = "monkey " + mBlacklist + " " + mDebuggingSetting
                        + " " + mGeneralSetting + " " + mEventSetting + " " + mOutputSetting;
                    mFullCmd.setText(mDefaultCmd);
                }
            }
        }
    }

    public void startTimeThread() {
        new Thread() {
            public void run() {
                while (true) {
                    SystemClock.sleep(60*1000);
                    if (DEBUG)
                        Log.d(TAG, "monkey already running " + mRunningTime + " minutes");
                    if (mMonkeyThread.isRunning) {
                        mRunningTime++;
                        //if (mRunningTime % 5 == 0)
                        SystemProperties.set("persist.monkey.test",Integer.toString(mRunningTime));
                    } else {
                        Log.d(TAG, "monkey thread died");
                        break;
                    }
                }
            }
        }.start();
    }

    private void showEditDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.log_dialog, null);

        final EditText place = (EditText) alertLayout.findViewById(R.id.logPlace_editText);
        final EditText size = (EditText) alertLayout.findViewById(R.id.logSize_editText);
        final EditText number = (EditText) alertLayout.findViewById(R.id.logNum_editText);

        place.setText("/cache/monkey");
        size.setText("2048");
        number.setText("10");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Android logfile save place");
        builder.setView(alertLayout);
        builder.setCancelable(false);


        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mLogFile = place.getText().toString();
                mLogSize = size.getText().toString();
                mLogNum = Integer.parseInt(number.getText().toString());
                mLogSetting.setText(mLogFile + " num " + mLogNum + " every " + mLogSize + "kb");
                Log.d(TAG, "set android log file to:" + mLogFile);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void showEventsDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.event_dialog, null);

        final EditText speed = (EditText) alertLayout.findViewById(R.id.speed_editText);
        final EditText throttle = (EditText) alertLayout.findViewById(R.id.throttle_editText);
        final EditText trackball = (EditText) alertLayout.findViewById(R.id.trackball_editText);
        final EditText nav = (EditText) alertLayout.findViewById(R.id.nav_editText);
        final EditText majornav = (EditText) alertLayout.findViewById(R.id.majornav_editText);
        final EditText anyevent = (EditText) alertLayout.findViewById(R.id.anyevent_editText);
        final EditText touch = (EditText) alertLayout.findViewById(R.id.touch_editText);
        final EditText motion = (EditText) alertLayout.findViewById(R.id.motion_editText);

        speed.setText("1000");
        throttle.setText("300");
        trackball.setText("0");
        nav.setText("0");
        majornav.setText("0");
        anyevent.setText("0");
        touch.setText("0");
        motion.setText("0");

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("event setting");
        alert.setView(alertLayout);
        alert.setCancelable(false);
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String sp = speed.getText().toString();
                String th = throttle.getText().toString();
                String tr = trackball.getText().toString();
                String na = nav.getText().toString();
                String ma = majornav.getText().toString();
                String an = anyevent.getText().toString();

                String to = touch.getText().toString();
                String mo = motion.getText().toString();

                String event = " -s " + sp + " --ptc-trackball " + tr
                    + " --ptc-touch " + to + " --ptc-motion " + mo
                    + " --ptc-nav " + na + " --ptc-majornav " + ma
                    + " --ptc-anyevent " + an + "--throttle " + th ;

                if (DEBUG) Log.d(TAG, "event: " + event);

                mEventSetting = event;
                mEventsTextView.setText(event);
                mDefaultCmd = "monkey " + mBlacklist + " " + mDebuggingSetting
                    + " " + mGeneralSetting + " " + mEventSetting + " " + mOutputSetting;
                mFullCmd.setText(mDefaultCmd);
            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }

    private static void copyFileUsingFileStreams(File source, File dest) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        } finally {
            if (input != null)
                input.close();
            if (output != null)
                output.close();
        }
    }
    public void copyBlacklist() throws IOException {
        InputStream input = getResources().openRawResource(R.raw.blacklist);
        OutputStream output = null;
        File dest = new File("/cache/blacklist.txt");
        if (dest.exists())
            return;

        try {
            output = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        } catch (IOException e) {
            //
        } finally {
            if (input != null)
                input.close();
            if (output != null)
                output.close();
        }
    }

    public int startCopyLog() throws IOException {
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        String destPath = null;

        for (int i = 0; i < volumes.length; i++) {
            if (volumes[i].isRemovable()) {
                String path = volumes[i].getPath();
                String state = mStorageManager.getVolumeState(path);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    destPath = path;
                    break;
                }
            }
        }

        if (destPath == null) {
            //Toast.makeText(MainActivity.this, R.string.noMoveStorage,
            //       Toast.LENGTH_SHORT).show();
            Log.d(TAG, "no external storage");
            return -1;
        }

        destPath = destPath + "/monkeylog";
        try {
            File f = new File(destPath);
            f.mkdir();
        } catch (Exception e) {
            Log.e(TAG, "mkdir error");
            return -1;
        }

        for (int i = 0; i < mLogNum; i++) {
            String sourcePath = mLogFile + (i > 0 ? "."+i : "");
            File source = new File(sourcePath);

            if (DEBUG) Log.d(TAG, "source file:" + source);
            if (source.exists() && !source.isDirectory()) {
                File dest = new File(destPath + "/" + source.getName());
                if (DEBUG) Log.d(TAG, "dest file:" + dest);
                copyFileUsingFileStreams(source, dest);
            } else {
                Log.d(TAG, "android log not exists");
            }
        }
        return 0;
    }

	public void onStopButtonClicked() {
        int pid = -1;
        Runtime runtime = Runtime.getRuntime();
        String prog = "ps | grep \"commands.monkey\" | busybox awk '{print $2}'";
        String[] cmds = {"sh","-c",prog};
        try {
            Process process = runtime.exec(cmds);
            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            BufferedReader buffered = new BufferedReader(reader);
            String line;
            while ((line = buffered.readLine()) != null) {
                Log.d(TAG,"read line = " + line);
                pid = Integer.parseInt(line);
            }

            int status = process.waitFor();
            if (DEBUG) Log.d(TAG, "cmd :" + prog + " returned " + status);
            if (pid != -1) {
                String kill = "kill " + pid;
                process = runtime.exec(kill);
                status = process.waitFor();
                if (DEBUG) Log.d(TAG, "cmd :" + kill + " returned " + status);
            }
        } catch (Exception e) {
            Log.d(TAG,"runtime error = " + e);
        }
    }
    public void doCopyLog() {
        new Thread() {
            public void run() {
                try {
                    startCopyLog();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}
