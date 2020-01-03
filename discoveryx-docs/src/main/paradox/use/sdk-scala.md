# Scala SDK

## Module Info

需要在项目中添加如下依赖：

@@dependency[sbt,Gradle,Maven] { group="com.akka-fusion.fusion" artifact="discoveryx-client_$scala.binary_version$" version="$version$" }

## 使用

### 通过配置使用

Fusion DiscoveryX Client for Scala SDK 使用 akka-grpc 开发，使用时需要配置 `akka.grpc.client` 指定 DiscoveryX Server 地址。

@@snip [discovery](../../../../../discoveryx-client/src/main/resources/reference.conf) { #grpc-client }

若需要把服务自动注册到 DiscoveryX Server，需要配置 `discoveryx.client.naming.auto-registration = true` 。同时，还需要设置`namespace`、`service-name`、`ip`、`port`，其中`ip`可通过自动检测获取（当不指定或设置为空字符串时，若发现自动检测到的IP地址不正确请手动指定）。

## 配置

**discoveryx.client**

@@snip [discovery](../../../../../discoveryx-client/src/main/resources/reference.conf) { #discoveryx-client }

**akka.grpc.client**

@@snip [discovery](../../../../../discoveryx-client/src/main/resources/reference.conf) { #grpc-client }
