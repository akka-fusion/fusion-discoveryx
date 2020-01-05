# 在 Play 中使用

## Module Info

需要在项目中添加如下依赖：

@@dependency[sbt,Gradle,Maven] { 
  group="com.akka-fusion.fusion" artifact="discoveryx-client-play-ws_$scala.binary_version$" version="$version$" 
}

@@@note { title=服务发现 }
`discoveryx-client-play-ws`基于`discoveryx-client`开发，有关Scala SDK与服务发现配置内容请参阅 @ref[Scala SDK](sdk-scala.md) 和 @ref[在 Akka 中使用](akka.md) 。 
@@@

## SDK

Fusion DiscoveryX 提供了 `DiscoveryXWSClient` 工具类来创建或包装一个`StandaloneWSClient`或`WSClient`。`DiscoveryXStandaloneWSClient`实现了`StandaloneWSClient`接口并重写了`url`函数，使其可以解析请求URL里面的`hostname`部分并将其转换成实际访问的`ip:port`地址。

### StandaloneWSClient

Play已将`WSClient`拆分独立出来，我们可以单独使用它。

Scala
:  @@snip [DiscoveryXWSClient.scala](../../../../../discoveryx-client-play-ws/src/main/scala/fusion/discoveryx/client/play/scaladsl/DiscoveryXWSClient.scala) { #standaloneWSClient }

Java
:  @@snip [DiscoveryXWSClient.scala](../../../../../discoveryx-client-play-ws/src/main/scala/fusion/discoveryx/client/play/javadsl/DiscoveryXWSClient.java) { #standaloneWSClient }

使用

@@snip [DiscoveryXWSClientTest.scala](../../../../../discoveryx-client-play-ws/src/test/scala/fusion/discoveryx/client/play/scaladsl/DiscoveryXWSClientTest.scala) { #standaloneWSClient-create }

### WSClient

使用 Playframework 附带的 `WSClient` 时，调用 `StandaloneWSClient` 的 `wsClient` 函数新建或包装一个 `WSClient`。

Scala
:  @@snip [DiscoveryXWSClient.scala](../../../../../discoveryx-client-play-ws/src/main/scala/fusion/discoveryx/client/play/scaladsl/DiscoveryXWSClient.scala) { #wsClient }

Java
:  @@snip [DiscoveryXWSClient.scala](../../../../../discoveryx-client-play-ws/src/main/scala/fusion/discoveryx/client/play/javadsl/DiscoveryXWSClient.java) { #wsClient }

使用

```play
import javax.inject.Inject

import fusion.discoveryx.client.play.scaladsl.DiscoveryXPlayWSClient
import fusion.discoveryx.client.play.scaladsl.DiscoveryXPlay
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._
import play.api.http.HttpEntity
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.ExecutionContext

// 直接流入
class MainForImmediately @Inject() (
  ws: DiscoveryXPlayWSClient,
  val controllerComponents: ControllerComponents) extends BaseController {}

// 通过 @Named
class MainForNamed @Inject() (
  @Named("discoveryx") ws: WSClient, 
  val controllerComponents: ControllerComponents) extends BaseController {}

// 通过附加注解
class MainForNamed @Inject() (
  @DiscoveryXPlay ws: WSClient, 
  val controllerComponents: ControllerComponents) extends BaseController {}
```
```play
val request: WSRequest = ws.url(url)
```
