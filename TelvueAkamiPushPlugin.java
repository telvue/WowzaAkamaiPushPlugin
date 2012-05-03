package com.telvue.wowza;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.wowza.wms.amf.AMFPacket;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.plugin.pushpublish.protocol.rtmp.*;

import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamActionNotify2;

public class TelvueAkamaiPushPlugin extends ModuleBase
{
	String baseDir = "/usr/local/WowzaMediaServer/pushConfigFiles/";
	Map<IMediaStream, PushPublisherRTMP> publishers = new HashMap<IMediaStream, PushPublisherRTMP>();

	class StreamNotify implements IMediaStreamActionNotify2
	{
		File configDir = null;

		String akamaiUsername;
		String akamaiPassword;
		String akamaiHostName;
		String akamaiDstApplicationName;
		String akamaiDstStreamName;

		boolean sendFcPublish           = true;
		boolean sendReleaseStream       = true;
		boolean sendStreamCloseCommands = true;
		boolean adaptiveStreaming       = false;
		boolean debugLogging            = false;

		int akamaiPort;

		public StreamNotify(){

		}

		public StreamNotify(File configDir){
			this.configDir = configDir;
		}

		public void onPlay(IMediaStream stream, String streamName, double playStart, double playLen, int playReset)
		{
		}

		public void onPause(IMediaStream stream, boolean isPause, double location)
		{
		}

		public void onSeek(IMediaStream stream, double location)
		{
		}

		public void onStop(IMediaStream stream)
		{
		}

		public void onMetaData(IMediaStream stream, AMFPacket metaDataPacket)
		{
		}

		public void onPauseRaw(IMediaStream stream, boolean isPause, double location)
		{
		}

		public void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			if (!streamName.startsWith("push-"))
			{
			
				try
				{	
					IApplicationInstance appInstance = stream.getStreams().getAppInstance();
					File configFile = new File(configDir, (streamName + ".cnfg"));
					WMSLoggerFactory.getLogger(null).info("PushPublisherRTMP : Config file name -> " + configFile.getName());

					if (configFile.exists() && configFile.isFile() && configFile.canRead()){
						loadConfigParameters(configFile);
					} else {
						WMSLoggerFactory.getLogger(null).info("PushPublisherRTMP : Config file not found " + configFile.getName());
						return;
					}
					
					synchronized(publishers)
					{
						PushPublisherRTMP publisher = new PushPublisherRTMP();

						WMSLoggerFactory.getLogger(null).info("PushPublisherRTMP : APPLICATION NAME ((" + appInstance.getApplication().getName() + "))");
						WMSLoggerFactory.getLogger(null).info("PushPublisherRTMP : APPINSTANCE NAME ((" + appInstance.getName() + "))");
						WMSLoggerFactory.getLogger(null).info("PushPublisherRTMP : STREAM NAME (("+ streamName + "))");


						// Source stream
						publisher.setAppInstance(appInstance);
						publisher.setSrcStreamName(streamName);

						// Destination stream
						publisher.setHostname(akamaiHostName);
						publisher.setPort(akamaiPort);

						publisher.setDstApplicationName(akamaiDstApplicationName);
						publisher.setDstStreamName(akamaiDstStreamName);

						publisher.setDebugLog(debugLogging);

						publisher.setSendFCPublish(sendFcPublish);
						publisher.setSendReleaseStream(sendReleaseStream);
						publisher.setSendStreamCloseCommands(sendStreamCloseCommands);

						publisher.setConnectionFlashVerion(PushPublisherRTMP.CURRENTFLASHVERSION);

						if (PushPublisherRTMP.isFlashVerionFMLE(publisher.getConnectionFlashVerion()))  {
							PushPublishRTMPAuthProviderAdobe adobeRTMPAuthProvider = new PushPublishRTMPAuthProviderAdobe();

							adobeRTMPAuthProvider.init(publisher);
							adobeRTMPAuthProvider.setUserName(akamaiUsername);
							adobeRTMPAuthProvider.setPassword(akamaiPassword);

							publisher.setRTMPAuthProvider(adobeRTMPAuthProvider);
						}
						else{
							publisher.setAkamaiUserName(akamaiUsername);
							publisher.setAkamaiPassword(akamaiPassword);
						}


						WMSLoggerFactory.getLogger(null).info("PushPublisherRTMP : Akamai Credentials - " + publisher.getAkamaiUserName() + "--" + publisher.getAkamaiPassword() );
						WMSLoggerFactory.getLogger(null).info("PushPublisherRTMP : connecting to " + publisher.getContextStr());

						if (adaptiveStreaming)  {
							publisher.setSendOriginalTimecodes(true);
							publisher.setOriginalTimecodeThreshold(0x100000);
						}
						else  {
							publisher.setSendOriginalTimecodes(false);
						}

						publisher.connect();
						publishers.put(stream, publisher);
					}
				}

				catch(Exception e)
				{
					WMSLoggerFactory.getLogger(null).info("PushPublisherRTMP : " + e.toString());
				}
			}
		}

