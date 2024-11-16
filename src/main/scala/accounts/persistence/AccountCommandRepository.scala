package fr.corpauration.finance
package accounts.persistence

import java.time.OffsetDateTime

import accounts.models.{ Account, AccountError, AccountEvent, AccountId, AccountMetadata }
import accounts.models.AccountError.{ AccountNotFound, CustomError }
import accounts.models.AccountEvent.*
import accounts.models.AccountId.uuid
import accounts.persistence.AccountEntity
import accounts.persistence.AccountEventEntity.{ accountEvent, event }
import cats.effect.IO as CatsIO
import cats.effect.kernel.Resource
import cats.implicits.catsStdInstancesForList
import common.Event
import common.types.cents.Cents
import common.utils.helper.mapError
import doobie.Transactor
import doobie.implicits.*
import doobie.postgres.implicits.UuidType
import doobie.util.fragments
import kyo.*

case class AccountCommandRepository(db: Resource[CatsIO, Transactor[CatsIO]]) {

  private def insertEvent(
      accountEvent: AccountEvent
    ): Unit < (Abort[AccountError] & Async) = {
    val event: Event = accountEvent.event
    val sql =
      fr"""
          |INSERT INTO account_events("id", "timestamp", "data")
          |VALUES""".stripMargin
    val query = sql ++ fragments.values(event)
    val k: Unit < (Abort[Throwable] & Async) =
      Cats
        .get(
          db.use(query.update.run.transact).attempt
        )
        .map(_ ?=> e => Abort.get(e))
        .map(_ ?=> _ => ())

    k.catchAbort(e => Abort.fail(CustomError(e.getMessage)))
  }

  def createAccount(
      account: Account
    ): Unit < (Abort[AccountError] & Async) =
    insertEvent(
      AccountCreatedEvent(
        id = account.id,
        ownerId = account.ownerId,
        metadata = account.metadata,
        maxDebtAllowed = account.maxDebtAllowed,
        balance = account.balance,
        timestamp = OffsetDateTime.now()
      )
    )

  def updateMetadata(
      id: AccountId,
      metadata: AccountMetadata
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      MetadataUpdatedEvent(
        id = id,
        metadata = metadata,
        timestamp = OffsetDateTime.now()
      )
    )
    account <- rebuildAccount(id)
  } yield account

  def updateMaximumDebtAllowed(
      id: AccountId,
      maxDebtAllowed: Cents,
      reason: String,
      labels: Map[String, String]
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      MaxDebtAllowedUpdated(
        id = id,
        maxDebtAllowed = maxDebtAllowed,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )
    account <- rebuildAccount(id)
  } yield account

  def increaseBalance(
      id: AccountId,
      amount: Cents,
      reason: String,
      labels: Map[String, String]
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      BalanceIncreased(
        id = id,
        amount = amount,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )
    account <- rebuildAccount(id)
  } yield account

  def decreaseBalance(
      id: AccountId,
      amount: Cents,
      reason: String,
      labels: Map[String, String]
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      BalanceDecreased(
        id = id,
        amount = amount,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )
    account <- rebuildAccount(id)
  } yield account

  def deactivateAccount(
      id: AccountId,
      reason: String,
      labels: Map[String, String]
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      AccountDeactivated(
        id = id,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )
    account <- rebuildAccount(id)
  } yield account

  def reactivateAccount(
      id: AccountId,
      reason: String,
      labels: Map[String, String]
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      AccountReactivated(
        id = id,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )
    account <- rebuildAccount(id)
  } yield account

  def rebuildAccount(
      id: AccountId
    ): Account < (Abort[AccountError] & Async) = {
    val eventStream = sql"""
                           |SELECT e.id, e.data, e.timestamp
                           |FROM account_events
                           |WHERE e.id = ${id.uuid}
                           |ORDER BY e.timestamp ASC
                           |""".stripMargin.query[Event].stream

    val ioEither = db.use(
      eventStream
        .transact[CatsIO](_)
        .evalMap { event =>
          event.accountEvent match {
            case Left(value)  => CatsIO.raiseError(value)
            case Right(value) => CatsIO.pure(value)
          }
        }
        .compile
        .fold(Left(AccountNotFound(id)).withRight[Account])(AccountEvent.applyEvent)
    )

    Cats.get(ioEither).map { either => Abort.get(either) }
  }
}
