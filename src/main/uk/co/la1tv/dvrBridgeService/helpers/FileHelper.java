package uk.co.la1tv.dvrBridgeService.helpers;

import java.io.File;
import java.util.Arrays;

public class FileHelper {
	
	/**
	 * Formats the path passed in so that it is correct for the filesystem it's running on. 
	 * @param path
	 * @return Formatted path
	 */
	public static String format(String path) {
		char sep = System.getProperty("file.separator").charAt(0);
		return path.replace(sep == '\\' ? '/' : '\\', sep);
	}
	
	/**
	 * Return the extension from the file name if there is one or null otherwise.
	 * @param filename
	 * @return
	 */
	public static String getExtension(String filename) {
		String[] parts = filename.split("\\.");
		if (parts.length == 0) {
			return null;
		}
		return parts[parts.length-1];
	}
	
	public static void purgeDirectory(File dir, String[] extensions) {
	    for (File file: dir.listFiles()) {
	        if (file.isDirectory()) purgeDirectory(file, extensions);
	        if (Arrays.asList(extensions).contains(getExtension(file.getName()))) {
	        	file.delete();
	        }
	    }
	}
}
