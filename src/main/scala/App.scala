package fr.corpauration.finance

import sttp.tapir.server.netty.zio.NettyZioServer
import sttp.tapir.ztapir.ZServerEndpoint
import zio.*

object App extends ZIOAppDefault {

  private val routes: ZIO[Any, Nothing, List[ZServerEndpoint[Nothing, Any]]] = ???

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = ???
}
