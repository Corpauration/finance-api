package fr.corpauration.finance
package accounts.persistence

import java.time.OffsetDateTime

import accounts.models.*
import common.Event
import common.types.cents.Cents
import io.circe.{ Decoder, DecodingFailure, Encoder }
import io.circe.derivation.Configuration
import io.circe.syntax.*

enum AccountEventEntity {

  case AccountCreatedEvent(
      ownerId: String,
      metadata: AccountMetadata,
      maxDebtAllowed: Cents,
      balance: Cents)

  case MetadataUpdatedEvent(
      metadata: AccountMetadata)

  case MaxDebtAllowedUpdated(
      maxDebtAllowed: Cents,
      reason: String,
      labels: Map[String, String],
      timestamp: OffsetDateTime)

  case BalanceDecreased(
      amount: Cents,
      reason: String,
      labels: Map[String, String])

  case BalanceIncreased(
      amount: Cents,
      reason: String,
      labels: Map[String, String])

  case AccountDeactivated(
      reason: String,
      labels: Map[String, String])

  case AccountReactivated(
      reason: String,
      labels: Map[String, String])
}

object AccountEventEntity {

  def apply(accountEvent: AccountEvent): AccountEventEntity =
    accountEvent match {
      case AccountEvent.AccountCreatedEvent(id, ownerId, metadata, maxDebtAllowed, balance, timestamp) =>
        AccountEventEntity.AccountCreatedEvent(ownerId, metadata, maxDebtAllowed, balance)
      case AccountEvent.MetadataUpdatedEvent(id, metadata, timestamp) =>
        AccountEventEntity.MetadataUpdatedEvent(metadata)
      case AccountEvent.MaxDebtAllowedUpdated(id, maxDebtAllowed, reason, labels, timestamp) =>
        AccountEventEntity.MaxDebtAllowedUpdated(maxDebtAllowed, reason, labels, timestamp)
      case AccountEvent.BalanceDecreased(id, amount, reason, labels, timestamp) =>
        AccountEventEntity.BalanceDecreased(amount, reason, labels)
      case AccountEvent.BalanceIncreased(id, amount, reason, labels, timestamp) =>
        AccountEventEntity.BalanceIncreased(amount, reason, labels)
      case AccountEvent.AccountDeactivated(id, reason, labels, timestamp) =>
        AccountEventEntity.AccountReactivated(reason, labels)
      case AccountEvent.AccountReactivated(id, reason, labels, timestamp) =>
        AccountEventEntity.AccountReactivated(reason, labels)
    }

  extension (event: Event) {

    def accountEvent: Either[DecodingFailure, AccountEvent] = {
      val Event(uuid, timestamp, data) = event
      val accountId = AccountId(uuid)
      event.data.toJson.as[AccountEventEntity].map {
        case AccountEventEntity.AccountCreatedEvent(ownerId, metadata, maxDebtAllowed, balance) =>
          AccountEvent.AccountCreatedEvent(
            accountId,
            ownerId,
            metadata,
            maxDebtAllowed,
            balance,
            timestamp
          )
        case AccountEventEntity.MetadataUpdatedEvent(metadata) =>
          AccountEvent.MetadataUpdatedEvent(
            accountId,
            metadata,
            timestamp
          )
        case AccountEventEntity.MaxDebtAllowedUpdated(maxDebtAllowed, reason, labels, timestamp) =>
          AccountEvent.MaxDebtAllowedUpdated(
            accountId,
            maxDebtAllowed,
            reason,
            labels,
            timestamp
          )
        case AccountEventEntity.BalanceDecreased(amount, reason, labels) =>
          AccountEvent.BalanceDecreased(
            accountId,
            amount,
            reason,
            labels,
            timestamp
          )
        case AccountEventEntity.BalanceIncreased(amount, reason, labels) =>
          AccountEvent.BalanceIncreased(
            accountId,
            amount,
            reason,
            labels,
            timestamp
          )
        case AccountEventEntity.AccountDeactivated(reason, labels) =>
          AccountEvent.AccountDeactivated(
            accountId,
            reason,
            labels,
            timestamp
          )
        case AccountEventEntity.AccountReactivated(reason, labels) =>
          AccountEvent.AccountReactivated(
            accountId,
            reason,
            labels,
            timestamp
          )
      }
    }
  }

  given Configuration = Configuration.default.withDiscriminator("_type_hint")

  given encoder: Encoder.AsObject[AccountEventEntity] = Encoder.AsObject.derivedConfigured[AccountEventEntity]

  given decoder: Decoder[AccountEventEntity] = Decoder.derivedConfigured[AccountEventEntity]

  extension (accountEvent: AccountEvent) {

    def event: Event = {
      import AccountEventEntity.given
      val entity = AccountEventEntity(accountEvent)
      new Event(
        id = accountEvent.id.uuid,
        timestamp = accountEvent.timestamp,
        data = entity.asJsonObject
      )
    }
  }
}
