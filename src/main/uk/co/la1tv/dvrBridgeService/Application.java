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
import uk.co.la1tv.dvrBridgeService.hlsRecorder.IPlaylistUpdatedCallback;

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
			a = context.getBean(HlsPlaylistCapture.class, new HlsPlaylist(new URL(url)), new IPlaylistUpdatedCallback() {

				@Override
				public void onPlaylistUpdated(HlsPlaylistCapture source) {
					System.out.println(source.getPlaylistContent());
				}
				
			});
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	a.startCapture();
    }
}
