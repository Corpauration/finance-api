package fr.corpauration.finance
package common

import io.circe.JsonObject

import java.time.OffsetDateTime
import java.util.UUID

case class Event(
    id: UUID,
    streamId: UUID,
    timestamp: OffsetDateTime,
    data: JsonObject)
