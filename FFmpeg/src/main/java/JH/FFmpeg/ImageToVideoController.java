package JH.FFmpeg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImageToVideoController {

  // FFmpeg 실행 파일 경로
  @Value("${ffmpeg.path}")
  private String ffmpegPath;
  // 비디오 파일이 저장될 경로
  @Value("${video.outputPath}")
  private String videoOutputPath;

  @PostMapping("/convert")
  public void convertToVideo(@RequestBody List<String> imageUrls) throws IOException, InterruptedException {
    // 이미지를 비디오로 변환하는 FFmpeg 명령어 생성
    StringBuilder ffmpegCommandBuilder = new StringBuilder();
    ffmpegCommandBuilder.append(
            ffmpegPath) // FFmpeg 실행 파일 경로
        .append(" -framerate 30"); // 초당 프레임 수 설정
    for (String imageUrl : imageUrls) {
      ffmpegCommandBuilder.append(" -i ").append(imageUrl); // 각각의 이미지 사용 설정
    }
    ffmpegCommandBuilder.append(" -filter_complex \""); // 여러개의 이미지를 하나로 결합하는 필터 사용
    for (int i = 0; i < imageUrls.size(); i++) {
      ffmpegCommandBuilder.append("[").append(i).append(":v]"); // index 번째 이미지 필터 설정
      ffmpegCommandBuilder.append("loop=60"); // loop/framerate 초 만큼 각 이미지가 재생된다
      ffmpegCommandBuilder.append(":size=1"); // 출력 프레임 크기 설정 - 설정한 사이즈 그대로 출력
      ffmpegCommandBuilder.append(":start=0"); // 반복 시작 시점 설정 - 0초부터 시작
      ffmpegCommandBuilder.append(",scale=1024:1024"); // 이미지 크기 설정 - 반드시 짝수로 설정해야함
      ffmpegCommandBuilder.append(",setsar=1"); // 픽셀 샘플링 화면 비율 설정 - 1:1로 설정
      ffmpegCommandBuilder.append("[v").append(i).append("]; "); // index 번째 이미지 필터 설정
    }
    for (int i = 0; i < imageUrls.size(); i++) {
      ffmpegCommandBuilder.append("[v").append(i).append("]"); // 사용할 이미지 필터 명시
    }
    ffmpegCommandBuilder.append("concat=n=") // 이미지 필터 결합
        .append(imageUrls.size()) // 이미지 개수 설정
        .append(":v=1") // 비디오로 출력 설정
        .append(":a=0") // 오디오로 출력 미설정
        .append("[out]\"") // 이미지 결합 설정
        .append(" -map \"[out]\"") // 이미지 결합 사용 설정
        .append(" -y ") // 저장 시 자동 덮어쓰기 설정
        .append(videoOutputPath); // 저장 경로 설정

    String ffmpegCommand = ffmpegCommandBuilder.toString(); // FFmpeg 명령어 완성
    System.out.println(ffmpegCommand);

    executeCommand(ffmpegCommand); // 명령어를 시스템 커맨드로 실행
  }

  private void executeCommand(String command) throws IOException, InterruptedException {
    // 프로세스 빌더 인스턴스 생성
    ProcessBuilder processBuilder = new ProcessBuilder();
    // 명령어 실행을 위한 cmd.exe 호출 및 명령어 전달
    processBuilder.command("cmd.exe", "/c", command);
    // 에러 스트림을 표준 출력 스트림으로 리다이렉트
    processBuilder.redirectErrorStream(true);

    // 프로세스 실행
    Process process = processBuilder.start();

    // 명령어 실행 결과를 읽어오기 위한 BufferedReader 인스턴스 생성
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    // 명령어 실행 결과를 한 줄씩 읽어와서 콘솔에 출력
    while ((line = reader.readLine()) != null) {
      System.out.println(line);
    }

    // 에러 스트림을 읽어서 로그에 추가 정보 기록
    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String errorLine;
    while ((errorLine = errorReader.readLine()) != null) {
      System.out.println(errorLine);
    }

    // 프로세스의 종료를 대기하고 종료 코드 출력
    int exitCode = process.waitFor();
    System.out.println("Exited with error code: " + exitCode);
  }
}
