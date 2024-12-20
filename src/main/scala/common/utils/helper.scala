package fr.corpauration.finance
package common.utils

import scala.reflect.ClassTag

import cats.effect.IO as CatsIO
import kyo.*

object helper {

  extension [T: Flat, E1: ClassTag: Tag, S](k: T < (Abort[E1] & S)) {

    inline def mapError[E2](f: E1 => E2): T < (Abort[E2] & S) = {
      k.catchAbort(e1 => Abort.fail(f(e1)))
    }
  }

  extension [A](io: CatsIO[Either[Throwable, A]]) {

    def asKyo: A < (Abort[Throwable] & Async) =
      Cats.get(io).map(_ ?=> e => Abort.get(e))
  }
}
