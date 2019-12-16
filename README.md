# Fusion DiscoveryX

服务注册与发现管理系统，基于Akka生态开发。

产品文档：[https://akka-fusion.github.io/fusion-discoveryx/](https://akka-fusion.github.io/fusion-discoveryx/)

## 开发

### Docker运行环境

```shell script
docker-compose -f docker-compose.yml up -d // --build
docker exec fusion-discoveryx_cassandra cqlsh -f /docker-entrypoint-initdb.d/cassandra-schema.cql
```

`-d`后可添加参数：`postgres`或`cassandra`只启动特定数据库。

### 服务发现测试

*默认使用akka-persistence-jdbc(postgres) 数据库，若需要使用akka-persistence-cassandra请将测试类的配置文件修改为`application-test_cassandra.conf`。*

```sbtshell
> discoveryx-server/testOnly fusion.discoveryx.server.route.NamingManagementRouteTest
```

**配置管理测试**

```sbtshell
> discoveryx-server/testOnly fusion.discoveryx.server.route.ConfigManagementRouteTest
```

## 技术

| 功能       | 使用技术              |
| ---------- | --------------------- |
| 开放API    | Akka gRPC             |
| 集群序例化 | Protobuf              |
| 配置持久化 | Akka Persistence      |
| 容错与扩展 | Akka Cluster Sharding |
| REST       | Akka HTTP             |

