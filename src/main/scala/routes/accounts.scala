package fr.corpauration.finance
package routes

import fr.corpauration.finance.accounts.{Account, AccountMetadata, AccountStatus}
import io.circe.{Decoder, Encoder}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

import java.util.UUID

object accounts {
  given Schema[AccountStatus] = Schema.derivedEnumeration.defaultStringBased

  val statusQuery =
    query[Option[AccountStatus]]("query")
      .description("Filter by account status")
      .default(None)
      .example(Some(AccountStatus.DEACTIVATED))
      .map(_.getOrElse(AccountStatus.ACTIVE))(Option.apply)

  case class AccountCreationPayload(
    ownerId: String,
    name: String,
    description: String,
    tag: Seq[String],
    labels: Map[String, String],
    maxDebtAllowed: Long,
    balance: Long,
  )derives Encoder.AsObject, Decoder, Schema

  case class AccountOutput(
    id: UUID,
    ownerId: String,
    metadata: AccountMetadata,
    maxDebtAllowed: Long,
    balance: Long,
    status: String,
  )derives Encoder.AsObject, Decoder, Schema

  val createAccount =
    endpoint.post
      .name("createAccount")
      .description("Create an account")
      .tags(List("finance", "account", "v1.0"))
      .in("v1.0" / "finance" / "accounts")
      .in(jsonBody[AccountCreationPayload])
      .out(jsonBody[AccountOutput] and statusCode(StatusCode.Created))
      .errorOut(statusCode and stringBody)

  val getAccounts =
    endpoint.get
      .name("getAccounts")
      .description("Retrieves all accounts")
      .tags(List("finance", "account", "v1.0"))
      .in("v1.0" / "finance" / "accounts")
      .in(statusQuery)
      .out(jsonBody[List[AccountOutput]] and statusCode(StatusCode.Ok))
      .errorOut(statusCode and stringBody)

  val getAccount =
    endpoint.get
      .name("getAccount")
      .description("Retrieves an accounts by id")
      .tags(List("finance", "account", "v1.0"))
      .in("v1.0" / "finance" / "accounts" / path[UUID]("accountId"))
      .in(statusQuery)
      .out(jsonBody[AccountOutput] and statusCode(StatusCode.Ok))
      .errorOut(statusCode and stringBody)

}
