package su.rumishistem.ffmpegui.Module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import su.rumishistem.ffmpegui.Type.VideoProbe;

public class FFProbe {
	public static VideoProbe show(String path) {
		try {
			ProcessBuilder pb = new ProcessBuilder("/usr/bin/ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams", path);
			Process p = pb.start();

			StringBuilder sb = new StringBuilder();

			InputStream is = p.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}

			int code = p.waitFor();
			if (code != 0) {
				throw new RuntimeException("FFProbe Error");
			}

			JsonNode data = new ObjectMapper().readTree(sb.toString());

			VideoProbe probe = new VideoProbe();
			probe.duration = data.get("format").get("duration").asDouble();
			return probe;
		} catch (IOException ex) {
			return null;
		} catch (InterruptedException ex) {
			return null;
		}
	}
}
