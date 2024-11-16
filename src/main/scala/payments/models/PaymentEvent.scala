package fr.corpauration.finance
package payments.models

import payments.models.*

import java.time.OffsetDateTime
import java.util.UUID

enum PaymentEvent {
  case PaymentSubmitted(
    id: UUID,
    author: String,
    metadata: PaymentMetadata,
    amount: Long,
    method: PaymentMethod,
    timestamp: OffsetDateTime
  )

  case PaymentMetadataUpdated(
    id: UUID,
    metadata: PaymentMetadata,
    timestamp: OffsetDateTime
  )

  case PaymentConfirmed(
    id: UUID,
    timestamp: OffsetDateTime
  )

  case PaymentCancelled(
    id: UUID,
    timestamp: OffsetDateTime
  )
}