package uk.co.la1tv.dvrBridgeService.helpers;

import org.apache.log4j.Logger;

public class FileHelper {
	
	private static Logger logger = Logger.getLogger(FileHelper.class);
	
	private FileHelper() {}
	
	/**
	 * Formats the path passed in so that it is correct for the filesystem it's running on. 
	 * @param path
	 * @return Formatted path
	 */
	public static String format(String path) {
		char sep = System.getProperty("file.separator").charAt(0);
		return path.replace(sep == '\\' ? '/' : '\\', sep);
	}
}
