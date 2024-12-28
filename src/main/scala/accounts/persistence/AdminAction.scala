package fr.corpauration.finance
package accounts.persistence

import common.types.cents.Cents

import fr.corpauration.finance.accounts.models.id.AccountId
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

import java.time.OffsetDateTime
import java.util.UUID

private case class MaxDebtAmount(amount: Cents)

object MaxDebtAmount {
  given Encoder[MaxDebtAmount] = Encoder.AsObject.derived
  given Decoder[MaxDebtAmount] = Decoder.derived
}

enum AdminAction {

  case MaxDebtUpdated(
      id: UUID,
      accountId: AccountId,
      timestamp: OffsetDateTime,
      maxDebt: Cents,
      reason: String,
      labels: Map[String, String])

  case AccountDeactivated(
      id: UUID,
      accountId: AccountId,
      timestamp: OffsetDateTime,
      reason: String,
      labels: Map[String, String])

  case AccountReactivated(
      id: UUID,
      accountId: AccountId,
      timestamp: OffsetDateTime,
      reason: String,
      labels: Map[String, String])
}

object AdminAction {

  def maxDebtUpdated(
      id: AccountId,
      maxDebt: Cents,
      reason: String,
      labels: Map[String, String] = Map.empty
    ): AdminAction =
    MaxDebtUpdated(
      id = UUID.randomUUID(),
      accountId = id,
      timestamp = OffsetDateTime.now(),
      maxDebt = maxDebt,
      reason = reason,
      labels = labels
    )

  def accountDeactivated(id: AccountId, reason: String, labels: Map[String, String] = Map.empty): AdminAction =
    AccountDeactivated(
      id = UUID.randomUUID(),
      accountId = id,
      timestamp = OffsetDateTime.now(),
      reason = reason,
      labels = labels
    )

  def accountReactivated(id: AccountId, reason: String, labels: Map[String, String] = Map.empty): AdminAction =
    AccountReactivated(
      id = UUID.randomUUID(),
      accountId = id,
      timestamp = OffsetDateTime.now(),
      reason = reason,
      labels = labels
    )
}

case class AdminActionEntity(
    id: UUID,
    accountId: AccountId,
    timestamp: OffsetDateTime,
    reason: String,
    labels: Map[String, String],
    kind: String,
    data: Json) {

  import fr.corpauration.finance.accounts.persistence.AdminAction.*

  def action: Option[AdminAction] = kind match {
    case "MaxDebtUpdated" =>
      data.as[MaxDebtAmount].toOption.map {
        case MaxDebtAmount(amount) =>
          MaxDebtUpdated(
            id = id,
            accountId = accountId,
            timestamp = timestamp,
            maxDebt = amount,
            reason = reason,
            labels = labels
          )
      }
    case "AccountDeactivated" =>
      Some(AccountDeactivated(id = id, accountId = accountId, timestamp = timestamp, reason = reason, labels = labels))
    case "AccountReactivated" =>
      Some(AccountDeactivated(id = id, accountId = accountId, timestamp = timestamp, reason = reason, labels = labels))
    case _ => None
  }
}

object AdminActionEntity {
  import fr.corpauration.finance.accounts.persistence.AdminAction.*

  def apply(action: AdminAction): AdminActionEntity = action match {
    case MaxDebtUpdated(id, accountId, timestamp, maxDebt, reason, labels) =>
      AdminActionEntity(
        id = id,
        accountId = accountId,
        timestamp = timestamp,
        reason = reason,
        labels = labels,
        kind = "MaxDebtUpdated",
        data = MaxDebtAmount(maxDebt).asJson
      )
    case AccountDeactivated(id, accountId, timestamp, reason, labels) =>
      AdminActionEntity(
        id = id,
        accountId = accountId,
        timestamp = timestamp,
        reason = reason,
        labels = labels,
        kind = "AccountDeactivated",
        data = Json.Null
      )
    case AccountReactivated(id, accountId, timestamp, reason, labels) =>
      AdminActionEntity(
        id = id,
        accountId = accountId,
        timestamp = timestamp,
        reason = reason,
        labels = labels,
        kind = "AccountReactivated",
        data = Json.Null
      )
  }

  extension (action: AdminAction) {
    def entity: AdminActionEntity = AdminActionEntity(action)
  }
}
