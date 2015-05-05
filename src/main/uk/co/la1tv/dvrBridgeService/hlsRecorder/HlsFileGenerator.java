package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.co.la1tv.dvrBridgeService.helpers.FileHelper;

/**
 * Responsible for generating unique File objects for chunks to be downloaded to.
 *
 */
@Service
public class HlsFileGenerator {

	private static Logger logger = Logger.getLogger(HlsFileGenerator.class);
	
	@Value("${app.chunksDirectory}")
	private String chunksDirectoryStr;
	
	private File chunksDirectory = null;
	private MessageDigest digest = null;
	private Random random = new Random();
	final protected static char[] hexArray = "0123456789abcdef".toCharArray();
	
	@PostConstruct
	private void onPostConstruct() {
		chunksDirectory = new File(FileHelper.format(chunksDirectoryStr));
		try {
			digest = MessageDigest.getInstance("md5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw(new RuntimeException("MD5 algorithm could not be found."));
		}
		
		if (!chunksDirectory.exists()) {
			logger.info("Chunks directory does not exist. Creating it.");
			chunksDirectory.mkdir();
		}
		
		if (!chunksDirectory.canWrite()) {
			throw(new RuntimeException("Cannot write to chunks directory."));
		}
	
		// empty the folder
		FileHelper.purgeDirectory(chunksDirectory);
	}
	
	public File generateFile(String extension) {
		
		File file = null;
		do {
			String name = bytesToHex(digest.digest((System.currentTimeMillis()+":"+random.nextInt()).getBytes()));
			if (extension != null) {
				name += "."+extension;
			}
			file = new File(chunksDirectory, name);
		} while(file.exists());
		return file;
	}

	// http://stackoverflow.com/a/9855338/1048589
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

}
