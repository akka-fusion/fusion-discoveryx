# 介绍

*Fusion DiscoveryX 成长于作者对 Akka 生态的学习，API上参考了Nacos的某些设计。*

Fusion DiscoveryX（之后简称 DiscoveryX）是一款服务注册与发现管理系统，致力于帮助您发现、配置及管理微服务。基于Akka生态开发。

@@project-info{ projectId="fusion-discoveryx" }

## 特性

- 配置管理
- 服务发现
- 动态配置服务

## 开发技术

DiscoveryX 在开发中主要使用到以下技术：

| 功能       | 使用技术              |
| ---------- | --------------------- |
| 开放API    | Akka gRPC             |
| 集群序例化 | Protobuf              |
| 配置持久化 | Akka Persistence      |
| 容错与扩展 | Akka Cluster Sharding |
| REST       | Akka HTTP             |

## 接下来

访问 @ref[quick-start](use/quick-start.md) 开始快速使用 DiscoveryX 。

也可以访问 @ref[architecture](design/architecture.md) 了解 DiscoveryX 的架构设计与技术选择。
