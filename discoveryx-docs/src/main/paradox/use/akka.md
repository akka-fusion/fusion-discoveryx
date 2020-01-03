# 在 Akka 中使用

## Module Info

需要在项目中添加如下依赖：

@@dependency[sbt,Gradle,Maven] { group="com.akka-fusion.fusion" artifact="discoveryx-client_$scala.binary_version$" version="$version$" }

## 服务发现

Akka通过 [akka-discovery](https://doc.akka.io/docs/akka/current/discovery/index.html) 提供了默认的服务发现功能，DiscoveryX Client提供了对其的支持。我们只需要配置`akka.discovery`设置使用`fusion-discoveryx`使用 DiscoveryX Client 来为 akka-discovery 提供服务发现功能。

@@snip [discovery](../../../../../discoveryx-client/src/main/resources/reference.conf) { #discovery }

