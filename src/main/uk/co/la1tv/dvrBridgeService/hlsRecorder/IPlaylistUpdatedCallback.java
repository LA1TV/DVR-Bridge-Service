package uk.co.la1tv.dvrBridgeService.hlsRecorder;

public interface IPlaylistUpdatedCallback {

	/**
	 * Called when the playlist has changed.
	 */
	void onPlaylistUpdated(HlsPlaylistCapture source, String playlistContent);
}
