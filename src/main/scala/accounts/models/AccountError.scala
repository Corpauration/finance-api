package fr.corpauration.finance
package accounts.models

import accounts.models.AccountId
import sttp.model.StatusCode

enum AccountError {
  case AccountAlreadyCreated(id: AccountId)
  case AccountNotFound(id: AccountId)
  case CustomError(reason: String)

  def message: String = this match {
    case AccountAlreadyCreated(id) => s"The account $id already exists"
    case AccountNotFound(id)       => s"The account $id is not found"
    case CustomError(reason)       => s"Unknown error: $reason"
  }

  def status: StatusCode =
    this match {
      case _: AccountAlreadyCreated => StatusCode.Conflict
      case _: AccountNotFound       => StatusCode.NotFound
      case _: CustomError           => StatusCode.InternalServerError
    }
}
