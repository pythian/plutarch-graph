/*
 *    Copyright (c) 2019 Pythian and Valentin Nikotin
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package plutarch.server.data.raw

import scala.concurrent.Future

// todo: fast raw to scales converter
trait Raw {
  def put(t: Long, values: Seq[(String, Double)]): Future[Unit]
  def first: Long
  def current: Long
  def iterator: Iterator[(Long, Seq[(String, Double)])]
}

object Raw {
  def create(name: String): Raw = new Impl(name)

  class DummyRawImpl extends Raw {
    var first: Long = Long.MaxValue
    val current: Long = Long.MinValue
    def put(t: Long, values: Seq[(String, Double)]): Future[Unit] = {
      if (first == Long.MaxValue) first = t
      Future.successful()
    }
    def iterator: Iterator[(Long, Seq[(String, Double)])] = Iterator.empty
  }

  class Impl(name: String) extends Raw {
    var first: Long = Long.MaxValue
    var current: Long = Long.MinValue
    private var store: List[(Long, Seq[(String, Double)])] = Nil
    def put(t: Long, values: Seq[(String, Double)]): Future[Unit] = {
      if (first == Long.MaxValue) first = t
      store = (t, values) :: store
      current = current max t
      Future.successful()
    }
    def iterator: Iterator[(Long, Seq[(String, Double)])] = store.iterator
  }
}