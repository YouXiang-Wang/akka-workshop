package tech.parasol.akka.workshop.thread

import java.util.concurrent.{Executors, TimeUnit}

object ThreadSleepTest {

  val oneThreadScheduleExecutor = Executors.newScheduledThreadPool(1)

  def nonBlockingTask(id: Int): Runnable = () => {
    println(s"${Thread.currentThread().getName()} start-$id")

    val endTask: Runnable = () => {
      println(s"${Thread.currentThread().getName()} end-$id")
    }
    //instead of Thread.sleep for 10s, we schedule it in the future, no more blocking!
    oneThreadScheduleExecutor.schedule(endTask, 10, TimeUnit.SECONDS)
  }


  def task(id: Int): Runnable = () => {
    println(s"${Thread.currentThread().getName()} start-$id")
    Thread.sleep(10000)
    println(s"${Thread.currentThread().getName()} end-$id")
  }

  def threadSleep = {
    new Thread(task(1), "Thread-1").start()
  }

  def poolSleep = {
    val oneThreadExecutor = Executors.newFixedThreadPool(1)

    // send 2 tasks to the executor
    (1 to 2).foreach(id =>
      oneThreadExecutor.execute(task(id)))
  }

  def schedule() = {

    val oneThreadExecutor = Executors.newFixedThreadPool(1)

    // send 2 tasks to the executor
    (1 to 2).foreach(id =>
      oneThreadExecutor.execute(nonBlockingTask(id)))
  }

  def main(args: Array[String]): Unit = {
    Thread.sleep(1000)
    schedule
    Thread.sleep(1000000)
  }
}
