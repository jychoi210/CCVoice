import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

public class DataReceivingThread extends Thread {

    // 클라이언트가 보낸 데이터가 담기는 인풋 스트림 변수
    InputStream in;

    public DataReceivingThread(InputStream in) {
        this.in = in;
    }

    public void run() {
        try {
            DataInputStream din = new DataInputStream(in);
            FileOutputStream out = null;

            int totalFileCount = din.readInt(); // 클라이언트로부터 전송될 파일의 총 개수를 얻어옴
            System.out.println("\nDataReceivingThread Message - 총 파일 개수: " + totalFileCount);
            
            String id = din.readUTF();
            System.out.println("\n아이디: " + id);

            // 총 파일 개수만큼 반복
            for (int i = 0; i < totalFileCount; i++) {
                byte[] buffer = new byte[102400]; // 데이터를 담을 버퍼 생성
                
                String fileName = din.readUTF(); // 파일 이름을 얻어옴
                //String filePath = "/home/yoon/customer_list/a/" + file_name;
                fileName = fileName.substring(0,fileName.length()-3);
                String filePath = "/home/yoon/customer_list/" + id + "/" + fileName + "wav";
                out = new FileOutputStream(filePath); // 얻어온 파일 이름으로 파일을 생성

                long fileSize = din.readLong(); // 파일 크기를 얻어옴
                System.out.print("DataReceivingThread Message - [" + (i + 1) + "] " + fileName + "  " + fileSize + "B\t");

                int read;

                // 파일 크기가 0보다 크면 반복
                while (fileSize > 0) {
                    // 버퍼의 크기가 남은 파일 크기보다 크다면

                    if (buffer.length > fileSize) {
                        buffer = null;
                        buffer = new byte[(int) fileSize]; // 파일 크기만큼의 버퍼를 다시 생성
                    }
                    read = in.read(buffer); // 버퍼에 데이터를 채우고
                    out.write(buffer, 0, read); // 버퍼에 있는 데이터를 파일에 입력
                    fileSize -= read; // 파일 크기를 읽어온 크기만큼 감소
                }

                System.out.println("[완료]");
                
                recognitionSpeech(filePath, id, fileName);

                out.flush();
                out.close();
            }
            in.close();
            din.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void recognitionSpeech(String filePath, String id, String file_name) {
        
        try {
            SpeechClient speech = SpeechClient.create(); // Client 생성
            
            // 오디오 파일에 대한 설정부분
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(44100)
                    .setLanguageCode("ko-KR")
                    .build();

            RecognitionAudio audio = getRecognitionAudio(filePath); // Audio 파일에 대한 RecognitionAudio 인스턴스 생성
            RecognizeResponse response = speech.recognize(config, audio); // 요청에 대한 응답
            List<SpeechRecognitionResult> results = response.getResultsList(); // 응답 결과들
            
            /*File file = new File(filePath+".txt");
            FileOutputStream fos = new FileOutputStream(file);
            PrintStream ps = new PrintStream(fos);
            System.setOut(ps);*/
            
            //System.out.println(ps)
            
            for (SpeechRecognitionResult result: results) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                //System.out.printf("%s%n", alternative.getTranscript());
                
                try {
                    String message = alternative.getTranscript();
                    
                    //File file = new File("/home/yoon/customer_list/a/" + file_name + ".txt");
                    //String str = "01011112222"; System.out.println(filename.substring(0, str.length()-3));
                    //file_name = file_name.substring(0,file_name.length()-3);
                    File file = new File("/home/yoon/customer_list/" + id + "/" + file_name + "txt");
                    FileWriter writer = null;
                    
                    // 기존 파일의 내용에 이어서 쓰려면 true를, 기존 내용을 없애고 새로 쓰려면 false를 지정한다.
                    writer = new FileWriter(file, true);
                    writer.write(message);
                    writer.flush();
                    
                } catch(IOException e) {
                    e.printStackTrace();
                    } 
                }
            System.out.println("txt파일 완료");
            speech.close();
            }catch (Exception e) {
                e.printStackTrace();
                }
        }
    
    // RecognitionAudio 만들어 주는 부분
    public static RecognitionAudio getRecognitionAudio(String filePath) throws IOException {
        RecognitionAudio recognitionAudio;

        // 파일이 GCS에 있는 경우
        if (filePath.startsWith("gs://")) {
            recognitionAudio = RecognitionAudio.newBuilder()
                    .setUri(filePath)
                    .build();
        }
        else { // 파일이 로컬에 있는 경우
            Path path = Paths.get(filePath);
            byte[] data = Files.readAllBytes(path);
            ByteString audioBytes = ByteString.copyFrom(data);

            recognitionAudio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();
        }
        return recognitionAudio;   
    }
} 
