package fr.corpauration.finance
package common

import java.time.OffsetDateTime
import java.util.UUID
import cats.implicits.catsSyntaxEither
import doobie.{Meta, Read, Write}
import doobie.implicits.*
import doobie.postgres.implicits.*
import fr.corpauration.finance.accounts.models.AccountEvent
import io.circe.{Decoder, DecodingFailure, Encoder, JsonObject}
import io.circe.derivation.Configuration
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.postgresql.util.PGobject
import types.cents.Cents

case class Event(
    id: UUID,
    timestamp: OffsetDateTime,
    data: JsonObject)

object Event {

  given Meta[JsonObject] =
    Meta.Advanced
      .other[PGobject]("jsonb")
      .timap[JsonObject](a => decode[JsonObject](a.getValue).leftMap[JsonObject](e => throw e).merge)(a => {
        val o = new PGobject
        o.setType("jsonb")
        o.setValue(a.toJson.noSpaces)
        o
      })

  given Read[Event] = Read[(UUID, OffsetDateTime, JsonObject)].map {
    case (accountId, timestamp, data) => Event(accountId, timestamp, data)
  }

  given Write[Event] = Write[(UUID, OffsetDateTime, JsonObject)].contramap {
    case Event(accountId, timestamp, data) => (accountId, timestamp, data)
  }
}