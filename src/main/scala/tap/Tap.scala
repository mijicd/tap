package tap

import scalaz.zio.{ Ref, UIO, ZIO }

/**
  * A `Tap` adjusts the flow of tasks through
  * an external service in response to observed
  * failures in the service, always trying to
  * maximize flow while attempting to meet the
  * user-defined upper bound on failures.
  */
trait Tap[-E1, +E2] {

  /**
    * Sends the task through the tap. The
    * returned task may fail immediately with a
    * default error depending on the service
    * being guarded by the tap.
    */
  def apply[R, E >: E2 <: E1, A](effect: ZIO[R, E, A]): ZIO[R, E, A]
}

object Tap {
  type Percentage = Double

  /**
    * Creates a tap that aims for the specified
    * maximum error rate, using the specified
    * function to qualify errors (unqualified
    * errors are not treated as failures for
    * purposes of the tap), and the specified
    * default error used for rejecting tasks
    * submitted to the tap.
    */
  def make[E1, E2](errBound: Percentage, qualified: E1 => Boolean, rejected: => E2): UIO[Tap[E1, E2]] =
    for {
      ref      <- Ref.make[List[Boolean]](Nil)
      results  <- ref.get
      failures = results.count(!_)
    } yield
      new Tap[E1, E2] {
        override def apply[R, E >: E2 <: E1, A](effect: ZIO[R, E, A]): ZIO[R, E, A] =
          if (failures > errBound * results.size)
            ref.update(storeResult(true)) *> ZIO.fail(rejected)
          else
            effect.either.flatMap {
              case err @ Left(e) => ref.update(storeResult(qualified(e))).map(_ => err)
              case res           => ref.update(storeResult(true)).map(_ => res)
            }.absolve
      }

  private def storeResult(result: Boolean)(results: List[Boolean]): List[Boolean] = (result :: results).take(QueueSize)

  private val QueueSize = 100
}
