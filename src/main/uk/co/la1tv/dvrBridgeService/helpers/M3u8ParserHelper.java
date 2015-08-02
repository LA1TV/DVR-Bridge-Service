package uk.co.la1tv.dvrBridgeService.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.co.la1tv.dvrBridgeService.hlsRecorder.exceptions.PlaylistRequestException;

@Service
public class M3u8ParserHelper {
	
	private static Logger logger = Logger.getLogger(M3u8ParserHelper.class);
	
	@Value("${m3u8Parser.nodePath}")
	private String nodePath;
	
	@Value("${m3u8Parser.applicationJsPath}")
	private String m3u8ParserApplicationPath;
	
	/**
	 * Make request to get variant playlist, parse it, and return info.
	 * @return
	 * @throws PlaylistRequestException 
	 */
	public JSONObject getPlaylistInfo(URL playlistUrl) throws PlaylistRequestException {
		String playlistUrlString = playlistUrl.toExternalForm();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		CommandLine commandLine = new CommandLine(FileHelper.format(nodePath));
		commandLine.addArgument(FileHelper.format(m3u8ParserApplicationPath));
		commandLine.addArgument(playlistUrlString);
		DefaultExecutor exec = new DefaultExecutor();
		// handle the stdout stream, ignore error stream
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, null);
		exec.setStreamHandler(streamHandler);
		int exitVal;
		try {
			exitVal = exec.execute(commandLine);
		} catch (IOException e1) {
			e1.printStackTrace();
			logger.warn("Error trying to retrieve playlist information.");
			throw(new PlaylistRequestException());
		}
	    if (exitVal != 0) {
			logger.warn("Error trying to retrieve playlist information.");
			throw(new PlaylistRequestException());
		}
		String playlistInfoJsonString = outputStream.toString();
		JSONObject playlistInfo = null;
		try {
			playlistInfo = (JSONObject) JSONValue.parseWithException(playlistInfoJsonString);
		} catch (ParseException e) {
			e.printStackTrace();
			logger.warn("Error trying to retrieve playlist information.");
			throw(new PlaylistRequestException());
		}
		return playlistInfo;
	}
	
	public boolean isVariantPlaylist(URL playlistUrl) throws PlaylistRequestException {
		JSONObject info = getPlaylistInfo(playlistUrl);
		try {
			return !((JSONArray)((JSONObject) info.get("items")).get("StreamItem")).isEmpty();
		}
		catch(Exception e) {
			return false;
		}
	}
}
