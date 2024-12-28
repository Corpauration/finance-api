package fr.corpauration.finance
package accounts.models

import fr.corpauration.finance.accounts.models.id.AccountId
import sttp.model.StatusCode

enum AccountError {
  case AccountAlreadyCreated(id: AccountId)
  case AccountNotFound(id: AccountId)
  case DeserializationError(id: AccountId, reason: String)
  case CustomError(reason: String)

  def message: String = this match {
    case AccountAlreadyCreated(id)        => s"The account $id already exists"
    case AccountNotFound(id)              => s"The account $id is not found"
    case DeserializationError(id, reason) => ???
    case CustomError(reason)              => s"Unknown error: $reason"
  }

  def status: StatusCode =
    this match {
      case _: AccountAlreadyCreated => StatusCode.Conflict
      case _: AccountNotFound       => StatusCode.NotFound
      case _: CustomError           => StatusCode.InternalServerError
      case _: DeserializationError  => StatusCode.InternalServerError
    }
}
