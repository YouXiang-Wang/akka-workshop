package tech.parasol.akka.workshop.utils



import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Partition}

trait StreamHelper {

  def balancer[In, Out](worker: Flow[In, Out, Any], parallelism: Int): Flow[In, Out, NotUsed] = {
    import GraphDSL.Implicits._

    Flow.fromGraph(GraphDSL.create() { implicit b =>
      val balancer = b.add(Balance[In](parallelism, waitForAllDownstreams = true))
      val merge = b.add(Merge[Out](parallelism))

      for (_ <- 1 to parallelism) {
        balancer ~> worker.async ~> merge
      }
      FlowShape(balancer.in, merge.out)
    })
  }


  def merge[In, Out](
                      worker: Flow[In, Out, Any],
                      parallelism: Int,
                      f: In => Int
                    ): Flow[In, Out, NotUsed] = {
    import GraphDSL.Implicits._

    val applicablePartition: Partition[In] = Partition[In](parallelism, f)


    Flow.fromGraph(GraphDSL.create() { implicit b =>

      val partition = b.add(applicablePartition)

      val balancer = b.add(Balance[In](parallelism, waitForAllDownstreams = true))
      val merge = b.add(Merge[Out](parallelism))

      for (_ <- 1 to parallelism) {
        balancer ~> worker.async ~> merge
      }
      FlowShape(balancer.in, merge.out)
    })
  }

}

object StreamHelper extends StreamHelper
