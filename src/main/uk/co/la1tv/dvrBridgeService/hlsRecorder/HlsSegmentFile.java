package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Represents a hls segment file.
 * Also handles downloading the file.
 */
@Component
@Scope("prototype")
public class HlsSegmentFile {
	
	@Autowired
	private DownloadManager downloadManager;
	
	@Value("${app.chunksBaseUrl}")
	private String baseUrlStr;
	private URL baseUrl = null;
	
	private final URL remoteUrl; // the url that this segment was located at
	private final File localFile; // the local location
	private boolean available = false; // true when the file is downloaded ready for accessing
	
	/**
	 * @param remoteUrl The url where the segment should be downloaded from.
	 * @param localFile The file where the segment should be downloaded to.
	 */
	public HlsSegmentFile(URL remoteUrl, File localFile) {
		this.remoteUrl = remoteUrl;
		this.localFile = localFile;
	}
	
	@PostConstruct
	private void onPostConstruct() {
		try {
			baseUrl = new URL(baseUrlStr);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw(new RuntimeException("Invalid chunks base url."));
		}
		downloadFile();
	}
	
	public URL getRemoteUrl() {
		return remoteUrl;
	}

	public boolean isAvailable() {
		return available;
	}
	
	public File getFile() {
		if (!available) {
			throw(new RuntimeException("This file is not available yet."));
		}
		return localFile;
	}
	
	public URL getFileUrl() {
		if (!available) {
			throw(new RuntimeException("This file is not available yet."));
		}
		try {
			return new URL(baseUrl, localFile.getName());
		} catch (MalformedURLException e) {
			// shouldn't happen!
			e.printStackTrace();
			throw(new RuntimeException("Unable to build file url."));
		}
	}
	
	/**
	 * Download the file and make it available.
	 */
	private void downloadFile() {
		downloadManager.queueDownload(remoteUrl, localFile, new FileDownloadedCallback());
	}
	
	private class FileDownloadedCallback implements IDownloadCompleteCallback {

		@Override
		public void onCompletion(boolean success) {
			if (success) {
				available = true;
			}
		}
		
	}
}