package fr.corpauration.finance
package accounts

import accounts.models.*
import accounts.persistence.AccountCommandRepository
import accounts.query.AccountQueryRepository

import kyo.*

case class AccountService(command: AccountCommandRepository, query: AccountQueryRepository) {

  def getAccountById(id: AccountId): Account < (Abort[AccountError] & Async) =
    query.findOneById(id).map(_ ?=> _.account)
}

object AccountService {

  val live: Layer[AccountService, Env[AccountCommandRepository] & Env[AccountQueryRepository]] =
    Layer.from(AccountService.apply)
}
