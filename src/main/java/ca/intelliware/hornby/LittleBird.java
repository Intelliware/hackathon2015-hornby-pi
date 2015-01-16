package ca.intelliware.hornby;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;


public class LittleBird {
	private static final String DEFAULT_HEIGHT = "240";
	private static final String DEFAULT_WIDTH = "320";
	private static final String COMPARE = "compare %s %s -compose src -fuzz 10%% %s";
	private static final String TAKE_A_PICTURE = "raspistill -t 1 -o %s -w %s -h %s";
	
	private static Runtime runtime = Runtime.getRuntime();
	
	public static void main(String[] args) throws Exception {
		System.out.println("In flight...");
		
		String[] safeArgs = Arrays.copyOf(args, 2);
		
		String width =  safeArgs[0] == null ? DEFAULT_WIDTH : safeArgs[0];
		String height = safeArgs[1] == null ? DEFAULT_HEIGHT : safeArgs[1];
		
		File prevFile = null;
		File jpgFile = createNewTempFile();
		File diffFile = createNewTempFile();
		takePicture(jpgFile, width, height);
		
//		long start;
//		long end;
		while (true) {
			prevFile = jpgFile;
			
			jpgFile = createNewTempFile();

//			start = getTime();
			
			takePicture(jpgFile, width, height);
			
//			end = getTime();
			
//			System.out.println("pic: " + (end - start));
		
//			start = getTime();
			
			runDiff(prevFile, jpgFile, diffFile);
			
//			end = getTime();
			
//			System.out.println("diff: " + (end - start));

			long diffSize = Files.size(Paths.get(diffFile.getAbsolutePath()));
			System.out.println(diffSize);
			
			prevFile.delete();
		}
	}

//	private static long getTime() {
//		return new Date().getTime();
//	}

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
