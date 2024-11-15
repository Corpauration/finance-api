package fr.corpauration.finance
package payments

import fr.corpauration.finance.accounts.id.AccountId
import fr.corpauration.finance.payments.id.PaymentId
import fr.corpauration.finance.types.cents.Cents

import java.time.OffsetDateTime
import java.util.UUID

package id {
  opaque type PaymentId = UUID

  object PaymentId {
    def apply(uuid: UUID): PaymentId = uuid
  }

  extension (accountId: PaymentId) {
    def uuid: UUID = accountId
  }
}

case class Payment(
  id: PaymentId,
  author: String,
  metadata: PaymentMetadata,
  amount: Cents,
  method: PaymentMethod,
  status: PaymentStatus,
)

case class PaymentMetadata(
  name: String,
  source: String,
  description: String,
  tag: Seq[String],
  labels: Map[String, String]
)

enum PaymentMethod {
  case Cash
  case Balance(accountId: AccountId)
}

enum PaymentStatus {
  case PENDING_VALIDATION, CANCELLED, PAID
}
