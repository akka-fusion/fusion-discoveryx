# Fusion DiscoveryX

服务注册与发现管理系统，基于Akka生态开发。

产品文档：[https://akka-fusion.github.io/fusion-discoveryx/](https://akka-fusion.github.io/fusion-discoveryx/)

## test

**数据库**

以PostgreSQL数据库为例，运行以下脚本，并修改 [discoveryx-server/src/test/resources/application-test.conf](discoveryx-server/src/test/resources/application-test.conf) 相对数据库访问配置。

```postgresql
DROP TABLE IF EXISTS public.journal;

CREATE TABLE IF NOT EXISTS public.journal
(
    ordering        BIGSERIAL,
    persistence_id  VARCHAR(255) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    deleted         BOOLEAN      DEFAULT FALSE,
    tags            VARCHAR(255) DEFAULT NULL,
    message         BYTEA        NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);

CREATE UNIQUE INDEX journal_ordering_idx ON public.journal (ordering);

DROP TABLE IF EXISTS public.snapshot;

CREATE TABLE IF NOT EXISTS public.snapshot
(
    persistence_id  VARCHAR(255) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    created         BIGINT       NOT NULL,
    snapshot        BYTEA        NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);
```

**服务发现测试**

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
