package fr.corpauration.finance
package accounts.models

import java.util.UUID
import scala.util.Try
import common.types.cents.Cents

import doobie.Meta
import doobie.postgres.implicits.pgEnumStringOpt
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

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
  given Meta[AccountStatus] =
    pgEnumStringOpt[AccountStatus]("account_status_type", AccountStatus.safeValueOf, _.toString)
  
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

opaque type AccountId = UUID

object AccountId {
  def apply(uuid: UUID): AccountId = uuid

  extension (accountId: AccountId) {
    def uuid: UUID = accountId
  }
}
