### FileDownloader
Tool to download files

### Build
```
sbt assembly
```

### Usage
```
java -jar file_downloader.jar --temp-dir=/tmp/downloads/temp --result-dir=/tmp/downloads/result http://www.ovh.net/files/100Mio.dat ftp://speedtest.tele2.net/20MB.zip ftp://speedtest:speedtest@ftp.otenet.gr/test10Mb.db
```