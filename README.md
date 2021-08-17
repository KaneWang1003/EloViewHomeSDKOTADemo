# OTAManager

演示EloviewHomeSDK 的 OTA update

downloadApplyABOTA(Context context, java.lang.String accessToken, java.lang.String otaURL, 
java.lang.String otaPath, java.lang.String otaVersion, Handler handler)


This method is used to download OTA file from remote http server or from local file path, VERIFY the OTA file and APPLY the update.

支持断点续传，目前没有实现多线程
