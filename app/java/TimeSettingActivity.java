package com.example.gill.cc_voice;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

public class TimeSettingActivity extends Activity {

    // 레이아웃 뷰 변수들
    private RelativeLayout layoutSettingInfo;
    private TimePicker timePickerStart, timePickerEnd;
    private TextView textSettingInfo;

    // 알람 등록에 사용될 변수들
    private Intent intent, intent_id;
    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;

    // 시간값 변수들
    private int startHour, startMin, endHour, endMin;
    private String ID;

    // 값 저장을 위한 변수
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timesetting);

        ID = getIntent().getStringExtra("id");

        // SharedPreferences "time" 항목을 오픈
        sp = getSharedPreferences("time", MODE_PRIVATE);

        // 알람매니저 객체 생성
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // 타임피커 변수와 뷰를 바인딩
        timePickerStart = findViewById(R.id.time_picker_start);
        timePickerEnd = findViewById(R.id.time_picker_end);
        timePickerEnd.setIs24HourView(true);

        // 타임피커 표시 값 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePickerEnd.setHour(0);
            timePickerEnd.setMinute(1);
        } else {
            timePickerEnd.setCurrentHour(0);
            timePickerEnd.setCurrentMinute(1);
        }

        // 변수와 뷰 바인딩
        layoutSettingInfo = findViewById(R.id.layout_setting_info);
        textSettingInfo = findViewById(R.id.text_setting_info);

        // 해제 버튼 클릭 이벤트
        findViewById(R.id.btn_release).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 등록된 알람을 가져와서 취소
                pendingIntent = PendingIntent.getService(TimeSettingActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();

                // 레이아웃 뷰 속성을 변경하여 사라지게함
                layoutSettingInfo.setVisibility(View.GONE);
            }
        });

        // 등록된 알람이 있는지 확인
        intent = new Intent(this, RecordingService.class);
        pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_NO_CREATE);

        // 등록된 알람이 있다면
        if (pendingIntent != null) {

            // SharedPreferences 에서 저장된 시간 값을 가져옴
            startHour = sp.getInt("startHour", 0);
            startMin = sp.getInt("startMin", 0);
            endHour = sp.getInt("endHour", 0);
            endMin = sp.getInt("endMin", 0);

            // 레이아웃을 꾸미는 메소드 호출
            setSettingLayout();
        }

        // 확인 버튼 클릭 이벤트
        findViewById(R.id.setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 타임피커에서 선택한 값을 가져옴
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    startHour = timePickerStart.getHour();
                    startMin = timePickerStart.getMinute();

                    endHour = timePickerEnd.getHour();
                    endMin = timePickerEnd.getMinute();

                } else {
                    startHour = timePickerStart.getCurrentHour();
                    startMin = timePickerStart.getCurrentMinute();

                    endHour = timePickerEnd.getCurrentHour();
                    endMin = timePickerEnd.getCurrentMinute();
                }

                // 타임피커에서 선택한 시간을 밀리세컨드 형식으로 바꾸기 위한 작업
                Calendar startTime = Calendar.getInstance();
                long currentMillis = startTime.getTimeInMillis();

                startTime.set(Calendar.HOUR_OF_DAY, startHour);
                startTime.set(Calendar.MINUTE, startMin);
                startTime.set(Calendar.SECOND, 0);
                long timePickerMillis = startTime.getTimeInMillis();

                // 선택한 시작 시간이 현재 시간보다 작거나 같다면 하루를 더해줌
                if(timePickerMillis <= currentMillis){
                    timePickerMillis += 86400000;
                }

                // SharedPreferences에 값을 저장
                SharedPreferences.Editor spEditor = sp.edit();
                spEditor.putLong("triggerAtMillis", timePickerMillis);
                spEditor.putInt("startHour", startHour);
                spEditor.putInt("startMin", startMin);
                spEditor.putInt("endHour", endHour);
                spEditor.putInt("endMin", endMin);
                spEditor.putString("id", ID);

                spEditor.commit();

                // 위에서 얻어온 값으로 레이아웃을 꾸며줌
                setSettingLayout();

                // 설정된 시간으로 알람 등록
                Intent intent = new Intent(TimeSettingActivity.this, RecordingService.class);
                PendingIntent alarmIntent = PendingIntent.getService(TimeSettingActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, startTime.getTimeInMillis(), alarmIntent);
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime.getTimeInMillis(), alarmIntent);
                }
            }
        });
    }

    private void setSettingLayout() {
        layoutSettingInfo.setVisibility(View.VISIBLE);

        String strSetting = startHour + "시 " + startMin + "분 부터 " + endHour + "시간 " + endMin + "분 동안";
        textSettingInfo.setText(strSetting);
    }
}
