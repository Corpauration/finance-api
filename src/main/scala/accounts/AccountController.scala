package fr.corpauration.finance
package accounts

import accounts.models.*
import accounts.models.AccountId
import accounts.persistence.AccountCommandRepository
import accounts.routes
import accounts.routes.*
import common.utils.helper.mapError
import kyo.*

case class AccountController(service: AccountService) {

  val getAccountLogic: Unit < Routes = Routes.add(routes.getAccount) { id =>
    service
      .getAccountById(AccountId(id))
      .map(_ ?=> account => AccountOutput(account))
      .mapError(error => HttpError(error.status, error.message))
  }
}

object AccountController {
  val live: Layer[AccountController, Env[AccountService]] = Layer.from(AccountController.apply)
}
