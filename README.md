# DVR-Bridge-Service
A service which records one or more hls streams simultaneously and re-serves them as a different hls streams. It is controlled via http requests to various endpoints which handle starting the recording, stopping the recording and removing the recording from the server when it is no longer needed. The source stream playlist url is provided in the START request and the url to the generated playlist can be retrieved with the GET_URL request.

It supports both [variant](https://developer.apple.com/library/ios/technotes/tn2288/_index.html#//apple_ref/doc/uid/DTS40012238-CH1-BASIC_VARIANT_PLAYLIST) and standard playlists, and generates "[EVENT](https://developer.apple.com/library/ios/technotes/tn2288/_index.html#//apple_ref/doc/uid/DTS40012238-CH1-EVENT_PLAYLIST)" type playlists.

Requests
---
The following shows the requests that are made, when they are made, and the action the service performs as a result of the requests. It also shows what would cause each request from our website.

Each request will be to the provided url and the data will be provided as simple post data. This makes it possible for extra data to be included and passed to the bridge service implementation in the url with parameters. For this bridge service implementation the url needs to contain a "hlsPlaylistUrl" parameter, which is the source playlist url, and a "secret" parameter, where the secret is configured in the config and forms a basic form of authentication. An example url would be: https://stream1.la1tv.co.uk:3456/dvrBridgeService?secret=super_secret_string&hlsPlaylistUrl=http://www.nasa.gov/multimedia/nasatv/NTV-Public-IPS.m3u8

| Cause Of Request | Request Frequency | Data | Task | Response (JSON)
--------------------|--------------------------------|-----------|-----------|--------------------------|
Media Item Marked Live | Once Whenever This Happens | <ul><li>type=START</li><li>id=[a unique id which represents the stream]</li></ul> | Start recording and generate url to playlist that can be retrieved with "GET_URL" request type. If this is occurs more than once this will work as if the "REMOVE" action has happened just before it. | `{url: <<url to playlist file>>}`
Media Item Marked As Over | Once Whenever This Happens | <ul><li>type=STOP</li><li>id=[a unique id which represents the stream]</li></ul>| Stop recording. Url will still be available from request with "GET_URL" type. Multiple calls will be no-ops. | `<<empty>>`
Media Item Live/Over And No VOD | Repeated Whilst This Is The Case | <ul><li>type=PING</li><li>id=[a unique id which represents the stream]</li></ul> | Responses with a 200 status code if everything is good and the recording is still available, otherwise return an error status code. If not received a ping after a duration longer than the ping interval (configured in config), then the recording will be terminated and removed. | `<<empty>>`
Media Item VOD Uploaded (and accessible), or gone back to "not live" state | Once Whenever This Happens | <ul><li>type=REMOVE</li><li>id=[a unique id which represents the stream]</li></ul> | Removes the recording from the server, and hence the stream url. Multiple calls are no-ops. | `<<empty>>`
Url is needed to send to player in browser | Whenever needed. | <ul><li>type=GET_URL</li><li>id=[a unique id which represents the stream]</li></ul> | Returns the url to the generated hls playlist. | `{url: <<url to playlist file>>}`

All successful requests will get the http status code 200 in the response. If something goes wrong or something was unsuccessful an error code will be returned instead.
