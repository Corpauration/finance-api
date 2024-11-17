package fr.corpauration.finance
package accounts.query

import accounts.models.*
import accounts.models.AccountError.{ AccountNotFound, CustomError }
import accounts.persistence.AccountEntity
import io.getquill.*
import javax.sql.DataSource
import zio.*

case class AccountQueryRepository(dataSource: DataSource) {
  private val context = PostgresZioJdbcContext(SnakeCase)
  import context.run

  private val accounts = quote {
    querySchema[AccountEntity]("accounts")
  }

  def findOneById(id: AccountId): ZIO[Any, AccountError, AccountEntity] = {
    run(quote { accounts.filter(_.id == id.uuid) })
      .provideEnvironment(ZEnvironment(dataSource))
      .mapError { e => CustomError(e.getMessage) }
      .flatMap { entities => ZIO.getOrFailWith(AccountNotFound(id))(entities.headOption) }
  }

  def findAllByStatus(status: AccountStatus): ZIO[Any, AccountError, Seq[AccountEntity]] = {
    run(quote { accounts.filter(_.status == status) })
      .provideEnvironment(ZEnvironment(dataSource))
      .mapError(e => CustomError(e.getMessage))
  }
}

object AccountQueryRepository {

  val live: ZLayer[DataSource, Nothing, AccountQueryRepository] = ZLayer.fromFunction(ds => AccountQueryRepository(ds))
}
