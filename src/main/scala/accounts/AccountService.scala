package fr.corpauration.finance
package accounts

import accounts.models.*
import accounts.models.{ Account, AccountError }
import accounts.persistence.AccountCommandRepository
import kyo.*

case class AccountService(repository: AccountCommandRepository) {

  def getAccountById(id: AccountId): Account < (Abort[AccountError] & Async) =
    repository.rebuildAccount(id)
}

object AccountService {
  val live: Layer[AccountService, Env[AccountCommandRepository]] = Layer.from(AccountService.apply)
}
