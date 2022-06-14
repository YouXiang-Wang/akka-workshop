package tech.parasol.akka.workshop.persistence

import java.util.concurrent.{ExecutorService, Executors}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.duration._
import akka.pattern.ask

import scala.concurrent.ExecutionContextExecutor

class LotteryRun(lotteryActor: ActorRef, lotteryCmd: LotteryCmd)(implicit executor: ExecutionContextExecutor) extends Runnable {
  implicit val timeout = Timeout(3.seconds)
  def run: Unit = {
    for {
      fut <- lotteryActor ? lotteryCmd
    } yield fut match {  //根据不同事件显示不同的抽奖结果
      case le: LuckyEvent => println(s"恭喜用户${le.userId}抽到了${le.luckyMoney}元红包")
      case fe: FailureEvent =>  println(fe.reason)
      case _ => println("系统错误，请重新抽取")
    }
  }
}



object PersistenceTest extends App {
  val lottery = Lottery(10000,10000)
  val system = ActorSystem("example-05")
  val lotteryActor = system.actorOf(Props(new LotteryActor(lottery)), "LotteryActor-2")  //创建抽奖Actor
  implicit val executor: ExecutionContextExecutor = system.getDispatcher

  val pool: ExecutorService = Executors.newFixedThreadPool(10)
  val r = (1 to 100).map(i =>
    new LotteryRun(lotteryActor, LotteryCmd(i.toLong,"godpan","xx@gmail.com"))  //创建100个抽奖请求
  )

  r.map(pool.execute(_))  //使用线程池来发起抽奖请求，模拟同时多人参加
  Thread.sleep(5000)
  pool.shutdown()
  system.terminate()
}
