package tech.parasol.akka.workshop.stream.kafka


import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge}

trait StreamHelper {

  def balancer[In, Out](worker: Flow[In, Out, Any], parallelism: Int): Flow[In, Out, NotUsed] = {
    import GraphDSL.Implicits._

    Flow.fromGraph(GraphDSL.create() { implicit b =>
      val balancer = b.add(Balance[In](parallelism, waitForAllDownstreams = true))
      val merge = b.add(Merge[Out](parallelism))

      for (_ <- 1 to parallelism) {
        // for each worker, add an edge from the balancer to the worker, then wire
        // it to the merge element
        balancer ~> worker.async ~> merge
      }
      FlowShape(balancer.in, merge.out)
    })
  }

}

object StreamHelper extends StreamHelper
