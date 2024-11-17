package fr.corpauration.finance
package accounts

import accounts.models.*
import accounts.models.AccountError.{ AccountAlreadyCreated, AccountNotFound }
import accounts.persistence.AccountCommandRepository
import accounts.query.AccountQueryRepository
import zio.*

case class AccountService(command: AccountCommandRepository, query: AccountQueryRepository) {

  def getAccountById(id: AccountId): ZIO[Any, AccountError, Account] =
    query.findOneById(id).map(_.account)

  def listByStatus(status: AccountStatus): ZIO[Any, AccountError, Seq[Account]] =
    query.findAllByStatus(status).map(_.map(_.account))

  def createAccount(account: Account): ZIO[Any, AccountError, Account] = {
    command.createAccount(account).map(_ => account)
  }
}

object AccountService {

  val live: ZLayer[AccountQueryRepository & AccountCommandRepository, Nothing, AccountService] =
    for {
      command <- ZLayer.service[AccountCommandRepository]
      query <- ZLayer.service[AccountQueryRepository]
    } yield AccountService(command, query)
}
