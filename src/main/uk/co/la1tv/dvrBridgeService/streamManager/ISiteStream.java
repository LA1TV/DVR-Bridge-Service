package uk.co.la1tv.dvrBridgeService.streamManager;

import java.net.URL;

public interface ISiteStream {
	public void setCaptureRemovedListener(ISiteStreamCaptureRemovedListener captureRemovedListener);
	public boolean hasCapture();
	public boolean captureDeleted();
	public void registerActivity();
	public long getSiteStreamId();
	public boolean startCapture();
	public boolean stopCapture();
	public boolean removeCapture();
	public URL getPlaylistUrl();
}
