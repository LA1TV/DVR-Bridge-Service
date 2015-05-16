package uk.co.la1tv.dvrBridgeService.servableFiles;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import uk.co.la1tv.dvrBridgeService.helpers.FileHelper;

/**
 * Responsible for generating unique File objects for chunks to be downloaded to.
 *
 */
@Service
public class ServableFileGenerator {

	private static Logger logger = Logger.getLogger(ServableFileGenerator.class);
	
	@Value("${app.webDirectory}")
	private String webDirectoryStr;
	
	@Autowired
	private ApplicationContext context;
	
	private File webDirectory = null;
	private MessageDigest digest = null;
	private Random random = new Random();
	final protected static char[] hexArray = "0123456789abcdef".toCharArray();
	
	@PostConstruct
	private void onPostConstruct() {
		webDirectory = new File(FileHelper.format(webDirectoryStr));
		try {
			digest = MessageDigest.getInstance("md5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw(new RuntimeException("MD5 algorithm could not be found."));
		}
		
		if (!webDirectory.exists()) {
			logger.info("Web directory does not exist. Creating it.");
			webDirectory.mkdir();
		}
		
		if (!webDirectory.canWrite()) {
			throw(new RuntimeException("Cannot write to web directory."));
		}
	
		// empty the folder of all .ts and .m3u8 files
		FileHelper.purgeDirectory(webDirectory, new String[]{"ts", "m3u8"});
	}
	
	public ServableFile generateServableFile(String extension) {
		
		ServableFile file = null;
		do {
			String name = bytesToHex(digest.digest((System.currentTimeMillis()+":"+random.nextInt()).getBytes()));
			if (extension != null) {
				name += "."+extension;
			}
			file = context.getBean(ServableFile.class, webDirectory, name);
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
