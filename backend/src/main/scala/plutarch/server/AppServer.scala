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

package plutarch.server

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{ ConnectionContext, Http }
import akka.stream.ActorMaterializer
import buildinfo.BuildInfo
import com.typesafe.scalalogging.LazyLogging
import plutarch.server.config.ServerConfig
import plutarch.server.data.metricManager.MetricManager
import plutarch.server.data.persistence.FileManager
import plutarch.server.data.store.DefaultMetricStoreCreatorCreator
import plutarch.server.pipelines._
import plutarch.server.ws.WebSocketFlowCoordinator

import scala.util.{ Failure, Success }

object AppServer extends LazyLogging {

  private def bindAndHandle(route: Route, connectionContext: ConnectionContext, port: Int)(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer): Unit = {

    val futureBinding = Http().bindAndHandle(
      handler = Route.handlerFlow(route),
      interface = ServerConfig.interface,
      port = port,
      connectionContext = connectionContext)

    futureBinding.onComplete {
      case Success(binding) ⇒
        logger.info(s"Server listens on port ${binding.localAddress.getPort}, fullOpt=${BuildInfo.fullOpt}")
      case Failure(cause) ⇒
        logger.warn("Could not start server. Cause: {}", cause.getMessage, cause)
    }(actorSystem.dispatcher)
  }

  def up(): Unit = {

    implicit val actorSystem: ActorSystem = ActorSystem(s"${BuildInfo.name}-actor-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import actorSystem.dispatcher

    //val fm = FileManager()
    //val metricManager = MetricManager.create(fm.getCC(Paths.get("./src/test/fixtures/fs01")))

    val metricManager = MetricManager.create(DefaultMetricStoreCreatorCreator)
    val webSocketFlowCoordinator = WebSocketFlowCoordinator.create(metricManager)

    val externalPipeline = new ExternalPipeline(webSocketFlowCoordinator, metricManager, actorSystem)
    val dummyPipeline = new DummyPipeline(webSocketFlowCoordinator, metricManager, actorSystem)
    //val currencyPipeline = new CurrencyPipeline(webSocketFlowCoordinator, metricManager, actorSystem)

    val service = new Service(webSocketFlowCoordinator, metricManager)

    ServerConfig.TLS.connectionContext match {
      case Success(connectionContext) ⇒ bindAndHandle(service.routes, connectionContext, ServerConfig.TLS.port)
      case Failure(exception)         ⇒ logger.warn("Could not create TLS connection context", exception)
    }

    // listen both https and http
    bindAndHandle(service.routes, ConnectionContext.noEncryption, ServerConfig.port)
  }

}
