package com.example.gill.cc_voice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

// 설정된 시간에 녹음을 진행하기 위한 서비스 클래스
public class RecordingService extends Service {

    public RecordingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // SharedPreferences "time" 항목을 불러옴
        SharedPreferences sp = getSharedPreferences("time", MODE_PRIVATE);

        // 저장된 값들을 가져옴
        int endHour = sp.getInt("endHour", 0);
        int endMin = sp.getInt("endMin", 0);

        // 시간과 분을 초로 변경
        int limitSec = endHour * 3600 + endMin * 60;

        // 안드로이드 SDK KITKAT(19) 버전 이상인 경우
        // 알람 반복을 수동으로 해줘야함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SharedPreferences.Editor spEditor = sp.edit();

            // 알람에 지정한 밀리세컨드 값을 불러와서 하루를 더하고 해당 값을 다시 저장
            long triggerAtMillis = sp.getLong("triggerAtMillis", 0);
            triggerAtMillis += 86400000;
            spEditor.putLong("triggerAtMillis", triggerAtMillis);
            spEditor.commit();

            // 등록된 알람을 취소시키고 하루를 더한 시간으로 다시 등록
            Intent newIntent = new Intent(getApplicationContext(), RecordingService.class);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();

            pendingIntent = PendingIntent.getService(getApplicationContext(), 0, newIntent, 0);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }

        // 녹음쓰레드를 생성하고 녹음이 가능한 상태라면 쓰레드를 실행
        RecordingThread recordingThread = new RecordingThread(sp.getString("id", ""), limitSec, RecordingThread.MODE_TIMER);
        if(recordingThread.ready()){
            recordingThread.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
