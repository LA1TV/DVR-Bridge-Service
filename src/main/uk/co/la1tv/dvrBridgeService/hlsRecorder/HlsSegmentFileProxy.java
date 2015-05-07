package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.File;
import java.net.URL;
import java.util.HashSet;

/**
 * Represents a hls segment file.
 * Also handles downloading the file.
 * 
 * This is a proxy to the actual segment file object so that it can
 * provide a delete method. E.g. several proxies may point to the same
 * hls segment file object, and both must have called delete before the
 * file can actually be deleted.
 */
public class HlsSegmentFileProxy {
	
	private Object lock = new Object();
	
	private final HlsSegmentFile hlsSegmentFile;
	private boolean released = false;
	
	private HashSet<IHlsSegmentFileStateChangeCallback> callbacks = new HashSet<>();

	public HlsSegmentFileProxy(HlsSegmentFile hlsSegmentFile) {
		hlsSegmentFile.onProxyCreated();
		this.hlsSegmentFile = hlsSegmentFile;
	}
	
	public URL getRemoteUrl() {
		synchronized(lock) {
			checkReleased();
			return hlsSegmentFile.getRemoteUrl();
		}
	}

	public HlsSegmentFileState getState() {
		synchronized(lock) {
			checkReleased();
			return hlsSegmentFile.getState();
		}
	}
	
	public File getFile() {
		synchronized(lock) {
			checkReleased();
			return hlsSegmentFile.getFile();
		}
	}
	
	public URL getFileUrl() {
		synchronized(lock) {
			checkReleased();
			return hlsSegmentFile.getFileUrl();
		}
	}
	
	public boolean registerStateChangeCallback(IHlsSegmentFileStateChangeCallback callback) {
		synchronized(lock) {
			checkReleased();
			callbacks.add(callback);
			return hlsSegmentFile.registerStateChangeCallback(callback);
		}
	}
	
	public boolean unregisterStateChangeCallback(IHlsSegmentFileStateChangeCallback callback) {
		synchronized(lock) {
			checkReleased();
			callbacks.remove(callback);
			return hlsSegmentFile.unregisterStateChangeCallback(callback);
		}
	}
	
	/**
	 * Call when you no longer need access to this file.
	 */
	public void release() {
		synchronized(lock) {
			checkReleased();
			// unregister any callbacks that have been registered
			for (IHlsSegmentFileStateChangeCallback callback : callbacks) {
				hlsSegmentFile.unregisterStateChangeCallback(callback);
			}
			callbacks.clear();
			released = true;
			hlsSegmentFile.onProxyReleased();
		}
	}
	
	public boolean isReleased() {
		synchronized(lock) {
			return released;
		}
	}
	
	private void checkReleased() {
		synchronized(lock) {
			if (released) {
				throw(new RuntimeException("release() has been called."));
			}
		}
	}
}