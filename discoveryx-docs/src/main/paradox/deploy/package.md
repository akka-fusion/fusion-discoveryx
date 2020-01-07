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

## 源码构建

```
git clone https://github.com/akka-fusion/fusion-discoveryx.git
cd fusion-discoveryx
pushd web-console
yarn && yarn build
popd
pushd ../scripts
./publish-dist.sh
popd
sbt "discoveryx-server" dist
```

@@@vars
最终将在 `discoveryx-server/target/universal` 目录生成 discoveryx-server-$version$.zip 软件包。解压生成的 **zip** 软件包（discoveryx-server-$version$.zip），执行 `bin` 目录里的 `sh` 或 `bat` 脚本即可运行程序。
@@@

@@@vars
```
cd discoveryx-server/target/universal
unzip discoveryx-server-$version$.zip
cd discoveryx-server-$version$
./bin/discoveryx-server
```
@@@
