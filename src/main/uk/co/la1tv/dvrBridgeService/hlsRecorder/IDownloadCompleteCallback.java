package uk.co.la1tv.dvrBridgeService.hlsRecorder;

public interface IDownloadCompleteCallback {
	
	/**
	 * Called when a download completes.
	 * @param success True if the download succeeded.
	 */
	void onCompletion(boolean success);
}
