# 集群部署（使用Cassandra作为存储）

## 基本配置

Fusion DiscoveryX 使用 Akka Persistence 作为存储层，[akka-persistence-cassandra](https://doc.akka.io/docs/akka-persistence-cassandra/current/) 为 Akka Persistence 提供了 JDBC 访问插件。

```hocon
akka {
  persistence {
    journal {
      plugin = "cassandra-journal"
      // auto-start-journals = ["cassandra-journal"]
    }
    snapshot-store {
      plugin = "cassandra-snapshot-store"
      // auto-start-snapshot-stores = ["cassandra-snapshot-store"]
    }
  }
}
```

### 初始化数据库

在启动 Fusion DiscoveryX 之前，需要提前建好 Cassandra 数据库表。在软件包的 `share/cassandra/schema` 目录下能找到 Cassandra 数据库表的创建脚本。

```
├── journal-schema.cql
└── snapshot-schema.cql
```
