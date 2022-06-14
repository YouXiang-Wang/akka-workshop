package tech.parasol.akka.workshop.mailbox

import akka.actor.{ActorSystem, PoisonPill}
import akka.dispatch.PriorityGenerator
import akka.dispatch.UnboundedStablePriorityMailbox
import com.typesafe.config.Config

// We inherit, in this case, from UnboundedStablePriorityMailbox
// and seed it with the priority generator
class MyPrioMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedStablePriorityMailbox(
    // Create a new PriorityGenerator, lower prio means more important
    PriorityGenerator {
      // 'highpriority messages should be treated first if possible
      case "seq" => {
        println("------------")
        0
      }
      case "highpriority" => 1

      // 'lowpriority messages should be treated last if possible
      case "lowpriority"  => 3

      // PoisonPill when no other left
      case PoisonPill    => 4

      // We default to 1, which is in between high and low
      case otherwise     => 2
    }
  )