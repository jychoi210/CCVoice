package com.example.gill.cc_voice;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

// 파일 전송 쓰레드
public class FileTransferThread extends Thread {

    // 전송할 파일을 담는 리스트
    private ArrayList<File> fileList;

    // 수동 녹음 파일 전송시 완료 상태를 알리기 위해 참조할 액티비티 변수
    private Record record = null;

    //받아온 아이디 값
    private String ID;

    public FileTransferThread(Record record, String ID, ArrayList<File> fileList) {
        this.record = record;
        this.ID = ID;
        this.fileList = fileList;
    }

    public FileTransferThread(String ID, ArrayList<File> fileList) {
        this.ID = ID;
        this.fileList = fileList;
    }

    @Override
    public void run() {
        try {
            // 소켓 생성
            //192.168.21.191
            Socket socket = new Socket("172.30.1.2", 11001);

            // 데이터 전송을 위한 스트림을 생성
            OutputStream os = socket.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(os);
            FileInputStream inFs;

            // 파일 개수를 보냄
            dos.writeInt(fileList.size());
            dos.writeUTF(ID);

            // 리스트에 담긴 파일을 하나씩 반복
            for (File file : fileList) {
                // 파일 이름과 사이즈 전송
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());

                // 현재 파일의 인풋 스트림 생성
                inFs = new FileInputStream(file);

                // 파일 데이터를 담을 버퍼 생성
                byte[] txt = new byte[102400];
                int read;

                // 데이터를 읽어 버퍼에 담고 읽은 크기가 0보다 크다면 반복
                while ((read = inFs.read(txt)) > 0) {
                    // 서버에 버퍼 값을 전송
                    bos.write(txt, 0, read);
                    bos.flush();
                }
                inFs.close();
            }

            dos.close();
            bos.close();
            os.close();

            // 액티비티 변수가 null이 아니라면(쓰레드를 생성한 곳이 메인액티비티라면)
            if (record != null) {
                // 핸들러를 통해 완료 메시지 전송
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        record.sendMessage(Record.MSG_TRANSFER_END);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
