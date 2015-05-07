package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Handles all file downloads.
 */
@Service
public class DownloadManager {

	private static Logger logger = Logger.getLogger(DownloadManager.class);
	
	@Value("${app.numMaxConcurrentDownloads}")
	private int numMaxConcurrentDownloads;
	
	private ExecutorService executor = null;
	
	@PostConstruct
	private void onPostConstruct() {
		executor = Executors.newFixedThreadPool(numMaxConcurrentDownloads);
	}
	
	/**
	 * Queue a download. The completionCallback will be informed when the download has completed.
	 * @param source
	 * @param destination
	 * @param completionCalback
	 */
	public void queueDownload(URL source, File destination, IHlsSegmentFileDownloadCallback completionCalback) {
		executor.execute(new Downloader(source, destination, completionCalback));
	}
	
	private class Downloader implements Runnable {

		private final URL source;
		private final File destination;
		private final IHlsSegmentFileDownloadCallback callback;
		
		public Downloader(URL source, File destination, IHlsSegmentFileDownloadCallback callback) {
			this.source = source;
			this.destination = destination;
			this.callback = callback;
		}
		
		@Override
		public void run() {
			boolean success = false;
			
			if (callback != null) {
				callback.onDownloadStart();
			}
			
			try {
				logger.debug("Attempting to download \""+source.toExternalForm()+"\" to \""+destination.getAbsolutePath()+"\".");
				ReadableByteChannel rbc = Channels.newChannel(source.openStream());
				FileOutputStream fos =  new FileOutputStream(destination);
				try {
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				}
				finally {
					fos.close();
				}
				success = true;
				logger.debug("Download completed succesfully.");
			}
			catch(Exception e) {
				e.printStackTrace();
				logger.warn("Download failed for some reason.");
			}

			if (callback != null) {
				callback.onCompletion(success);
			}
		}
		
	}
	
}