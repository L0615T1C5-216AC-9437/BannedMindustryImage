![Discord](https://img.shields.io/discord/917595056075071488)
[![Github All Releases](https://img.shields.io/github/downloads/L0615T1C5-216AC-9437/BannedMindustryImage/total.svg)]()
# GlobalImageBan
A mindustry plugin that checks if a image is banned. No more nsfw (hopefully)

 **ATTENTION:**
1) We need people to submit NSFW Schematics in the [BMI](https://discord.gg/v7SyYd2D3y) server (Only 18+ Allowed). The more submissions we get the more NSFW get blocked.
2) The api uses HTTP, not HTTPS, please refrain from attempting to access the api with https requests.

## Installation Guide
1. Download the latest mod verion in [#Releases](https://github.com/L0615T1C5-216AC-9437/MaxRateCalculator/releases).  
2. Go to your server's directory \ config \ mods
3. Move the mod (`Jar` file) into the mods folder  
4. Restart the server.  
5. Use the `mods` command to list all mods. If you see GIB as a mod, GIB was successfully installed.  
6. Join the BMI discord server and use the bot to receive a API key.  
7. Config the api key through the `bmiconfig` command.  
8. Restart the server once again and look for a successfull api connection message.  

## Usage
The plugin will scan the code of logic blocks, only when placed, for `drawflush` which signifies the code prints to a screen.  
The code is then hashed and sent to `http://c-n.ddns.net:8888` to see if the hash is banned.  

## Settings  
These are the raw setting names for the `bmiconfig`.
* `ApiKey` (String): Api Key to access BMI API. To get an API key go to discord.gg/v7SyYd2D3y and use the bot slash command.  
* `NudityAction` (Int): 0 for Ban, 1 for Kick, 2 for Force Disconnect, 3 to Ignore.
default: `1`
* `BorderlineAction` (Int): 0 for Ban, 1 for Kick, 2 for Force Disconnect, 3 to Ignore.
default: `1`
* `FurryAction` (Int): 0 for Ban, 1 for Kick, 2 for Force Disconnect, 3 to Ignore.
default: `3`
* `ComplexSearch` (Boolean): If true, each drawflush will be checked individually.  
default: `false`  
* `DisconnectMessage` (String): What message to send when user is banned/kicked/disconnected. Identifier and BMI discord invite will still be sent.
default: `[scarlet]Built banned logic image`  
* `ConnectionTimeout` (Int): How long, in millis, the server will wait for a http response before giving up. 
default: `1000`  
*Note: `c-n.ddns.net` does not respond to pings.*  
* `gib_KickDuration` (Int): How many minutes the player will kick be for.  
default: `180`  
* `HTTPThreadCount` (Int): Max # of threads to use for HTTP requests.
default: 4

## Commands  
`bmiconfig`: Same as `config` but for GIB settings. Set value to `default` for default value.  
`gibclearcache`: Clears the hash cache

## RPC Info
Rate Limit: Depends on API tier, unverified users can check 10 hashes per second.
