package ca.intelliware.hornby;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;


public class LittleBird {
	private static final String JSON_TEMPLATE = "{ \"uid\": \"%s\", \"video\": { \"data\": %s }, \"audio\": {} }";
	private static final String DEFAULT_WEB_SERVICE_ENDPOINT = "http://192.168.0.224:9000/api/measurements";
	private static final String DEFAULT_HEIGHT = "240";
	private static final String DEFAULT_WIDTH = "320";
	private static final String COMPARE = "compare %s %s -compose src -fuzz 10%% %s";
	private static final String TAKE_A_PICTURE = "raspistill -t 1 -o %s -w %s -h %s";
	
	private static Runtime runtime = Runtime.getRuntime();
	private static UUID uuid = UUID.randomUUID();
	
	public static void main(String[] args) throws Exception {
		System.out.println("In flight...");
		
		String[] safeArgs = Arrays.copyOf(args, 4);
		
		String width =  safeArgs[0] == null ? DEFAULT_WIDTH : safeArgs[0];
		String height = safeArgs[1] == null ? DEFAULT_HEIGHT : safeArgs[1];
		String endpoint = safeArgs[2] == null ? DEFAULT_WEB_SERVICE_ENDPOINT : safeArgs[2];
		String uidOverride = safeArgs[3] == null ? uuid.toString() : safeArgs[3];
		
		File prevFile = null;
		File jpgFile = createNewTempFile();
		File diffFile = createNewTempFile();
		takePicture(jpgFile, width, height);
		
		endpoint += "/" + uidOverride;
		URI uri = new URI(endpoint);
		HttpClient httpClient = HttpClients.createDefault();
		while (true) {
			prevFile = jpgFile;
			
			jpgFile = createNewTempFile();
			
			takePicture(jpgFile, width, height);
			
			runDiff(prevFile, jpgFile, diffFile);
			
			long diffSize = Files.size(Paths.get(diffFile.getAbsolutePath()));
			
			String json = String.format(JSON_TEMPLATE, uidOverride, diffSize);
			HttpUriRequest request = RequestBuilder.put()
				.setUri(uri)
				.setEntity(new StringEntity(json, ContentType.create("application/json", "UTF-8")))
				.build();
			CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request);
			response.close();
		}
	}

	private static void runDiff(File prevFile, File jpgFile, File diffFile) throws IOException, InterruptedException {
		Process process = runtime.exec(String.format(COMPARE, prevFile.getAbsolutePath(), jpgFile.getAbsolutePath(), diffFile.getAbsolutePath()));
		process.waitFor();
	}

	private static void takePicture(File jpgFile, String width, String height) throws IOException, InterruptedException {
		Process jpgProcess = runtime.exec(String.format(TAKE_A_PICTURE, jpgFile.getAbsolutePath(), width, height));
		jpgProcess.waitFor();
	}

	private static File createNewTempFile() throws IOException {
		File result = File.createTempFile("seed", ".jpg");
		result.deleteOnExit();
		return result;
	}
}
