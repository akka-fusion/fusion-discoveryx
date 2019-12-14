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

package fusion.discoveryx.server.naming

import helloscala.common.Configuration

trait BaseSettings {
  val c: Configuration

  def defaultPage: Int = c.getInt("default-page")
  def defaultSize: Int = c.getInt("default-size")

  def findSize(size: Int): Int = if (size < defaultSize) defaultSize else size

  def findPage(page: Int): Int = if (page < defaultPage) defaultPage else page

  def findOffset(page: Int, size: Int): Int = if (page > 0) (page - 1) * size else 0
}
