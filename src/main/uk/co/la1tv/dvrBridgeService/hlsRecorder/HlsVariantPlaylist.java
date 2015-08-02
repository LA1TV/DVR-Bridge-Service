package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.awt.Dimension;
import java.net.URL;
import java.util.ArrayList;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.helpers.M3u8ParserHelper;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.exceptions.PlaylistRequestException;

/**
 * Represents a variant playlist file.
 */
@Component("HlsVariantPlaylist")
@Scope("prototype")
public class HlsVariantPlaylist extends HlsPlaylist {

	private static Logger logger = Logger.getLogger(HlsVariantPlaylist.class);

	@Autowired
	private M3u8ParserHelper m3u8ParserHelper;
	
	@Autowired
	private ApplicationContext context;
	private HlsPlaylist[] hlsPlaylists = null;
	private final URL variantPlaylistUrl;
	
	public HlsVariantPlaylist(URL variantPlaylistUrl) {
		super(variantPlaylistUrl);
		this.variantPlaylistUrl = variantPlaylistUrl;
	}
	
	@PostConstruct
	private void onPostConstruct() {
		// make request to get list of playlists this contains and create them
		JSONObject playlistInfo;
		try {
			playlistInfo = m3u8ParserHelper.getPlaylistInfo(variantPlaylistUrl);
		} catch (PlaylistRequestException e) {
			e.printStackTrace();
			return;
		}
		
		ArrayList<HlsPlaylist> playlists = new ArrayList<>();
		try {
			JSONObject items = (JSONObject) playlistInfo.get("items");
			JSONArray streamItems = (JSONArray) items.get("StreamItem");
			for (int i=0; i<streamItems.size(); i++) {
				JSONObject streamItem = (JSONObject) streamItems.get(i);
				JSONObject attributes = (JSONObject) ((JSONObject) streamItem.get("attributes")).get("attributes");
				int bandwidth = new Integer(String.valueOf(attributes.get("bandwidth")));
				String codecs = (String) attributes.get("codecs");
				if (codecs == null) {
					throw(new RuntimeException("Missing codecs string."));
				}
				JSONArray resolutionJsonArray = (JSONArray) attributes.get("resolution");
				Dimension resolution = new Dimension(new Integer(String.valueOf(resolutionJsonArray.get(0))), new Integer(String.valueOf(resolutionJsonArray.get(1))));
				JSONObject properties = (JSONObject) streamItem.get("properties");
				String uriString = (String) properties.get("uri");
				URL playlistUrl = new URL(getUrl(), String.valueOf(uriString));
				playlists.add((HlsPlaylist) context.getBean("HlsPlaylist", playlistUrl, bandwidth, codecs, resolution));
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
			logger.warn("Error occurred when trying to traverse variant playlist.");
			return;
		}
		
		hlsPlaylists = playlists.toArray(new HlsPlaylist[playlists.size()]);
	}
	
	/**
	 * Get the playlist objects corresponding to the playlists
	 * the variant playlist contains.
	 * Returns null if there was an error trying to get the playlists.
	 * @return
	 */
	public HlsPlaylist[] getPlaylists() {
		return hlsPlaylists;
	}

}
