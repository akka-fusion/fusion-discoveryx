# 其它存储机制

Fusion DiscoveryX 使用 Akka Persistence 作为存储层，且使用 Akka Persistence 来存储所有数据，所以理论上只要实现了 Akka Persistence Plugins 的存储系统都可应用于 Fusion DiscoveryX。

需要修改 `akka.persistence` 配置使用想要使用的存储插件，同时，还需要将相应插件依赖的 **jar** 包入到 Fusion DiscoveryX 软件 `lib` 目录里面。

## MongoDB

- [https ://github.com/scullxbones/akka-persistence-mongo](https://github.com/scullxbones/akka-persistence-mongo)

@@dependency[sbt,Gradle,Maven] { group="com.github.scullxbones" artifact="akka-persistence-mongo-rxmongo_2.12" version="$akka.persistence.mongo.version$" }

## DynamoDB

- [https://github.com/akka/akka-persistence-dynamodb](https://github.com/akka/akka-persistence-dynamodb)

@@dependency[sbt,Gradle,Maven] { group="com.typesafe.akka" artifact="akka-persistence-dynamodb_$scala.binary_version$" version="$akka.persistence.dynamodb.version$" }

## CouchBase

- [https://github.com/akka/akka-persistence-couchbase](https://github.com/akka/akka-persistence-couchbase)

@@dependency[sbt,Gradle,Maven] { group="com.lightbend.akka" artifact="akka-persistence-couchbase_$scala.binary_version$" version="$akka.persistence.couchbase.version$" }
