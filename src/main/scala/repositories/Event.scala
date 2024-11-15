package fr.corpauration.finance
package repositories

import java.time.OffsetDateTime
import java.util.UUID

import cats.implicits.catsSyntaxEither
import doobie.{ Meta, Read, Write }
import doobie.implicits.*
import doobie.postgres.implicits.*
import fr.corpauration.finance.accounts.AccountMetadata
import fr.corpauration.finance.accounts.id.{ uuid, AccountId }
import fr.corpauration.finance.events.AccountEvent
import fr.corpauration.finance.repositories.Event.toAccountEvent
import fr.corpauration.finance.types.cents.Cents
import io.circe.{ Decoder, DecodingFailure, Encoder, JsonObject }
import io.circe.derivation.Configuration
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.postgresql.util.PGobject

private[repositories] case class Event(
    id: UUID,
    timestamp: OffsetDateTime,
    data: JsonObject) {
  def accountEvent: Either[DecodingFailure, AccountEvent] = toAccountEvent(this)
}

object Event {

  given Meta[JsonObject] =
    Meta.Advanced
      .other[PGobject]("jsonb")
      .timap[JsonObject](a => decode[JsonObject](a.getValue).leftMap[JsonObject](e => throw e).merge)(a => {
        val o = new PGobject
        o.setType("jsonb")
        o.setValue(a.toJson.noSpaces)
        o
      })

  given Read[Event] = Read[(UUID, OffsetDateTime, JsonObject)].map {
    case (accountId, timestamp, data) => Event(accountId, timestamp, data)
  }

  given Write[Event] = Write[(UUID, OffsetDateTime, JsonObject)].contramap {
    case Event(accountId, timestamp, data) => (accountId, timestamp, data)
  }

  def apply(accountEvent: AccountEvent): Event = {
    import AccountEventEntity.given
    val entity = AccountEventEntity(accountEvent)
    new Event(
      id = accountEvent.id.uuid,
      timestamp = accountEvent.timestamp,
      data = entity.asJsonObject
    )
  }

  def toAccountEvent(event: Event): Either[DecodingFailure, AccountEvent] = {
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

  given Configuration = Configuration.default.withDiscriminator("_type_hint")

  given encoder: Encoder.AsObject[AccountEventEntity] = Encoder.AsObject.derivedConfigured[AccountEventEntity]

  given decoder: Decoder[AccountEventEntity] = Decoder.derivedConfigured[AccountEventEntity]
}
