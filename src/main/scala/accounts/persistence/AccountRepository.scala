package fr.corpauration.finance
package accounts.persistence

import java.sql.{ SQLException, Types }
import java.util.UUID

import accounts.models.{ AccountMetadata, AccountStatus, Operation }
import accounts.models.AccountStatus.{ ACTIVE, DEACTIVATED }
import accounts.models.id.AccountId
import accounts.models.id.AccountId.uuid
import accounts.persistence.AdminActionEntity.entity
import common.types.cents.Cents
import io.getquill.*
import io.getquill.jdbczio.Quill
import org.postgresql.util.PGobject
import zio.*

case class AccountRepository(context: Quill.Postgres[SnakeCase]) {
  import context.*

  inline given MappedEncoding[UUID, AccountId] = MappedEncoding(AccountId.apply)
  inline given MappedEncoding[AccountId, UUID] = MappedEncoding(_.uuid)

  inline given MappedEncoding[Long, Cents] = MappedEncoding(Cents.apply)
  inline given MappedEncoding[Cents, Long] = MappedEncoding(_.value)

  inline given Encoder[Map[String, String]] = encoder(
    Types.OTHER,
    (index, mapValue, row) => {
      val pgObject = PGobject()
      pgObject.setType("hstore")
      pgObject.setValue(mapValue.map { case k -> v => s"$k=>$v" }.mkString("\n"))
      row.setObject(index, pgObject)
    }
  )

  inline given Decoder[Map[String, String]] =
    decoder(
      row =>
        index => {
          import scala.jdk.CollectionConverters.MapHasAsScala
          row.getObject(index, classOf[java.util.Map[String, String]]).asScala.toMap
        }
    )

  inline given Encoder[AccountStatus] = encoder(
    Types.OTHER,
    (index, status, row) => {
      val pgObject = PGobject()
      pgObject.setType("account_status")
      pgObject.setValue(status.toString)
      row.setObject(index, pgObject)
    }
  )

  inline given Decoder[AccountStatus] =
    decoder(
      row =>
        index => {
          AccountStatus.valueOf(row.getObject(index, classOf[String]))
        }
    )

  def createAccount(accountEntity: AccountEntity): ZIO[Any, SQLException, AccountEntity] = {
    val q = quote {
      query[AccountEntity].insertValue(lift(accountEntity)).returning(x => x)
    }

    run(q)
  }

  def increaseAccountBalance(id: AccountId, amount: Cents): ZIO[Any, Throwable, AccountEntity] = {
    val up = quote {
      query[AccountEntity]
        .filter(_.id == lift(id))
        .update(e => e.balance -> lift(Cents(e.balance.value + amount.value)))
        .returning(x => x)
    }

    val ins = quote {
      query[Operation].insertValue(lift(Operation.credit(id, amount, Map.empty)))
    }

    transaction {
      run(ins) *> run(up)
    }
  }

  def decreaseAccountBalance(id: AccountId, amount: Cents): ZIO[Any, Throwable, AccountEntity] = {
    val up = quote {
      query[AccountEntity]
        .filter(_.id == lift(id))
        .update(e => e.balance -> lift(Cents(e.balance.value - amount.value)))
        .returning(x => x)
    }

    val ins = quote {
      query[Operation].insertValue(lift(Operation.debit(id, amount, Map.empty)))
    }

    transaction {
      run(ins) *> run(up)
    }
  }

  def updateAccountMetadata(id: AccountId, metadata: AccountMetadata): ZIO[Any, SQLException, AccountEntity] = {
    val q = quote {
      query[AccountEntity]
        .filter(_.id == lift(id))
        .update(
          _.name -> lift(metadata.name),
          _.description -> lift(metadata.description),
          _.tag -> lift(metadata.tag),
          _.labels -> lift(metadata.labels)
        )
        .returning(x => x)
    }

    run(q)
  }

  def updateMaxDebtAllowed(id: AccountId, maxDebtAllowed: Cents, reason: String, labels: Map[String, String]): ZIO[Any, Throwable, AccountEntity] = {
    val q = quote {
      query[AccountEntity].filter(_.id == lift(id)).update(_.maxDebtAllowed -> lift(maxDebtAllowed)).returning(x => x)
    }

    val ins = quote {
      query[AdminActionEntity].insertValue(lift(AdminAction.maxDebtUpdated(id, maxDebtAllowed, reason, labels).entity))
    }

    transaction {
      run(ins) *> run(q)
    }
  }

  def deactivateAccount(id: AccountId, reason: String, labels: Map[String, String]): ZIO[Any, Throwable, AccountEntity] = {
    val q = quote {
      query[AccountEntity].filter(_.id == lift(id)).update(_.status -> DEACTIVATED).returning(x => x)
    }

    val ins = quote {
      query[AdminActionEntity].insertValue(lift(AdminAction.accountDeactivated(id, reason, labels).entity))
    }

    transaction {
      run(ins) *> run(q)
    }
  }

  def reactivateAccount(id: AccountId, reason: String, labels: Map[String, String]): ZIO[Any, Throwable, AccountEntity] = {
    val q = quote {
      query[AccountEntity].filter(_.id == lift(id)).update(_.status -> ACTIVE).returning(x => x)
    }

    val ins = quote {
      query[AdminActionEntity].insertValue(lift(AdminAction.accountReactivated(id, reason, labels).entity))
    }

    transaction {
      run(ins) *> run(q)
    }
  }
}
