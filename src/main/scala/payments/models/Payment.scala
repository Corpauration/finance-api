package fr.corpauration.finance
package payments.models

import java.time.OffsetDateTime
import java.util.UUID

import accounts.models.AccountId
import common.types.cents.Cents
import payments.models.*

case class Payment(
    id: PaymentId,
    author: String,
    metadata: PaymentMetadata,
    amount: Cents,
    method: PaymentMethod,
    status: PaymentStatus)

case class PaymentMetadata(
    name: String,
    source: String,
    description: String,
    tag: Seq[String],
    labels: Map[String, String])

enum PaymentMethod {
  case Cash
  case Balance(accountId: AccountId)
}

enum PaymentStatus {
  case PENDING_VALIDATION, CANCELLED, PAID
}

opaque type PaymentId = UUID

object PaymentId {
  def apply(uuid: UUID): PaymentId = uuid

  extension (id: PaymentId) {
    def uuid: UUID = id
  }
}
