package fr.corpauration.finance
package accounts

import java.util.UUID

import accounts.models.*
import accounts.routes.*
import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import zio.*

case class AccountController(service: AccountService) {

  val listAccountsLogic: ZServerEndpoint[Any, Any] = routes.listAccounts.zServerLogic {
    status =>
      service.listByStatus(status).map(_.map(AccountOutput.apply).toList).mapError(e => HttpError(e.status, e.message))
  }

  val getAccountLogic: ZServerEndpoint[Any, Any] = routes.getAccount.zServerLogic {
    id => service.getAccountById(id).map(AccountOutput.apply).mapError(e => HttpError(e.status, e.message))
  }

  val createAccountLogic: ZServerEndpoint[Any, Any] = routes.createAccount.zServerLogic {
    payload =>
      if payload.maxDebtAllowed < 0 then ZIO.fail(
        HttpError(StatusCode.PreconditionFailed, s"${payload.maxDebtAllowed} is negative")
      )
      else
        service
          .createAccount(
            Account(
              id = AccountId(UUID.randomUUID()),
              ownerId = payload.ownerId,
              metadata = AccountMetadata(
                name = payload.name,
                description = payload.description,
                tag = payload.tag,
                labels = payload.labels
              ),
              maxDebtAllowed = payload.maxDebtAllowed,
              balance = payload.balance,
              status = AccountStatus.ACTIVE
            )
          )
          .map(AccountOutput.apply)
          .mapError(e => HttpError(e.status, e.message))
  }

  val endpoints: List[ZServerEndpoint[Nothing, Any]] = List(
    listAccountsLogic,
    getAccountLogic,
    createAccountLogic
  )
}

object AccountController {

  val live: ZLayer[AccountService, Nothing, AccountController] =
    ZLayer.service[AccountService].map(AccountController.apply)
}
