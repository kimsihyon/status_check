package check.demo.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class FfprobeChecker {

    public enum StreamStatus {
        OK, TIMEOUT, NOT_FOUND, ERROR, DOWN
    }

    public StreamStatus check(String url) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "ffprobe",
                    "-v", "debug",
                    "-show_streams",
                    "-print_format", "json",
                    url
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            String result = output.toString().toLowerCase();

            if (result.contains("could not find codec parameters") || result.contains("404 not found")) {
                return StreamStatus.NOT_FOUND;
            } else if (result.contains("max delay reached") || result.contains("timeout") || result.contains("missed")) {
                return StreamStatus.TIMEOUT;
            } else if (exitCode != 0 || result.contains("error")) {
                return StreamStatus.ERROR;
            } else {
                return StreamStatus.OK;
            }

        } catch (Exception e) {
            return StreamStatus.DOWN;
        }
    }
}