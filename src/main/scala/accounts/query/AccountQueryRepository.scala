package fr.corpauration.finance
package accounts.query

import accounts.models.*
import accounts.models.AccountError.{ AccountNotFound, CustomError }
import accounts.persistence.AccountEntity
import cats.effect.IO as CatsIO
import cats.effect.kernel.Resource
import common.utils.helper.mapError
import doobie.Transactor
import doobie.implicits.*
import doobie.postgres.implicits.*
import kyo.*

case class AccountQueryRepository(db: Resource[CatsIO, Transactor[CatsIO]]) {

  def findOneById(id: AccountId): AccountEntity < (Abort[AccountError] & Async) = {
    val query =
      sql"""
        SELECT a.id, a.owner_id, a.name, a.description, a.tags, a.labels, a.max_debt_allowed, a.balance, a.status
        FROM account a
        WHERE a.id = ${id.uuid}
         """.query[AccountEntity].option

    val ioEither: CatsIO[Either[Throwable, Maybe[AccountEntity]]] =
      db.use(query.transact).map(Maybe.fromOption).attempt

    Cats
      .get(ioEither)
      .map(_ ?=>
        _.fold(
          error => Abort.fail[AccountError](CustomError(error.getMessage)),
          opt => Abort.get(opt).mapError[AccountError](_ => AccountNotFound(id))
        )
      )
  }
}
