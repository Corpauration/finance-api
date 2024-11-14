package fr.corpauration.finance
package accounts

import fr.corpauration.finance.accounts.id.AccountId
import fr.corpauration.finance.types.cents.Cents
import io.circe.{Decoder, Encoder}

import java.util.UUID

package id {
  opaque type AccountId = UUID

  object AccountId {
    def apply(uuid: UUID): AccountId = uuid
  }

  extension (accountId: AccountId) {
    def uuid: UUID = accountId
  }
}

case class Account(
  id: AccountId,
  ownerId: String,
  metadata: AccountMetadata,
  maxDebtAllowed: Cents,
  balance: Cents,
  status: AccountStatus
)

enum AccountStatus {
  case ACTIVE, DEACTIVATED, DELETED
}

case class AccountMetadata(
  name: String,
  description: String,
  tag: Seq[String],
  labels: Map[String, String]
) derives Encoder.AsObject, Decoder

