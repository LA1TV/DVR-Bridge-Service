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
	
	private final Object lock = new Object();
	
	@Value("${app.chunksBaseUrl}")
	private String baseUrlStr;
	private URL baseUrl = null;
	
	private final URL remoteUrl; // the url that this segment was located at
	private final File localFile; // the local location
	private HlsSegmentFileState state = HlsSegmentFileState.DOWNLOAD_PENDING;
	private HashSet<IHlsSegmentFileStateChangeListener> stateChangeCallbacks = new HashSet<>();

	// the number of proxies that are wanting to access to the file
	private int numProxiesAccessingFile = 0;
	
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
		synchronized(lock) {
			return state;
		}
	}
	
	public File getFile() {
		synchronized(lock) {
			if (state != HlsSegmentFileState.DOWNLOADED) {
				throw(new RuntimeException("This file is not available."));
			}
			return localFile;
		}
	}
	
	public URL getFileUrl() {
		synchronized(lock) {
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
	}
	
	public boolean registerStateChangeCallback(IHlsSegmentFileStateChangeListener callback) {
		synchronized(stateChangeCallbacks) {
			return stateChangeCallbacks.add(callback);
		}
	}
	
	public boolean unregisterStateChangeCallback(IHlsSegmentFileStateChangeListener callback) {
		synchronized(stateChangeCallbacks) {
			return stateChangeCallbacks.remove(callback);
		}
	}
	
	/**
	 * Delete the file.
	 * This should only be called from the HlsSegmentFileStore
	 */
	public boolean deleteFile() {
		synchronized(lock) {
			if (state != HlsSegmentFileState.DOWNLOADED) {
				throw(new RuntimeException("Must be in the DOWNLOADED state in order to be deleted."));
			}
			if (numProxiesAccessingFile > 0) {
				throw(new RuntimeException("There are currently proxies that haven't called release() yet."));
			}
			return localFile.delete();
		}
	}
	

	/**
	 * Called by a proxy when it is created for this file.
	 */
	public void onProxyCreated() {
		numProxiesAccessingFile++;
	}
	
	/**
	 * Called by a proxy when the release method is called on it.
	 */
	public void onProxyReleased() {
		numProxiesAccessingFile--;
	}
	
	/**
	 * get the number of proxies that are expecting access to this file at the moment.
	 * @return
	 */
	public int getNumProxiesAccessingFile() {
		return numProxiesAccessingFile;
	}
	
	private void callStateChangeCallbacks(HlsSegmentFileState newState) {
		HashSet<IHlsSegmentFileStateChangeListener> clone = null;
		synchronized(stateChangeCallbacks) {
			// need to iterate over a clone because something in the callback might call the unregister method
			// and this would cause a concurrent modification exception
			clone = new HashSet<>(stateChangeCallbacks);
		}
		for(IHlsSegmentFileStateChangeListener callback : clone) {
			callback.onStateChange(newState);
		}
	}
	
	private void updateState(HlsSegmentFileState state) {
		synchronized(lock) {
			this.state = state;
		}
		callStateChangeCallbacks(state);
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