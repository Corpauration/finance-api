package fr.corpauration.finance

import accounts.AccountController
import fr.corpauration.finance
import sttp.tapir.server.netty.zio.NettyZioServer
import sttp.tapir.ztapir.ZServerEndpoint
import zio.*

object App extends ZIOAppDefault {

  private val endpoints = for {
    account <- ZIO.serviceWith[AccountController](_.endpoints)
  } yield account

  private val routes: ZIO[Any, Nothing, List[ZServerEndpoint[Nothing, Any]]] =
    endpoints.provideLayer(AccountController.live)

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = {
    routes.flatMap(ses => NettyZioServer().addEndpoints(ses).start())
  }
}
