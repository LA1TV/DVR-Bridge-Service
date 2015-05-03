package uk.co.la1tv.dvrBridgeService.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

/**
 * Reads an input stream and logs it. Terminates when the stream ends.
 * 
 * Also provides a method to retrieve all the output as a string.
 *
 */
public class StreamGobbler extends Thread implements StreamMonitor {
	
	private static Logger logger = Logger.getLogger(StreamGobbler.class);
	
	private InputStream is = null;
	private String output = "";
	
	@Override
	public void run() {
		
		logger.trace("StreamGobbler started.");
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		
		String line = null;
		try {
			while((line = br.readLine()) != null) {
				logger.trace(line);
				output += line+"\n";
			}
		} catch (IOException e) {
			throw(new RuntimeException("Error occured when trying to read stream."));
		}
		logger.trace("StreamGobbler finished.");
	}

	@Override
	public void setStream(InputStream stream) {
		is = stream;
	}
	
	/**
	 * Get all the output that has occurred (so far).
	 * @return
	 */
	public String getOutput() {
		return output;
	}
}
