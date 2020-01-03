/*
 * Copyright 2019 akka-fusion.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fusion.discoveryx.server.util

import java.net.{ URLDecoder, URLEncoder }
import java.nio.charset.StandardCharsets
import java.util.concurrent.ThreadLocalRandom

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.Cookie
import fusion.discoveryx.common.Constants
import fusion.discoveryx.server.protocol.TokenAccount
import helloscala.common.util.DigestUtils

import scala.util.control.NonFatal

object SessionUtils {
  def getTokenFromRequest(request: HttpRequest): Option[String] = {
    request
      .header[Cookie]
      .flatMap(tokenFromCookie)
      .orElse(request.headers.find(_.name == Constants.SESSION_TOKEN_NAME).map(_.value))
  }

  def tokenFromCookie(cookie: Cookie): Option[String] =
    cookie.cookies.find(_.name == Constants.SESSION_TOKEN_NAME).map(_.value)

  def decodeToken(token: String): String = new String(URLDecoder.decode(token, StandardCharsets.UTF_8))

  def parseAccount(originalToken: String): Either[String, TokenAccount] =
    try {
      val token = decodeToken(originalToken)
      SessionUtils.decodeToken(token).split("\\|") match {
        case Array(account, _) => Right(TokenAccount(originalToken, account))
        case other             => Left(s"Parse account from token failure. parsed value is $other")
      }
    } catch {
      case NonFatal(e) => Left(e.getLocalizedMessage)
    }

  def generateSessionToken(account: String): String = {
    val bytes = Array.ofDim[Byte](40)
    ThreadLocalRandom.current().nextBytes(bytes)
    val token = DigestUtils.md5Hex(bytes)
    URLEncoder.encode(s"$account|$token", StandardCharsets.UTF_8)
  }
}
