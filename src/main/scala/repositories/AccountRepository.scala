package fr.corpauration.finance
package repositories

import accounts.id.AccountId
import accounts.id.uuid
import accounts.{Account, AccountMetadata}
import types.cents.Cents

import cats.effect.IO as CatsIO
import cats.implicits.catsStdInstancesForList
import cats.effect.kernel.Resource
import doobie.Transactor
import doobie.implicits.*
import doobie.postgres.implicits.UuidType
import doobie.util.fragments
import fr.corpauration.finance.errors.AccountError
import fr.corpauration.finance.errors.AccountError.{AccountNotFound, CustomError}
import fr.corpauration.finance.events.AccountEvent
import fr.corpauration.finance.events.AccountEvent.*
import fr.corpauration.finance.repositories.Event.toAccountEvent
import kyo.*

import java.time.OffsetDateTime

case class AccountRepository(db: Resource[CatsIO, Transactor[CatsIO]]) {
  private def insertEvent(
                           accountEvent: AccountEvent
                         ): Unit < (Abort[AccountError] & Async) = {
    val event: Event = Event(accountEvent)
    val sql =
      fr"""
          |INSERT INTO account_events("id", "timestamp", "data")
          |VALUES""".stripMargin
    val query = sql ++ fragments.values(event)
    val k: Int < (Abort[Throwable] & Async) =
      Cats
        .get(
          db.use(query.update.run.transact)
            .attempt
        ).map(_ ?=> e => Abort.get(e))

    k.catchAbort(
      e => Abort.fail(CustomError(e.getMessage))
    )
  }

  def createAccount(
                     account: Account
                   ): Unit < (Abort[AccountError] & Async) =
    insertEvent(AccountCreatedEvent(
      id = account.id,
      ownerId = account.ownerId,
      metadata = account.metadata,
      maxDebtAllowed = account.maxDebtAllowed,
      balance = account.balance,
      timestamp = OffsetDateTime.now()
    ))

  def updateMetadata(
                      id: AccountId,
                      metadata: AccountMetadata
                    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(MetadataUpdatedEvent(
      id = id,
      metadata = metadata,
      timestamp = OffsetDateTime.now()
    ))
    account <- getAccountById(id)
  } yield account

  def updateMaximumDebtAllowed(
                                id: AccountId,
                                maxDebtAllowed: Cents,
                                reason: String,
                                labels: Map[String, String],
                              ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(MaxDebtAllowedUpdated(
      id = id,
      maxDebtAllowed = maxDebtAllowed,
      reason = reason,
      labels = labels,
      timestamp = OffsetDateTime.now()
    ))
    account <- getAccountById(id)
  } yield account

  def increaseBalance(
                       id: AccountId,
                       amount: Cents,
                       reason: String,
                       labels: Map[String, String],
                     ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(BalanceIncreased(
      id = id,
      amount = amount,
      reason = reason,
      labels = labels,
      timestamp = OffsetDateTime.now()
    ))
    account <- getAccountById(id)
  } yield account

  def decreaseBalance(
                       id: AccountId,
                       amount: Cents,
                       reason: String,
                       labels: Map[String, String],
                     ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(BalanceDecreased(
      id = id,
      amount = amount,
      reason = reason,
      labels = labels,
      timestamp = OffsetDateTime.now()
    ))
    account <- getAccountById(id)
  } yield account

  def deactivateAccount(
                         id: AccountId,
                         reason: String,
                         labels: Map[String, String],
                       ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(AccountDeactivated(
      id = id,
      reason = reason,
      labels = labels,
      timestamp = OffsetDateTime.now()
    ))
    account <- getAccountById(id)
  } yield account

  def reactivateAccount(
                         id: AccountId,
                         reason: String,
                         labels: Map[String, String],
                       ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(AccountReactivated(
      id = id,
      reason = reason,
      labels = labels,
      timestamp = OffsetDateTime.now()
    ))
    account <- getAccountById(id)
  } yield account

  def getAccountById(
                      id: AccountId
                    ): Account < (Abort[AccountError] & Async) =
    val eventStream = sql"""
                           |SELECT e.id, e.data, e.timestamp
                           |FROM account_events
                           |WHERE e.id = ${id.uuid}
                           |ORDER BY e.timestamp ASC
                           |""".stripMargin
      .query[Event]
      .stream

    val ioEither = db.use(eventStream.transact[CatsIO](_).evalMap { event =>
      event.accountEvent match
        case Left(value) => CatsIO.raiseError(value)
        case Right(value) => CatsIO.pure(value)
    }.compile.fold(Left(AccountNotFound(id)).withRight[Account])(AccountEvent.applyEvent))

    Cats.get(ioEither).map { either =>
      Abort.get(either)
    }
}
