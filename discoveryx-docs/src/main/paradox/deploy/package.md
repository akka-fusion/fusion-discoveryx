# 软件包

## 下载

在 <a href="https://github.com/akka-fusion/fusion-discoveryx/releases">https://github.com/akka-fusion/fusion-discoveryx/releases</a> 下载最新版本，解压缩到目录执行。

**Unix/Linux**

```shell script
unzip discoveryx-server-[VERSION].zip
cd discoveryx-server
./bin/discoveryx-server
```

**Windows**

```
unzip discoveryx-server-[VERSION].zip
cd discoveryx-server
bin/discoveryx-server.bat
```

## 构建

下载源码，执行`sbt`命令。

```
git clone https://github.com/akka-fusion/fusion-discoveryx.git
cd fusion-discoveryx
sbt
```

执行sbtshell命令：`discoveryx-server/dist`，将在`discoveryx-server/target/universal`目录生成`discoveryx-server-[VERSION].zip`软件包，解压缩后即可执行。

```sbtshell
> discoveryx-server/dist
```
