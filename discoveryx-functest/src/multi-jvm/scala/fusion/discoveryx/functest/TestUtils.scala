package fusion.discoveryx.functest

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.headers.Cookie
import fusion.discoveryx.client.HttpUtils
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.protocol._

import scala.concurrent.Future

object TestUtils {
  def apply(system: ActorSystem[_]): TestUtils = new TestUtils()(system)
}

import fusion.discoveryx.server.util.ProtobufJsonSupport._
class TestUtils private ()(implicit system: ActorSystem[_]) {
  private val httpClient = HttpUtils(system)

  def login(): Future[UserResponse] = {
    httpClient
      .singleRequest(
        HttpMethods.POST,
        "http://localhost:48000/fusion/discoveryx/console/sign/Login",
        entity = Login(Constants.DISCOVERYX, Constants.DISCOVERYX))
      .onSuccessResponseAs[UserResponse]
  }

  def listNamespace(logined: Logined, page: Int = 1, size: Int = 20)(): Future[ManagementResponse] = {
    httpClient
      .singleRequest(
        HttpMethods.POST,
        "http://localhost:48000/fusion/discoveryx/console/namespace/ListNamespace",
        List(Cookie(Constants.SESSION_TOKEN_NAME, logined.token)),
        ListNamespace(page, size))
      .onSuccessResponseAs[ManagementResponse]
  }

  def createNamespace(logined: Logined, namespace: String): Future[ManagementResponse] = {
    httpClient
      .singleRequest(
        HttpMethods.POST,
        "http://localhost:48000/fusion/discoveryx/console/namespace/CreateNamespace",
        List(Cookie(Constants.SESSION_TOKEN_NAME, logined.token)),
        CreateNamespace(namespace, s"Default namespace: $namespace."))
      .onSuccessResponseAs[ManagementResponse]
  }
}
