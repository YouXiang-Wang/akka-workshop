package tech.parasol.akka.workshop.stream


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object SubStreamExample {
  implicit val system = ActorSystem("SubStreamExample")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    //exampleOne()
    //mergeStream()

    flatMapMergeExample

    /*
    mergeStream()
    mergeSubstreamsWithParallelism()
    splitWhen()
    flatMapConcatExample()
    flatMapMergeExample()

     */
  }


  def exampleOne(): Unit ={


    //分成3组；然后每一组后面都添加一个sink；3个sink造成每个sink轮流输出是不能保证输出循序的
    //Source(1 to 10).groupBy(3, _ % 3).to(Sink.ignore).run()

    //1 4 7 10 (1 + 4  + 7 + 10  ) = 22
    //2 5 8 (2  + 5 + 8) = 15
    //3 6 9 (3  + 6  + 9) = 18
    Source(1 to 10).groupBy(3, _ % 3).flatMapConcat( i => Source(List.fill(1)(i))).reduce((x,y) => x +y).to(Sink.foreach(println)).run()  //22 28 15

  }
  def mergeStream(): Unit ={
    //groupBy(3, _ % 3)  根据后面的函数，将输入流进行分组；第一个参数最大子流数量
    //mergeSubstreams;合并3组到一个组；顺序是不能保证的
    Source(1 to 10)
      .groupBy(3, _ % 3)
      .mergeSubstreams
      .runWith(Sink.foreach(println))

  }

  def mergeSubstreamsWithParallelism(): Unit ={

    //groupBy(3, _ % 3)  根据后面的函数，将输入流进行分组；第一个参数最大子流数量
    //mergeSubstreamsWithParallelism;limit the number of active substreams running and being merged at a time
    //如果并行度小于子流数，会发生阻塞；第三类元素出来之后没有子流；会一直阻塞；其他的输出流的元素也不能出来；得到有效处理
    Source(1 to 10)
      .groupBy(3, _ % 3)
      .mergeSubstreamsWithParallelism(2)
      .runWith(Sink.foreach(println))

    //concatSubstreams is equivalent to mergeSubstreamsWithParallelism(1)
    Source(1 to 10)
      .groupBy(3, _ % 3)
      .concatSubstreams
      .runWith(Sink.ignore)
  }

  def splitWhen(): Unit ={
    val text =
      "This is the first line.\n" +
        "The second line.\n" +
        "There is also the 3rd line\n"

    //splitWhen 和 splitAfter 返回true时，都会产生一个新的子流；when是当前元素进入新的子流；after是当前的下一个元素进入子流
    val charCount = Source(text.toList)
      .splitAfter { _ == '\n' }
      .filter(_ != '\n')
      .map(_ ⇒ 1)
      .reduce(_ + _)
      .to(Sink.foreach(println))
      .run()
  }

  def flatMapConcatExample(): Unit ={
    // List.fill(3)(1) 一个list，充满3个1元素
    //flatMapConcat 处理多个子流；处理完后一个流，再处理另一个流; 1 to 2;起两个流，完成后流作废
    Source(1 to 2)
      .flatMapConcat(i ⇒ Source(List.fill(3)(i)))
      .runWith(Sink.foreach(println))
  }

  def flatMapMergeExample(): Unit ={
    //flatMapMerge 多个子流处理数据；第一个参数为宽度；子流个数
    Source(1 to 2)
      .flatMapMerge(3, i ⇒ Source(List.fill(3)(i)))
      .runWith(Sink.foreach(println))
  }
}


