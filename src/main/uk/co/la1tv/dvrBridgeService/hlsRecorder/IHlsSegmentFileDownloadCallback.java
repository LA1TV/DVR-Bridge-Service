package uk.co.la1tv.dvrBridgeService.hlsRecorder;

public interface IHlsSegmentFileDownloadCallback {
	
	/**
	 * Called when a download starts.
	 */
	void onDownloadStart();
	
	/**
	 * Called when a download completes.
	 * @param success True if the download succeeded.
	 */
	void onCompletion(boolean success);
}
