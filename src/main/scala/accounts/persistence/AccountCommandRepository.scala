package fr.corpauration.finance
package accounts.persistence

import java.time.OffsetDateTime

import accounts.models.*
import accounts.models.AccountError.{ AccountNotFound, CustomError }
import accounts.models.AccountEvent.*
import accounts.models.AccountId.uuid
import accounts.persistence.AccountEventEntity.{ accountEvent, event }
import common.Event
import common.types.cents.Cents
import io.getquill.*
import javax.sql.DataSource
import zio.*

case class AccountCommandRepository(datasource: DataSource) {
  private val context = PostgresZioJdbcContext(SnakeCase)
  import context.{ insertValue, run, transaction, updateValue }

  private val account_events = quote {
    querySchema[Event]("account_events", _.id -> "id", _.timestamp -> "timestamp", _.data -> "data")
  }

  def transact[A](zio: ZIO[DataSource, Throwable, AccountEntity]): ZIO[Any, AccountError, Account] =
    transaction(zio)
      .map(_.account)
      .mapError { throwable => CustomError(throwable.getMessage) }
      .provideEnvironment(ZEnvironment(datasource))

  def insertEvent(
      accountEvent: AccountEvent
    )(account: Account
    ): ZIO[Any, AccountError, Account] = {
    val event = accountEvent.event
    val upsertQuery =
      if event.isInstanceOf[AccountCreatedEvent] then quote {
        query[AccountEntity].insertValue(AccountEntity(account)).returning(identity)
      }
      else
        quote {
          query[AccountEntity].updateValue(AccountEntity(account)).returning(identity)
        }
    val eventQuery = quote(account_events.insertValue(event))

    transaction {
      run(eventQuery) *> run(upsertQuery)
    }.map(_.account).mapError(throwable => CustomError(throwable.getMessage))
  }

  def createAccount(
      account: Account
    ): ZIO[Any, AccountError, Unit] =
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
    ): ZIO[Any, AccountError, Account] = for {
    a <- insertEvent(
      MetadataUpdatedEvent(
        id = id,
        metadata = metadata,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield a

  def updateMaximumDebtAllowed(
      id: AccountId,
      maxDebtAllowed: Cents,
      reason: String,
      labels: Map[String, String]
    )(account: Account
    ): ZIO[Any, AccountError, Account] = for {
    a <- insertEvent(
      MaxDebtAllowedUpdated(
        id = id,
        maxDebtAllowed = maxDebtAllowed,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield a

  def increaseBalance(
      id: AccountId,
      amount: Cents,
      reason: String,
      labels: Map[String, String]
    )(account: Account
    ): ZIO[Any, AccountError, Account] = for {
    a <- insertEvent(
      BalanceIncreased(
        id = id,
        amount = amount,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield a

  def decreaseBalance(
      id: AccountId,
      amount: Cents,
      reason: String,
      labels: Map[String, String]
    )(account: Account
    ): ZIO[Any, AccountError, Account] = for {
    a <- insertEvent(
      BalanceDecreased(
        id = id,
        amount = amount,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield a

  def deactivateAccount(
      id: AccountId,
      reason: String,
      labels: Map[String, String]
    )(account: Account
    ): ZIO[Any, AccountError, Account] = for {
    a <- insertEvent(
      AccountDeactivated(
        id = id,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield a

  def reactivateAccount(
      id: AccountId,
      reason: String,
      labels: Map[String, String]
    )(account: Account
    ): ZIO[Any, AccountError, Account] = for {
    a <- insertEvent(
      AccountReactivated(
        id = id,
        reason = reason,
        labels = labels,
        timestamp = OffsetDateTime.now()
      )
    )(account)
  } yield a

  def rebuildAccount(id: AccountId): ZIO[Any, AccountError, Account] = {
    val q = quote {
      account_events.filter(_.streamId == id.uuid).sortBy(_.timestamp)
    }

    context
      .stream(q)
      .provideEnvironment(ZEnvironment(datasource))
      .mapZIO(event => ZIO.fromEither(event.accountEvent))
      .runFoldZIO(Left(AccountNotFound(id)).withRight[Account])(AccountEvent.applyEvent)
      .mapError(e => CustomError(e.getMessage))
      .flatMap(ZIO.fromEither)
  }
}

object AccountCommandRepository {

  val live: ZLayer[Datasource, Nothing, AccountCommandRepository] =
    ZLayer.fromFunction { dataSource => AccountCommandRepository(dataSource) }
}
