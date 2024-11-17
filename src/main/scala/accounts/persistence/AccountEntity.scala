package fr.corpauration.finance
package accounts.persistence

import accounts.models.*
import common.types.cents.Cents

import io.circe.Json

import java.util.UUID

case class AccountEntity(
    id: UUID,
    ownerId: String,
    name: String,
    description: String,
    tags: List[String],
    labels: Map[String, String],
    maxDebtAllowed: Long,
    balance: Long,
    status: AccountStatus) {

  def account: Account = Account(
    id = AccountId(id),
    ownerId = ownerId,
    metadata = AccountMetadata(
      name = name,
      description = description,
      tag = tags,
      labels = labels
    ),
    maxDebtAllowed = Cents(maxDebtAllowed),
    balance = Cents(balance),
    status = status
  )
}

object AccountEntity {
  def apply(account: Account): AccountEntity = {
    AccountEntity(
      id = account.id.uuid,
      ownerId = account.ownerId,
      name = account.metadata.name,
      description = account.metadata.description,
      tags = account.metadata.tag.toList,
      labels = account.metadata.labels,
      maxDebtAllowed = account.maxDebtAllowed.value,
      balance = account.balance.value,
      status = account.status
    )
  }
}
