package uk.co.la1tv.dvrBridgeService.helpers;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

public class RuntimeHelper {
	
	private static Logger logger = Logger.getLogger(RuntimeHelper.class);
	
	/**
	 * Execute the program passed in and return the exit code.
	 * @param cmd: The segments of the command to run.
	 * @param workingDir: The working directory the program should use. Can be null to use default.
	 * @param inputStreamHandler: Something implementing StreamMonitor to read the output stream from the program in a new thread. Null means discard stream.
	 * @param errStream: Something implementing StreamMonitor to read the error output of the program in a new thread. Null means discard stream.
	 * @return The exit code from the program being run.
	 */
	public static int executeProgram(String[] cmd, File workingDir, StreamMonitor inputStream, StreamMonitor errStream) {
		if (workingDir != null) {
			logger.trace("Executing '"+explode(cmd)+"' with working dir '"+workingDir.getAbsolutePath()+"'.");
		}
		else {
			logger.trace("Executing '"+explode(cmd)+"'.");
		}
		
		int exitVal;
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(cmd, null, workingDir);
			// consume the err stream and stdout stream from the process
			Thread inputStreamThread;
			if (inputStream != null) {
				inputStream.setStream(proc.getInputStream());
				inputStreamThread = new Thread(inputStream);
			}
			else {
				inputStreamThread = new StreamGobbler(proc.getInputStream(), StreamType.STDOUT); // the input stream is the STDOUT from the program being executed
			}
			inputStreamThread.start();
			
			Thread errStreamThread;
			if (errStream != null) {
				errStream.setStream(proc.getErrorStream());
				errStreamThread = new Thread(errStream);
			}
			else {
				errStreamThread = new StreamGobbler(proc.getErrorStream(), StreamType.ERR);
			}
			errStreamThread.start();
			
			// wait make sure stream threads have finished collecting output
			inputStreamThread.join();
			errStreamThread.join();
			exitVal = proc.waitFor();
		
		} catch (IOException e) {
			throw(new RuntimeException("Error trying to execute program."));
		} catch (InterruptedException e) {
			throw(new RuntimeException("InterruptException occured. This shouldn't happen."));
		}
		return exitVal;
	}
	
	private static String explode(String[] a) {
		String b = "";
		boolean first = true;
		for (String c : a) {
			if (!first) {
				b += " ";
			}
			else {
				first = false;
			}
			b += c;
		}
		
		return b;
	}
}
