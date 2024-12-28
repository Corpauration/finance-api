package fr.corpauration.finance
package accounts.persistence

import accounts.models.{Account, AccountMetadata, AccountStatus}
import common.types.cents.Cents

import fr.corpauration.finance.accounts.models.id.AccountId

case class AccountEntity(
    id: AccountId,
    ownerId: String,
    name: String,
    description: String,
    tag: Seq[String],
    labels: Map[String, String],
    maxDebtAllowed: Cents,
    balance: Cents,
    status: AccountStatus) {

  def account: Account = Account(
    id = id,
    ownerId = ownerId,
    metadata = AccountMetadata(
      name = name,
      description = description,
      tag = tag,
      labels = labels
    ),
    maxDebtAllowed = maxDebtAllowed,
    balance = balance,
    status = status
  )
}

object AccountEntity {

  def apply(account: Account): AccountEntity = AccountEntity(
    id = account.id,
    ownerId = account.ownerId,
    name = account.metadata.name,
    description = account.metadata.description,
    tag = account.metadata.tag,
    labels = account.metadata.labels,
    maxDebtAllowed = account.maxDebtAllowed,
    balance = account.balance,
    status = account.status
  )

  extension (account: Account) {
    def entity: AccountEntity = AccountEntity(account)
  }
}
