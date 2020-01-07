# 集群部署（使用JDBC持久化）

## 基本配置

Fusion DiscoveryX 使用 Akka Persistence 作为存储层，[akka-persistence-jdbc](https://doc.akka.io/docs/akka-persistence-jdbc/current/) 为 Akka Persistence 提供了 JDBC 访问插件。

```hocon
akka {
  persistence {
    journal {
      plugin = "jdbc-journal"
      // auto-start-journals = ["jdbc-journal"]
    }
    snapshot-store {
      plugin = "jdbc-snapshot-store"
      // auto-start-snapshot-stores = ["jdbc-snapshot-store"]
    }
  }
}
```

Akka Persistence 支持启动时自动回放事件日志和快照恢复。`auto-start-journals` 和 `auto-start-snapshot-stores` 设置当 `ActorSystem` 启动时自动启动 `journal` 和 `snapshot` ，这可能会延长系统的启动时间。否则将会在`EventSourcedBehavior`被创建时才恢复事件日志或快照。

### 初始化数据库

集群部署需要修改配置文件使用独立的数据库，如：PostgreSQL、MySQL、Oracle、MS SQL Server或 H2(TCP版)。在软件包的 `share/jdbc/schema` 目录下能找到各数据库的SQL创建脚本，需要在启动服务之前先将表创建好。

```
├── h2
│   └── h2-schema.sql
├── mysql
│   └── mysql-schema.sql
├── oracle
│   └── oracle-schema.sql
├── postgres
│   └── postgres-schema.sql
└── sqlserver
    └── sqlserver-schema.sql
```

### 数据库配置

Fusion DiscoveryX 使用 [akka-persistence](https://doc.akka.io/docs/akka/current/typed/persistence.html) 作为存储层，通过 HOCON 配置文件来管理数据库访问。在配置路径 `akka-persistence-jdbc.shared-databases` 内添加实际的数据库访问配置。再在 `jdbc-journal`、`jdbc-snapshot-store`和`jdbc-read-journal` 里面通过 `use-shared-db` 指定实际的数据库访问配置项。

Fusion DiscoveryX 默认提供了 H2 和 PostgreSQL 数据库的配置，可以通过修改`use-shared-db`配置项来选择要使用的数据库配置。下面的配置将使用 PostgreSQL 数据库。

**事件日志** 配置 `jdbc-journal` 使用 `postgres` 共享数据库配置：

```hocon
jdbc-journal {
  use-shared-db = "postgres"
}
```

**状态快照存储** 配置 `jdbc-snapshot-store` 使用 `postgres` 共享数据库配置：

```hocon
jdbc-snapshot-store {
  use-shared-db = "postgres"
}
```

**读日志** 配置 `jdbc-read-journal` 使用 `postgres` 共享数据库配置：

```hocon
jdbc-read-journal {
  use-shared-db = "postgres"
}
```

## PostgreSQL

PostgreSQL 数据库配置示例：

```hocon
akka-persistence-jdbc {
  shared-databases {
    postgres {
      profile = "slick.jdbc.PostgresProfile$"
      db {
        url = "jdbc:postgresql://localhost:5432/fusion_discoveryx?reWriteBatchedInserts=true"
        user = "devuser"
        password = "devPass.2019"
        driver = "org.postgresql.Driver"
        numThreads = 5
        maxConnections = 5
        minConnections = 1
      }
    }
  }
}
```

## H2

H2 数据库配置示例：

```hocon
akka-persistence-jdbc {
  shared-databases {
    h2 {
      profile = "slick.jdbc.H2Profile$"
      db {
        url = "jdbc:h2:~/fusion-discoveryx/db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
        user = devuser
        password = devPass.2019
        numThreads = 5
        maxConnections = 5
        minConnections = 1
      }
    }
  }
}
```

## MySQL

MySQL 数据库配置示例：

```hocon
akka-persistence-jdbc {
  shared-databases {
    slick {
      profile = "slick.jdbc.MySQLProfile$"
      db {
        url = "jdbc:mysql://localhost:3306/mysql?cachePrepStmts=true&cacheCallableStmts=true&cacheServerConfiguration=true&useLocalSessionState=true&elideSetAutoCommits=true&alwaysSendSetIsolation=false&enableQueryTimeouts=false&connectionAttributes=none&verifyServerCertificate=false&useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&useLegacyDatetimeCode=false&serverTimezone=UTC&rewriteBatchedStatements=true"
        user = "devuser"
        password = "devPass.2019"
        driver = "com.mysql.cj.jdbc.Driver"
        numThreads = 5
        maxConnections = 5
        minConnections = 1
      }
    }
  }
}
```

## Oracle

Oracle 数据库配置示例：

```hocon
akka-persistence-jdbc {
  shared-databases {
    slick {
      profile = "slick.jdbc.OracleProfile$"
      db {
        url = "jdbc:oracle:thin:@//localhost:1521/xe"
        user = "system"
        password = "oracle"
        driver = "oracle.jdbc.OracleDriver"
        numThreads = 5
        maxConnections = 5
        minConnections = 1
      }
    }
  }
}
```

## SQL Server

SQL Server 数据库配置示例：

```hocon
akka-persistence-jdbc {
  shared-databases {
    slick {
      profile = "slick.jdbc.SQLServerProfile$"
      db {
        url = "jdbc:sqlserver://localhost:1433;databaseName=docker;integratedSecurity=false;"
        user = "docker"
        password = "docker"
        driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
        numThreads = 5
        maxConnections = 5
        minConnections = 1
      }
    }
  }
}
```
