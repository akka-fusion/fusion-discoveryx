# 快速开始

## 下载

@@@vars
下载压缩包 [discoveryx-server-$version$.zip](https://github.com/akka-fusion/fusion-discoveryx/releases)，解压到目录。如：
@@@

@@@vars
```
/home/yangjing/discoveryx-server-$version$
```
@@@

## 启动服务

进入软件根目录执行启动脚本运行：

@@@vars
```
cd discoveryx-server-$version$/
./bin/discoveryx-server
```
@@@

**Windows**系统请执行 `bin/discoveryx-server.bat` 。

打开浏览器，输入：`http://localhost:48000`即可访问 Fusion DiscoveryX 管理控制台。

@@@note
默认将使用 H2 嵌入式文件数据库，这种方式只适合测试使用。若需要在产品中使用，请使用独立数据库，参阅： @ref[集群部署（使用JDBC持久化）](../deploy/cluster-jdbc.md) 或 @ref[集群部署（使用Cassandra作为存储）](../deploy/persistence-cassandra.md) 。
@@@

## 接下来

- @ref[在 Akka 中使用](akka.md)
- @ref[在 Play 中使用](play.md)
- @ref[在 Spring 中使用](spring.md)
