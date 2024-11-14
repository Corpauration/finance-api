package fr.corpauration.finance
package types

import io.circe.{Decoder, Encoder}

object cents {
  opaque type Cents = Long

  object Cents {
    given Encoder[Cents] = Encoder.encodeLong.contramap(_.value)

    given Decoder[Cents] = Decoder.decodeLong.map(Cents.apply)

    def apply(long: Long): Cents = long
  }

  extension (cents: Cents) {
    def value: Long = cents
  }
}
