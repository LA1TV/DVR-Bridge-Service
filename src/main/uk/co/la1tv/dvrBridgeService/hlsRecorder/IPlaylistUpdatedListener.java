package uk.co.la1tv.dvrBridgeService.hlsRecorder;

public interface IPlaylistUpdatedListener {

	/**
	 * Called when the generated playlist content changes.
	 * This is called in a separate thread. The playlist content is passed in
	 * as a parameter and this is the content as it it was when the callback
	 * was triggered. It is possible for the playlist content to have changed
	 * between this method getting called and after the callback thread was
	 * started. If this is the case the callback will be called again with these
	 * changes. The callback will get called once for every change.
	 * @param playlistContent
	 */
	void onPlaylistUpdated(String playlistContent);
}
