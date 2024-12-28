package fr.corpauration.finance
package accounts.models

import accounts.models.id.AccountId
import common.types.cents.Cents

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

import java.util.UUID
import scala.util.Try

case class Account(
    id: AccountId,
    ownerId: String,
    metadata: AccountMetadata,
    maxDebtAllowed: Cents,
    balance: Cents,
    status: AccountStatus)

enum AccountStatus {
  case ACTIVE, DEACTIVATED, DELETED
}

object AccountStatus {

  def safeValueOf(s: String): Option[AccountStatus] =
    Try {
      AccountStatus.valueOf(s)
    }.toOption
}

case class AccountMetadata(
    name: String,
    description: String,
    tag: Seq[String],
    labels: Map[String, String])
    derives Encoder.AsObject,
      Decoder,
      Schema

package id {
  opaque type AccountId = UUID

  object AccountId {
    def apply(uuid: UUID): AccountId = uuid

    extension (accountId: AccountId) {
      def uuid: UUID = accountId
    }
  }
}
