package com.example.gill.cc_voice;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;


public class Record extends Activity implements View.OnClickListener {
    private static final int PERMISSION_RECORD_AUDIO = 0;
    private static final int PERMISSION_TIME_SETTING = 1;
    public static final int MSG_RECORDING_END = 0;
    public static final int MSG_TRANSFER_END = 1;

    private RecordingThread recordingThread;
    private String ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record);

        // 인텐트에서 아이디 값을 얻어옴
        ID = getIntent().getStringExtra("id");

        // 각 버튼들의 클릭 리스너 등록
        findViewById(R.id.btn_record).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
        findViewById(R.id.btn_set_timer).setOnClickListener(this);

        // 등록된 알람이 현재 계정 아이디에서 등록한 알람인지 확인
        Intent recordingServiceIntent = new Intent(this, RecordingService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, recordingServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // 등록된 알람이 있다면
        if (pendingIntent != null) {
            SharedPreferences sp = getSharedPreferences("time", MODE_PRIVATE);
            String timeSettingID = sp.getString("id", "");

            // 로그인한 아이디와 알람 등록시 사용한 아이디가 일치하지 않다면 알람을 해제
            if (!timeSettingID.equals(ID)) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.btn_record: // 녹음버튼 클릭 이벤트
                // 녹음 작업에 필요한 권한이 있는지 확인
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO);
                    }
                }

                startRecording();
                break;
            case R.id.btn_stop: // 정지버튼 클릭 이벤트
                // 수동 녹음이 진행중이라면
                if (recordingThread != null && recordingThread.isRun()) {
                    // 녹음쓰레드의 정지 메소드를 호출
                    recordingThread.stopRecording();
                    recordingThread = null;
                } else {
                    // 녹음중이 아니라면
                    Toast.makeText(this, "현재 녹음중이 아닙니다.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_set_timer: // 시간설정버튼 클릭 이벤트
                // 녹음 작업에 필요한 권한이 있는지 확인
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, PERMISSION_TIME_SETTING);
                    }
                }

                startTimeSetting();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_RECORD_AUDIO || requestCode == PERMISSION_TIME_SETTING) {
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_DENIED) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                        alertDialog.setTitle("권한 알림");
                        alertDialog.setMessage("녹음 기능은 저장소, 녹음 권한이 필요합니다.");

                        alertDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        alertDialog.setNegativeButton("권한 설정", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent appSettingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                appSettingIntent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(appSettingIntent);
                            }
                        });

                        alertDialog.setCancelable(false);
                        alertDialog.show();

                        return;
                    }
                }

                if (requestCode == PERMISSION_RECORD_AUDIO) {
                    startRecording();
                } else {
                    startTimeSetting();
                }
            }
        }
    }

    private void startRecording() {
        // 쓰레드 변수가 null 이라면 녹음 쓰레드 객체 생성
        if (recordingThread == null) {
            recordingThread = new RecordingThread(this, ID, RecordingThread.MODE_NON_TIMER);
        }

        // 쓰레드를 준비하고 녹음이 가능한 상태라면
        if (recordingThread.ready()) {
            Toast.makeText(this, "녹음을 시작합니다.", Toast.LENGTH_SHORT).show();
            recordingThread.start();
        } else {
            // 녹음이 가능한 상태가 아니라면
            // 시간 예약 녹음이 진행중
            Toast.makeText(this, "이미 녹음이 진행중입니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 시간 설정 액티비티 실행
    private void startTimeSetting() {
        Intent timeSettingIntent = new Intent(Record.this, TimeSettingActivity.class);
        timeSettingIntent.putExtra("id", ID);
        startActivity(timeSettingIntent);
    }

    // 각 쓰레드에서 완료 메시지를 보내는 메소드
    public void sendMessage(int msgCode) {
        if (msgCode == MSG_RECORDING_END) {
            Toast.makeText(this, "녹음이 완료되었습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "파일 전송이 완료되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
