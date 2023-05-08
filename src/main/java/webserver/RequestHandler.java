package webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private final Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        logger.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {

            /**
             * Request 처리
             * */

            BufferedReader inputReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            // Request Line을 읽는다
            String requestLine = inputReader.readLine();
            logger.debug("request line : {}", requestLine);
            String[] requestHeader = requestLine.split(" ");

            // TODO 메서드 추출 후 처리
            String method = requestHeader[0];

            // Request Header를 읽고 로그를 출력한다
            while (!requestLine.equals("")) {
                requestLine = inputReader.readLine();
                logger.debug("header : {}", requestLine);
            }

            /**
             * Response 처리
             * */
            // Request Line에서 요청된 파일의 경로, 확장자, 컨텐트 타입을 추출한다
            String URI = requestHeader[1];
            String extension = URI.substring(URI.lastIndexOf(".") + 1);
            MediaType mediaType; // enum
            String path = "";
            String contentType = "";

            for (MediaType type : MediaType.values()) {
                if (type.getExtension().equals(extension)) {
                    mediaType = type;
                    contentType = mediaType.getContentType();
                    path = mediaType.getPath() + URI;
                    break;
                }
            }

            // 경로에 해당하는 파일을 읽는다
            // TODO toPath()가 꼭 필요할지 고민해보기
            byte[] body = Files.readAllBytes(new File(path).toPath());

            // 요청에 대한 Request Message를 전송한다
            DataOutputStream dos = new DataOutputStream(out);
            response200Header(dos, body.length, contentType);
            responseBody(dos, body);

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String contentType) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: " + contentType + ";charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            // TODO toString()
            logger.info(body.toString());
            dos.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
