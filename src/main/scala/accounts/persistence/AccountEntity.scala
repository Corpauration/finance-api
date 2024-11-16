package fr.corpauration.finance
package accounts.persistence

import accounts.models.*
import doobie.postgres.implicits.*
import doobie.{Meta, Read, Write}
import io.circe.Json

import java.util.UUID

case class AccountEntity(
    id: UUID,
    ownerId: String,
    name: String,
    description: String,
    tags: List[String],
    labels: Json,
    maxDebtAllowed: Long,
    balance: Long,
    status: AccountStatus)

object AccountEntity {
  given Meta[AccountStatus] = pgEnumString[AccountStatus]("status", AccountStatus.valueOf, _.toString)

  def apply(account: Account): AccountEntity = {
    AccountEntity(
      id = account.id.uuid,
      ownerId = account.ownerId,
      name = account.metadata.name,
      description = account.metadata.description,
      tags = account.metadata.tag.toList,
      labels = Json.fromFields(
        account.metadata.labels.map { case (key, value) => key -> Json.fromString(value) }
      ),
      maxDebtAllowed = account.maxDebtAllowed.value,
      balance = account.balance.value,
      status = account.status
    )
  }
}
