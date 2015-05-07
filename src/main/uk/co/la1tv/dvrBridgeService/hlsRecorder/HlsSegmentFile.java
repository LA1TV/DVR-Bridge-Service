package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

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
	private HlsSegmentFileState state = HlsSegmentFileState.DOWNLOAD_PENDING;
	private HashSet<IHlsSegmentFileStateChangeCallback> stateChangeCallbacks = new HashSet<>();
	
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

	public HlsSegmentFileState getState() {
		return state;
	}
	
	public File getFile() {
		if (state != HlsSegmentFileState.DOWNLOADED) {
			throw(new RuntimeException("This file is not available."));
		}
		return localFile;
	}
	
	public URL getFileUrl() {
		if (state != HlsSegmentFileState.DOWNLOADED) {
			throw(new RuntimeException("This file is not available."));
		}
		try {
			return new URL(baseUrl, localFile.getName());
		} catch (MalformedURLException e) {
			// shouldn't happen!
			e.printStackTrace();
			throw(new RuntimeException("Unable to build file url."));
		}
	}
	
	public boolean registerStateChangeCallback(IHlsSegmentFileStateChangeCallback callback) {
		synchronized(stateChangeCallbacks) {
			return stateChangeCallbacks.add(callback);
		}
	}
	
	public boolean unregisterStateChangeCallback(IHlsSegmentFileStateChangeCallback callback) {
		synchronized(stateChangeCallbacks) {
			return stateChangeCallbacks.remove(callback);
		}
	}
	
	private void callStateChangeCallbacks() {
		HashSet<IHlsSegmentFileStateChangeCallback> clone = null;
		synchronized(stateChangeCallbacks) {
			// need to iterate over a clone because something in the callback might call the unregister method
			// and this would cause a concurrent modification exception
			clone = new HashSet<>(stateChangeCallbacks);
		}
		for(IHlsSegmentFileStateChangeCallback callback : clone) {
			callback.onStateChange(state);
		}
	}
	
	private void updateState(HlsSegmentFileState state) {
		this.state = state;
		callStateChangeCallbacks();
	}
	
	/**
	 * Download the file and make it available.
	 */
	private void downloadFile() {
		downloadManager.queueDownload(remoteUrl, localFile, new FileDownloadCallback());
	}
	
	private class FileDownloadCallback implements IHlsSegmentFileDownloadCallback {

		@Override
		public void onDownloadStart() {
			updateState(HlsSegmentFileState.DOWNLOADING);
		}
		
		@Override
		public void onCompletion(boolean success) {
			updateState(success ? HlsSegmentFileState.DOWNLOADED : HlsSegmentFileState.DOWNLOAD_FAILED);
		}
		
	}
}