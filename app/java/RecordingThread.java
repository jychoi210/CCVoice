package com.example.gill.cc_voice;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class RecordingThread extends Thread {
    // 수동 녹음과 시간 예약 녹음을 구분하기 위한 상수
    public final static int MODE_TIMER = 1;
    public final static int MODE_NON_TIMER = 2;

    // 녹음시 생성되는 데이터의 형식을 지정하기 위한 변수들
    private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private final int SAMPLE_RATE = 44100; // Hz
    private final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    private final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);

    // 작업 완료 메시지를 보내기 위해 참조할 엑티비티 변수
    private Record record;

    // 녹음이 진행중인지 확인하기 위한 정적 변수
    private static boolean run = false;

    // 현재 작업이 수동 녹음인지 시간 예약 녹음인지 구분하기 위한 변수
    private int mode;

    // 녹음된 파일들을 담을 리스트
    private ArrayList<File> wavFileList;

    // 시간 예약 녹음시 지정되는 시간제한 값
    private int limitSec;

    private String ID;

    // 수동 녹음시 사용되는 생성자
    public RecordingThread(Record record, String ID, int mode) {
        this.ID = ID;
        wavFileList = new ArrayList<>();
        this.mode = mode;
        this.record = record;
    }

    // 시간 예약 녹음시 사용되는 생성자
    public RecordingThread(String ID, int limitSec, int mode) {
        this.ID = ID;
        wavFileList = new ArrayList<>();
        this.mode = mode;
        this.limitSec = limitSec;
    }

    // 녹음을 시작하기 전 현재 녹음이 진행중인지 확인하는 함수
    public synchronized boolean ready() {
        // 녹음이 진행중이라면
        if (run) {
            return false;
        } else {
            // 진행중이 아니라면
            run = true;
            return true;
        }
    }

    @Override
    public void run() {
        // 파일을 저장할 디렉토리가 존재하는지 확인하고 없다면 생성
        String filePath = Environment.getExternalStorageDirectory() + "/Voice Recorder/";
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdir();
        }

        // 녹음에서 사용할 AudioRecord 변수 선언
        AudioRecord audioRecord = null;

        // 녹음 데이터를 파일에 쓰기위한 스트림 변수
        FileOutputStream wavOut = null;

        // 녹음 데이터를 저장할 파일 변수
        File wavFile = null;

        // 날짜값을 이용하여 파일 이름 지정
        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyMMddHHmmss");
        String fileName = timeStampFormat.format(new Date()).toString();

        // 파일 개수를 담을 변수
        int count = 0;

        try {
            // 녹음을 위한 오디오 레코드 객체 생성
            audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
            // 녹음 데이터를 저장할 파일 객체 생성
            wavFile = new File(filePath + fileName + "_" + count + ".wav");
            // 생성된 객체를 리스트에 담음
            wavFileList.add(wavFile);
            // 파일 객체에 데이터를 쓰기위한 스트림 객체 생성
            wavOut = new FileOutputStream(wavFile);
            // wav 파일의 형식을 갖추기 위해 44바이트의 헤더를 정보를 작성
            writeWavHeader(wavOut, CHANNEL_MASK, SAMPLE_RATE, ENCODING);

            // 녹음 데이터를 받아올 버퍼 생성
            byte[] buffer = new byte[BUFFER_SIZE];
            // 오디오 레코드로부터 가져온 데이터의 크기를 저장하기 위한 변수
            int read;
            // wav 파일의 총 크기를 저장하기 위한 변수
            long total = wavFile.length();

            // 녹음을 시작
            audioRecord.startRecording();

            // 녹음이 진행중이라면 반복
            while (run) {
                // 버퍼에 녹음 데이터를 넣음
                read = audioRecord.read(buffer, 0, buffer.length);

                // 현재 wav 파일에 읽어온 데이터의 크기를 더했을때 시간이 1분 이상이면
                if ((wavFile.length() + read) / (SAMPLE_RATE * 2) > 59) {
                    // 녹음 데이터를 쓰며 파일의 크기가 변경됐기 때문에
                    // 헤더에서 파일 크기에 대한 정보를 업데이트 해주고
                    // 스트림을 닫는다
                    updateWavHeader(wavFile);
                    wavOut.close();

                    // 카운트를 증가
                    count++;

                    // 새로운 파일을 생성
                    wavFile = new File(filePath + fileName + "_" + count + ".wav");
                    // 파일 리스트에 파일을 추가
                    wavFileList.add(wavFile);
                    // 파일에 데이터를 쓰기위한 스트림 생성
                    wavOut = new FileOutputStream(wavFile);
                    // 새로운 파일에 헤더파일 정보 작성
                    writeWavHeader(wavOut, CHANNEL_MASK, SAMPLE_RATE, ENCODING);
                    // 현재 파일 크기를 총 크기 변수에 더함
                    total += wavFile.length();
                }

                // wav 파일의 최대 크기는 4GB
                // total + read가 4GB가 넘는다면
                // 4GB 까지만 파일에 작성하고 녹음 종료
                if (total + read > 4294967295L) {
                    for (int i = 0; i < read && total <= 4294967295L; i++, total++) {
                        wavOut.write(buffer[i]);
                    }
                    run = false;

                    // 모드가 시간 예약 모드이고 총 파일 크기에 읽어온 파일크기를 더했을때 시간이 제한 시간을 넘는다면
                    // 제한시간 만큼만 데이터를 쓰고 녹음 종료
                } else if (mode == MODE_TIMER && (total + read) / (SAMPLE_RATE * 2) > limitSec) {
                    for (int i = 0; total / (SAMPLE_RATE * 2) <= limitSec; i++) {
                        wavOut.write(buffer[i]);
                        total++;
                    }
                    run = false;
                } else {
                    // 가져온 데이터를 파일에 작성
                    wavOut.write(buffer, 0, read);
                    // 총크기에 읽어온 크기를 더함
                    total += read;
                }

                Log.d("time", String.valueOf(total / (SAMPLE_RATE * 2)));
            }

        } catch (IOException ex) {
            // 입출력예외
        } finally {
            // 오디오 레코드 객체가 NULL이 아니라면
            if (audioRecord != null) {
                try {
                    // 녹음이 진행중이라면
                    if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        // 중지시킴
                        audioRecord.stop();
                    }
                } catch (IllegalStateException ex) {
                    //
                }
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    // 오디오 레코드 객체의 상태가 초기화된 상태라면
                    // 해제시킴
                    audioRecord.release();
                }
            }

            // 파일출력 스트림이 NULL이 아니라면
            if (wavOut != null) {
                try {
                    // 스트림을 닫음
                    wavOut.close();
                } catch (IOException ex) {
                    //
                }
            }
        }

        try {
            // 마지막 녹음 파일의 헤더를 업데이트
            updateWavHeader(wavFile);
        } catch (IOException ex) {
            //
        }

        // 파일전송쓰레드
        FileTransferThread fileTransferThread;

        // 수동 녹음 모드라면
        if (mode == MODE_NON_TIMER) {
            // 녹음 완료 상태를 레코드 액티비티에 전달
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    record.sendMessage(Record.MSG_RECORDING_END);
                }
            });

            // 파일전송쓰레드 생성
            fileTransferThread = new FileTransferThread(record, ID, wavFileList);
        } else {
            fileTransferThread = new FileTransferThread(ID, wavFileList);
        }

        // 파일전송쓰레드 실행
        fileTransferThread.start();
    }

    public boolean isRun(){
        return run;
    }

    public void stopRecording() {
        run = false;
    }

    // 44바이트의 wav 파일 헤더 정보를 작성하기 위한 메소드
    private void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding) throws IOException {
        short channels;
        switch (channelMask) {
            case AudioFormat.CHANNEL_IN_MONO:
                channels = 1;
                break;
            case AudioFormat.CHANNEL_IN_STEREO:
                channels = 2;
                break;
            default:
                throw new IllegalArgumentException("Unacceptable channel mask");
        }

        short bitDepth;
        switch (encoding) {
            case AudioFormat.ENCODING_PCM_8BIT:
                bitDepth = 8;
                break;
            case AudioFormat.ENCODING_PCM_16BIT:
                bitDepth = 16;
                break;
            case AudioFormat.ENCODING_PCM_FLOAT:
                bitDepth = 32;
                break;
            default:
                throw new IllegalArgumentException("Unacceptable encoding");
        }

        byte[] littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((short) (channels * (bitDepth / 8)))
                .putShort(bitDepth)
                .array();

        out.write(new byte[]{
                // RIFF header
                'R', 'I', 'F', 'F', // ChunkID
                0, 0, 0, 0, // ChunkSize (업데이트 메소드를 통해 작성됨)
                'W', 'A', 'V', 'E', // Format
                // fmt subchunk
                'f', 'm', 't', ' ', // Subchunk1ID
                16, 0, 0, 0, // Subchunk1Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // NumChannels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                littleBytes[10], littleBytes[11], // BlockAlign
                littleBytes[12], littleBytes[13], // BitsPerSample
                // data subchunk
                'd', 'a', 't', 'a', // Subchunk2ID
                0, 0, 0, 0, // Subchunk2Size (업데이트를 메소드를 통해 작성됨)
        });
    }

    // 파일 크기 정보를 업데이트하기 위한 메소드
    private void updateWavHeader(File wav) throws IOException {
        byte[] sizes = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) (wav.length() - 8)) // ChunkSize
                .putInt((int) (wav.length() - 44)) // Subchunk2Size
                .array();

        RandomAccessFile accessWave = null;
        try {
            accessWave = new RandomAccessFile(wav, "rw");
            // ChunkSize
            accessWave.seek(4);
            accessWave.write(sizes, 0, 4);

            // Subchunk2Size
            accessWave.seek(40);
            accessWave.write(sizes, 4, 4);
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close();
                } catch (IOException ex) {
                    //
                }
            }
        }
    }
}
