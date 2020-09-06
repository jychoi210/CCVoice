import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

public class record {
    
    public static void main(String[] args) throws IOException { // 메인함수, IOException
        
        ServerSocket serverSocket = new ServerSocket(11001); // 서버소켓 생성

        while(true) {
            System.out.println("Main Thread Message - 새로운 연결 대기중...");
            Socket clientSocket = serverSocket.accept(); // 클라이언트 연결을 기다림
            
            System.out.println("Main Thread Message - 클라이언트와 연결 성공!");
            
            // 데이터수신쓰레드 생성 및 실행
            DataReceivingThread dataReceivingThread = new DataReceivingThread(clientSocket.getInputStream());
            dataReceivingThread.start();
        }
    }    
}
