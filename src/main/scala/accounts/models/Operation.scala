package fr.corpauration.finance
package accounts.models

import accounts.models.OperationKind.{CREDIT, DEBIT}
import common.types.cents.Cents

import fr.corpauration.finance.accounts.models.id.AccountId

import java.time.OffsetDateTime
import java.util.UUID

case class Operation(
    id: OperationId,
    accountId: AccountId,
    amount: Cents,
    timestamp: OffsetDateTime,
    labels: Map[String, String],
    kind: OperationKind)

enum OperationKind {
  case CREDIT, DEBIT
}

object Operation {

  def apply(
      accountId: AccountId,
      amount: Cents,
      labels: Map[String, String],
      kind: OperationKind
    ): Operation = Operation(
    id = OperationId.random(),
    accountId = accountId,
    amount = amount,
    timestamp = OffsetDateTime.now(),
    labels = labels,
    kind = kind
  )

  def credit(
      accountId: AccountId,
      amount: Cents,
      labels: Map[String, String]
    ): Operation =
    Operation(accountId, amount, labels, CREDIT)

  def debit(
      accountId: AccountId,
      amount: Cents,
      labels: Map[String, String]
    ): Operation = Operation(accountId, amount, labels, DEBIT)

}

opaque type OperationId = UUID

object OperationId {
  def random(): OperationId = UUID.randomUUID()

  def apply(uuid: UUID): OperationId = uuid

  extension (operationId: OperationId) {
    def uuid: UUID = operationId
  }
}
