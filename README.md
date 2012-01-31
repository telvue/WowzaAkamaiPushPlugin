
#Telvue Akamai Push Plugin

The Telvue Akamai push plugin allows for an incoming Wowza stream to be pushed to Akamai via RTMP. The plugin supports multiple stream applications, as well as multiple streams per application. All of the specifics are set up through configuration files.

##Required libraries

This plugin requires the Wowza SDK and the PushPublish plugin library `com.wowza.wms.plugin.pushpublish.protocol.rtmp.PushPublisherRTMP` 

##How to set up

First the code must be compiled and added to a .jar file. That jar file is then copied into the lib directory of the WowzaMediaServer folder for your version of Wowza. After this a folder for each Application stream must be created in the conf directory inside of the main Wowza directory. For example, if you had an application called MyLiveStream, and Wowza was installed at `/usr/local/WowzaMediaServer/` then you would create the directory `/usr/local/WowzaMediaServer/conf/MyLiveStream/`. Inside of this directory you need an Application.xml file. A Sample Application.xml has been made available. The key part to note is the modules tag. Inside of this must be the module for the TelvueAkamaiPushPlugin

```	
    <Module>
        <Name>TelvueAkamiPushPlugin</Name>
        <Description>TelvueAkamiPushPlugin</Description>
        <Class>com.telvue.wowza.TelvueAkamiPushPlugin</Class>
    </Module> 
```

This module entry is what allows the plugin to work with the incoming application stream. 

## Configuration files

The plugin was designed to be as generic as possible using configuration files to determine where to push the incoming streams to. 

### Configuration file layout

The layout of the configuration file is very simple. There are 6 key:value pairs that must be included.

```
	AkamaiUsername:username
	AkamaiPassword:password
	AkamaiHostName:p.epxxxx.i.akamaientrypoint.net
	AkamaiDstApplicationName:EntryPoint
	AkamaiDstStreamName:Stream-Name@xxxx
	AkamaiPort:1935
```
	
#### Optional Configuration Settings

These settings are the Wowza recommended settings by default, but are configurable. 

```
  DebugLogging:true
  SendStreamCloseCommands:true
  SendReleaseStream:true
  SendFcPublish:true
  AdaptiveStreaming:false
```

All pairs are separated by a single ":" character, and reside on their own lines. All configuration files must be named after their stream names, and have the extension ".cnfg". So if there is a stream called MyStream, the configuration file would be "MyStream.cnfg"

### Configuration directory layout

The configuration files are placed into a  root directory called pushConfigFiles. This directory will hold folders for each of the applications being streamed into Wowza (Just like the conf directory). If Wowza is installed at `/usr/local/WowzaMediaServer/` then the config file directory is `/usr/local/WowzaMediaServer/pushConfigFiles/` If there is an application called MyLiveStream, and it has 3 streams live1, live2, and live3. Then the directory structure is `/usr/local/WowzaMediaServer/pushConfigFiles/MyLiveStream/` and in this directory are 3 files `live1.cnfg`, `live2.cnfg`, and `live3.cnfg`. Each file has the 6 key:value pairs shown above. 

### Changing the base directory

It is possible (and necessary if you are not running wowza from /usr/local on a Linux box) to change where the plugin looks for the configuration files. Simply modify line 19 of the TelvueAkamiPushPlugin to the location of the pushConfigFiles directory on your server. Just remember to follow the layout within the pushConfigFiles directory and it will work. 

