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

package plutarch.shared

import plutarch.shared.data.Aggregations.Aggregation
import plutarch.shared.data.DataObject
import plutarch.shared.data.metrics.Meta

object Protocol {
  sealed trait WSOpenRequest
  case class WSNew() extends WSOpenRequest
  //case class WSReconnect(sessionId: String) extends WSOpenRequest
  case class WSEmptyRequest() extends WSOpenRequest
  case class WSInvalidRequest(str: String) extends WSOpenRequest

  sealed trait WSMessage
  sealed trait Broadcastable
  case class WSReady() extends WSMessage
  case class WSKeepAlive(tmsp: Long) extends WSMessage with Broadcastable
  case class WSData(metric: String, tmsp: Long, data: Seq[(Int, Double)], objects: Seq[DataObject]) extends WSMessage with Broadcastable
  case class WSAccepted(id: String, metrics: Seq[String]) extends WSMessage
  case class WSMetricsConfs(confs: Map[String, Meta]) extends WSMessage
  case class WSCurrents(metric: String, key: Long, data: Seq[CurrentData]) extends WSMessage
  case class CurrentData(aggregation: Aggregation, scale: Int, key: Long, version: Long, data: Map[Int, Double], objects: Seq[DataObject])

  case class WSRejected(msg: String) extends WSMessage
  case class WSTextMessage(message: String) extends WSMessage with Broadcastable
  case class WSSubscribe(metrics: Seq[String]) extends WSMessage
  case class WSSubscribeRealTime(metric: String, aggregation: Aggregation, scale: Int, clientId: String, active: Boolean) extends WSMessage
  case class WSHistRequest(requestId: Int, metric: String, aggregation: Aggregation, scale: Int, intervals: Seq[Interval]) extends WSMessage
  case class WSLoggingToServer(msg: String) extends WSMessage

  val CLIENT_CURRENT_INTERVAL = 1000
  case class Interval(left: Double, right: Double) {
    def isIntersected(that: Interval): Boolean = {
      this.left <= that.right && that.left <= this.right
    }
    def isIntersected(others: Seq[Interval]): Boolean = {
      others.exists(isIntersected)
    }
    def length: Double = right - left
  }
}
