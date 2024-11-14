package fr.corpauration.finance
package errors

import accounts.id.AccountId

enum AccountError {
  case AccountAlreadyCreated(id: AccountId)
  case AccountNotFound(id: AccountId)
  case CustomError(reason: String)
}