		public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend) {
			stopPublisher(stream);
		}

		/**
		 * Loads the application and stream specific data from the configuration files and makes them 
		 * available to the rest of the program. This makes the plugin generic and capable of handling
		 * multiple streams and applications. 
		 * @param configFile The file object that points to the configuration for the specific application and stream.
		 * @throws Exception - if the file could not be opened for reading, or the configuration has an error. 
		 */
		private void loadConfigParameters(File configFile) throws Exception{
			Scanner configScanner = new Scanner(configFile);

			int count = 0;
			int foundTokens = 0;
			while(configScanner.hasNextLine()){
				count++;
				String line = configScanner.nextLine();
				if (line.equals("")){
					continue;
				}
				String[] tokens = line.split(":");
				if (tokens.length != 2){
					throw new Exception("Error in config file line " + count + ", invalid token count");
				}

				if (tokens[0].equals("AkamaiUsername")){
					akamaiUsername = tokens[1];
					foundTokens++;
				}
				else if (tokens[0].equals("AkamaiPassword")){
					akamaiPassword = tokens[1];
					foundTokens++;
				}
				else if (tokens[0].equals("AkamaiHostName")){
					akamaiHostName = tokens[1];
					foundTokens++;
				}
				else if (tokens[0].equals("AkamaiDstApplicationName")){
					akamaiDstApplicationName = tokens[1];
					foundTokens++;
				}
				else if (tokens[0].equals("AkamaiDstStreamName")){
					akamaiDstStreamName = tokens[1];
					foundTokens++;
				}
				else if (tokens[0].equals("AkamaiPort")){
					akamaiPort = Integer.parseInt(tokens[1]);
					foundTokens++;
				}
				else if (tokens[0].equals("SendFcPublish")){
					if(tokens[1].equals("true")) {
						sendFcPublish = true;
					} else if(tokens[1].equals("false")) {
						sendFcPublish = false;
					}
				}
				else if (tokens[0].equals("SendReleaseStream")){
					if(tokens[1].equals("true")) {
						sendReleaseStream = true;
					} else if(tokens[1].equals("false")) {
						sendReleaseStream = false;
					}
				}
				else if (tokens[0].equals("SendStreamCloseCommands")){
					if(tokens[1].equals("true")) {
						sendStreamCloseCommands = true;
					} else if(tokens[1].equals("false")) {
						sendStreamCloseCommands = false;
					}
				}
				else if (tokens[0].equals("DebugLogging")){
					if(tokens[1].equals("true")) {
						debugLogging = true;
					} else if(tokens[1].equals("false")) {
						debugLogging = false;
					}
				}
				else if (tokens[0].equals("AdaptiveStreaming")){
					if(tokens[1].equals("true")) {
						adaptiveStreaming = true;
					} else if(tokens[1].equals("false")) {
						adaptiveStreaming = false;
					}
				}
				else{
					throw new Exception("Unknown key token: " + tokens[0]);
				}

			}

			configScanner.close();

			if (foundTokens < 6){
				throw new Exception("Invalid configuration file, not enouph configuration parameters found");
			}
		}
	}

	public void stopPublisher(IMediaStream stream)
	{
		try
		{
			synchronized(publishers)
			{
				PushPublisherRTMP publisher = publishers.remove(stream);
				if (publisher != null)
					publisher.disconnect();
			}
		}
		catch(Exception e)
		{
			WMSLoggerFactory.getLogger(null).error("ModulePushPublishSimpleExample#StreamNotify.onPublish: " + e.toString());
		}
	}

	public void onStreamCreate(IMediaStream stream)
	{
		WMSLoggerFactory.getLogger(null).info("onStreamCreate:: " + stream.getStreams().getAppName());
		File pushConfigDir = new File(baseDir + stream.getStreams().getAppName() + "/");
		if (pushConfigDir.exists() && pushConfigDir.isDirectory()){
			WMSLoggerFactory.getLogger(null).info("Found configuration dir, passing to streamNotify");
			stream.addClientListener(new StreamNotify(pushConfigDir));
		}
		else{
			WMSLoggerFactory.getLogger(null).info("Unable to find configuration directory for stream : " + stream.getStreams().getAppName());
		}

	}

	public void onStreamDestory(IMediaStream stream)
	{
		stopPublisher(stream);
	}
}