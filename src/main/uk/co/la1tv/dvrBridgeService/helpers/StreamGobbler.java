package uk.co.la1tv.dvrBridgeService.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

/**
 * Reads an input stream and logs it. Terminates when the stream ends.
 *
 */
public class StreamGobbler extends Thread {
	
	private static Logger logger = Logger.getLogger(StreamGobbler.class);
	
	private InputStream is;
	private StreamType type;

	public StreamGobbler(InputStream is, StreamType type) {
		this.is = is;
		this.type = type;
	}
	
	@Override
	public void run() {
		
		logger.trace("StreamGobbler started.");
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		
		String line = null;
		try {
			while((line = br.readLine()) != null) {
				logger.trace(type.name()+": "+line);
			}
		} catch (IOException e) {
			throw(new RuntimeException("Error occured when trying to read stream."));
		}
		logger.trace("StreamGobbler finished.");
	}
}
