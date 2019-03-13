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
  final case class Percentage private (value: Double) extends AnyVal

  object Percentage {
    def fromValue(value: Double): UIO[Percentage] =
      if (value < 0 || value > 100)
        ZIO.die(new IllegalArgumentException("Value must be taken from [0, 100]."))
      else
        ZIO.succeed(Percentage(value))
  }

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
      ref   <- Ref.make(State(0, 0))
      state <- ref.get
    } yield
      new Tap[E1, E2] {
        override def apply[R, E >: E2 <: E1, A](effect: ZIO[R, E, A]): ZIO[R, E, A] =
          if (state.breachesBound(errBound))
            ref.update(_.storeResult(true)) *> ZIO.fail(rejected)
          else
            effect.either.flatMap {
              case err @ Left(e) => ref.update(_.storeResult(qualified(e))).map(_ => err)
              case res           => ref.update(_.storeResult(true)).map(_ => res)
            }.absolve
      }

  private final case class State(totalTasks: Long, failedTasks: Long) {
    def breachesBound(bound: Percentage): Boolean = failedTasks * 100 > bound.value * totalTasks

    def storeResult(result: Boolean): State =
      if (result) copy(totalTasks = totalTasks + 1) else State(totalTasks + 1, failedTasks + 1)
  }
}
