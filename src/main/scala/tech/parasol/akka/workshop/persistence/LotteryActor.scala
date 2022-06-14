package tech.parasol.akka.workshop.persistence

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotSuccess, SnapshotOffer}

class LotteryActor(initState: Lottery) extends PersistentActor with ActorLogging{
  override def persistenceId: String = "lottery-actor-2"

  var state = initState  //初始化Actor的状态

  override def receiveRecover: Receive = {
    case event: LuckyEvent => {
      updateState(event)  //恢复Actor时根据持久化的事件恢复Actor状态
    }
    case SnapshotOffer(_, snapshot: Lottery) =>
      log.info(s"Recover actor state from snapshot and the snapshot is ${snapshot}")
      state = snapshot //利用快照恢复Actor的状态
    case RecoveryCompleted => log.info("the actor recover completed")
  }

  def updateState(le: LuckyEvent) =
    state = state.update(le.luckyMoney)  //更新自身状态

  override def receiveCommand: Receive = {
    case lc: LotteryCmd =>
      doLottery(lc) match {     //进行抽奖，并得到抽奖结果，根据结果做出不同的处理
        case le: LuckyEvent =>  //抽到随机红包
          persist(le) { event =>
            updateState(event)
            increaseEvtCountAndSnapshot()
            sender() ! event
          }
        case fe: FailureEvent =>  //红包已经抽完
          sender() ! fe
      }
    case "saveSnapshot" =>  // 接收存储快照命令执行存储快照操作
      saveSnapshot(state)

    case SaveSnapshotSuccess(metadata) =>  {
      //你可以在快照存储成功后做一些操作，比如删除之前的快照等
      println("SaveSnapshotSuccess success!")

    }
  }

  private def increaseEvtCountAndSnapshot() = {
    val snapShotInterval = 5
    if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0) {  //当有持久化5个事件后我们便存储一次当前Actor状态的快照
      self ! "saveSnapshot"
    }
  }

  def doLottery(lc: LotteryCmd) = {  //抽奖逻辑具体实现
    if (state.remainAmount > 0) {
      val luckyMoney = scala.util.Random.nextInt(state.remainAmount) + 1
      LuckyEvent(lc.userId, luckyMoney)
    }
    else {
      FailureEvent(lc.userId, s"${lc.userId}下次早点来，红包已被抽完咯！")
    }
  }
}