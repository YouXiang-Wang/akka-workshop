package tech.parasol.akka.workshop.route.retry

import java.util.concurrent.{ThreadLocalRandom, TimeUnit}

import scala.concurrent.duration.{Duration, FiniteDuration}

object Defaults {
  val delay: FiniteDuration = Duration(500, TimeUnit.MILLISECONDS)
  val cap: FiniteDuration = Duration(1, TimeUnit.MINUTES)
  val random: Jitter.RandomSource =
    Jitter.randomSource(ThreadLocalRandom.current())
  val jitter = Jitter.full()
}