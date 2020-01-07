# 部署

Fusion DiscoveryX 提供 **zip** 包和 **Docker** 镜像两种打包部署方式，高级用户亦可以下载源码自行编译打包以获得更多的定制性。Fusion DiscoveryX 支持单机、集群部署，官方集群部署提供了对 JDBC 、Cassandra 的后端存储支持，用户通过 Akka Persistence Plugins 可以很方便的集成其它存储方式。如：

- MongoDB: [https://github.com/scullxbones/akka-persistence-mongo](https://github.com/scullxbones/akka-persistence-mongo)
- DynamoDB: [https://github.com/akka/akka-persistence-dynamodb](https://github.com/akka/akka-persistence-dynamodb)
- CouchBase: [https://github.com/akka/akka-persistence-couchbase](https://github.com/akka/akka-persistence-couchbase)

@@toc { depth=3 }

@@@ index

- [package](package.md)
- [single](single.md)
- [cluster-jdbc](cluster-jdbc.md)
- [persistence-cassandra](persistence-cassandra.md)
- [docker](docker.md)
- [other-persistence](other-persistence.md)

@@@
