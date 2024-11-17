package fr.corpauration.finance
package accounts.models

import java.time.OffsetDateTime

import accounts.models.*
import accounts.models.AccountError.AccountAlreadyCreated
import accounts.models.AccountStatus.*
import accounts.persistence.*
import common.Event
import common.types.cents.Cents

trait BaseAccountEvent {
  val id: AccountId
  val timestamp: OffsetDateTime
}

enum AccountEvent extends BaseAccountEvent {

  case AccountCreatedEvent(
      id: AccountId,
      ownerId: String,
      metadata: AccountMetadata,
      maxDebtAllowed: Cents,
      balance: Cents,
      timestamp: OffsetDateTime)

  case MetadataUpdatedEvent(
      id: AccountId,
      metadata: AccountMetadata,
      timestamp: OffsetDateTime)

  case MaxDebtAllowedUpdated(
      id: AccountId,
      maxDebtAllowed: Cents,
      reason: String,
      labels: Map[String, String],
      timestamp: OffsetDateTime)

  case BalanceDecreased(
      id: AccountId,
      amount: Cents,
      reason: String,
      labels: Map[String, String],
      timestamp: OffsetDateTime)

  case BalanceIncreased(
      id: AccountId,
      amount: Cents,
      reason: String,
      labels: Map[String, String],
      timestamp: OffsetDateTime)

  case AccountDeactivated(
      id: AccountId,
      reason: String,
      labels: Map[String, String],
      timestamp: OffsetDateTime)

  case AccountReactivated(
      id: AccountId,
      reason: String,
      labels: Map[String, String],
      timestamp: OffsetDateTime)
}

object AccountEvent {

  def applyEvent(account: Either[AccountError, Account], event: AccountEvent): Either[AccountError, Account] =
    event match {
      case AccountCreatedEvent(id, ownerId, metadata, maxDebtAllowed, balance, _) =>
        onCreated(account)(id, ownerId, metadata, maxDebtAllowed, balance)
      case e: MetadataUpdatedEvent =>
        onMetadataUpdated(account)(e.metadata)
      case e: MaxDebtAllowedUpdated =>
        onMaxDebtAllowedUpdated(account)(e.maxDebtAllowed)
      case e: BalanceDecreased =>
        onBalanceDecreased(account)(e.amount)
      case e: BalanceIncreased =>
        onBalanceIncreased(account)(e.amount)
      case e: AccountDeactivated =>
        onAccountDeactivated(account)
      case e: AccountReactivated =>
        onAccountReactivated(account)
    }

  private def onCreated(
      account: Either[AccountError, Account]
    )(id: AccountId,
      ownerId: String,
      metadata: AccountMetadata,
      maxDebtAllowed: Cents,
      balance: Cents
    ): Either[AccountError, Account] = {
    account match {
      case Left(AccountError.AccountNotFound(notFoundId)) if notFoundId == id =>
        Right(
          Account(
            id = id,
            ownerId = ownerId,
            metadata = metadata,
            maxDebtAllowed = maxDebtAllowed,
            balance = balance,
            status = ACTIVE
          )
        )

      case _ => Left(AccountAlreadyCreated(id))
    }
  }

  private def onMetadataUpdated(
      account: Either[AccountError, Account]
    )(metadata: AccountMetadata
    ): Either[AccountError, Account] =
    account.map(_.copy(metadata = metadata))

  private def onMaxDebtAllowedUpdated(
      account: Either[AccountError, Account]
    )(maxDebtAllowedUpdated: Cents
    ): Either[AccountError, Account] = account.map(_.copy(maxDebtAllowed = maxDebtAllowedUpdated))

  private def onBalanceDecreased(
      account: Either[AccountError, Account]
    )(balance: Cents
    ): Either[AccountError, Account] = account.map {
    acc =>
      acc.copy(
        balance = Cents(acc.balance.value - balance.value)
      )
  }

  private def onBalanceIncreased(
      account: Either[AccountError, Account]
    )(balance: Cents
    ): Either[AccountError, Account] = account.map {
    acc =>
      acc.copy(
        balance = Cents(acc.balance.value + balance.value)
      )
  }

  private def onAccountDeactivated(account: Either[AccountError, Account]): Either[AccountError, Account] =
    account.map(_.copy(status = DEACTIVATED))

  private def onAccountReactivated(account: Either[AccountError, Account]): Either[AccountError, Account] =
    account.map(_.copy(status = ACTIVE))

}
