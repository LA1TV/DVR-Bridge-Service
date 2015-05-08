package uk.co.la1tv.dvrBridgeService;


import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylist;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylistCapture;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylistCaptureState;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.ICaptureStateChangeListener;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.IPlaylistUpdatedListener;

@ComponentScan
@EnableAutoConfiguration
public class Application {
	private static Logger logger = Logger.getLogger(Application.class);
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    
    @Autowired
	private ApplicationContext context;
    
    @PostConstruct
    public void onPostConstruct() {
    	logger.info("Application loaded!");
    	
    	HlsPlaylistCapture a = null;
		try {
			String url = "http://public.infozen.cshls.lldns.net/infozen/public/public/public_200.m3u8";
			//url = "https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear1/prog_index.m3u8";
			a = context.getBean(HlsPlaylistCapture.class, new HlsPlaylist(new URL(url)));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		final HlsPlaylistCapture b = a;
		
		b.setPlaylistUpdatedListener(new IPlaylistUpdatedListener() {
			
			private int count = 0;
			
			@Override
			public void onPlaylistUpdated(String playlistContent) {
				System.out.println(playlistContent);
				if (count++ == 3) {
					System.out.println("Stopping capture.");
					b.stopCapture();
					b.deleteCapture();
				}
			}
			
		});
		
		b.setStateChangeListener(new ICaptureStateChangeListener() {

			@Override
			public void onStateChange(HlsPlaylistCaptureState newState) {
				System.out.println("STATE CHANGED!");
			}
			
		});
		
		
    	a.startCapture();
    }
}
