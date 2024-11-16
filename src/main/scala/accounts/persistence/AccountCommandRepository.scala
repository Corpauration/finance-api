package fr.corpauration.finance
package accounts.persistence

import java.time.OffsetDateTime

import accounts.models.*
import accounts.models.AccountError.{ AccountNotFound, CustomError }
import accounts.models.AccountEvent.*
import accounts.models.AccountId.uuid
import accounts.persistence.AccountEventEntity.{ accountEvent, event }
import cats.effect.IO as CatsIO
import cats.effect.kernel.Resource
import common.Event
import common.types.cents.Cents
import common.utils.helper.{ asKyo, mapError }
import doobie.{ ConnectionIO, Transactor }
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.fragments
import kyo.*

case class AccountCommandRepository(db: Resource[CatsIO, Transactor[CatsIO]]) {

  private def insertEventCIO(accountEvent: AccountEvent): ConnectionIO[Unit] = {
    val event: Event = accountEvent.event
    val sql =
      sql"""
          |INSERT INTO account_events("id", "timestamp", "data")
          |VALUES""".stripMargin
    val query = sql ++ fragments.values(event)
    query.update.run.map(_ => ())
  }

  private def insertAccountCIO(account: Account): ConnectionIO[Unit] = {
    val sql =
      sql"""
          |INSERT INTO accounts(id, owner_id, name, description, tags, labels, max_debt_allowed, balance, status)
          |VALUES
          |""".stripMargin

    val query = sql ++ fragments.values(AccountEntity(account))
    query.update.run.map(_ => ())
  }

  private def updateAccountCIO(account: Account): ConnectionIO[Unit] = {
    val entity = AccountEntity(account)
    import entity.*
    val query = fr"""UPDATE accounts""" ++
      fragments.set(
        fr"id = $id",
        fr"owner_id = $ownerId",
        fr"name = $name",
        fr"description = $description",
        fr"tags = $tags",
        fr"labels = $labels",
        fr"max_debt_allowed = $maxDebtAllowed",
        fr"balance = $balance",
        fr"status = $status"
      ) ++ fr"WHERE id = $id"
    query.update.run.map(_ => ())
  }

  private def insertEvent(event: AccountEvent)(account: Account): Unit < (Abort[AccountError] & Async) = {
    val insertOrUpdate = {
      if event.isInstanceOf[AccountCreatedEvent] then insertAccountCIO(account)
      else updateAccountCIO(account)
    }

    val query = for {
      _ <- insertEventCIO(event)
      _ <- insertOrUpdate
    } yield ()

    db.use(query.transact).attempt.asKyo.mapError(e => CustomError(e.getMessage))
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
    )(account)

  def updateMetadata(
      id: AccountId,
      metadata: AccountMetadata
    )(account: Account
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      MetadataUpdatedEvent(
        id = id,
        metadata = metadata,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield account

  def updateMaximumDebtAllowed(
      id: AccountId,
      maxDebtAllowed: Cents,
      reason: String,
      labels: Map[String, String]
    )(account: Account
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      MaxDebtAllowedUpdated(
        id = id,
        maxDebtAllowed = maxDebtAllowed,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield account

  def increaseBalance(
      id: AccountId,
      amount: Cents,
      reason: String,
      labels: Map[String, String]
    )(account: Account
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      BalanceIncreased(
        id = id,
        amount = amount,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield account

  def decreaseBalance(
      id: AccountId,
      amount: Cents,
      reason: String,
      labels: Map[String, String]
    )(account: Account
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      BalanceDecreased(
        id = id,
        amount = amount,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield account

  def deactivateAccount(
      id: AccountId,
      reason: String,
      labels: Map[String, String]
    )(account: Account
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      AccountDeactivated(
        id = id,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield account

  def reactivateAccount(
      id: AccountId,
      reason: String,
      labels: Map[String, String]
    )(account: Account
    ): Account < (Abort[AccountError] & Async) = for {
    _ <- insertEvent(
      AccountReactivated(
        id = id,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )(account)
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
