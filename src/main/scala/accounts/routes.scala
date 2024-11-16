package fr.corpauration.finance
package accounts

import java.util.UUID

import accounts.models.*
import accounts.models.AccountId.uuid
import common.types.cents.value
import io.circe.{ Decoder, Encoder }
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object routes {
  given Schema[AccountStatus] = Schema.derivedEnumeration.defaultStringBased

  given Codec[String, AccountStatus, CodecFormat.TextPlain] =
    Codec.string.mapDecode(AccountStatus.safeValueOf andThen DecodeResult.fromOption)(_.toString)

  given Codec[List[String], Option[AccountStatus], CodecFormat.TextPlain] =
    Codec.listHeadOption[String, AccountStatus, CodecFormat.TextPlain]

  private val statusQuery: EndpointInput[AccountStatus] =
    query[Option[AccountStatus]]("status")
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
      balance: Long)
      derives Encoder.AsObject,
        Decoder,
        Schema

  case class AccountOutput(
      id: UUID,
      ownerId: String,
      metadata: AccountMetadata,
      maxDebtAllowed: Long,
      balance: Long,
      status: String)
      derives Encoder.AsObject,
        Decoder,
        Schema

  object AccountOutput {

    def apply(account: Account): AccountOutput =
      AccountOutput(
        id = account.id.uuid,
        ownerId = account.ownerId,
        metadata = account.metadata,
        maxDebtAllowed = account.maxDebtAllowed.value,
        balance = account.balance.value,
        status = account.status.toString
      )
  }

  case class HttpError(status: StatusCode, message: String)

  val createAccount: Endpoint[Unit, AccountCreationPayload, HttpError, AccountOutput, Any] =
    endpoint.post
      .name("createAccount")
      .description("Create an account")
      .tags(List("finance", "account", "v1.0"))
      .in("v1.0" / "finance" / "accounts")
      .in(jsonBody[AccountCreationPayload])
      .out(jsonBody[AccountOutput] and statusCode(StatusCode.Created))
      .errorOut(statusCode and stringBody)
      .mapErrorOutTo[HttpError]

  val listAccounts: Endpoint[Unit, AccountStatus, HttpError, List[AccountOutput], Any] =
    endpoint.get
      .name("listAccounts")
      .description("Retrieves all accounts")
      .tags(List("finance", "account", "v1.0"))
      .in("v1.0" / "finance" / "accounts")
      .in(statusQuery)
      .out(jsonBody[List[AccountOutput]] and statusCode(StatusCode.Ok))
      .errorOut(statusCode and stringBody)
      .mapErrorOutTo[HttpError]

  val getAccount: Endpoint[Unit, UUID, HttpError, AccountOutput, Any] =
    endpoint.get
      .name("getAccount")
      .description("Retrieves an accounts by id")
      .tags(List("finance", "account", "v1.0"))
      .in("v1.0" / "finance" / "accounts" / path[UUID]("accountId"))
      .out(jsonBody[AccountOutput] and statusCode(StatusCode.Ok))
      .errorOut(statusCode and stringBody)
      .mapErrorOutTo[HttpError]

}
