package tech.parasol.akka.workshop.persistence

case class LotteryCmd(
                       userId: Long,
                       username: String,
                       email: String
                     )



case class LuckyEvent(
                        userId: Long,
                        luckyMoney: Int
                     )

case class FailureEvent(
                          userId: Long,
                          reason: String
                       )
case class Lottery(
                    totalAmount: Int,  //total amount in package
                    remainAmount: Int  // remain amount in package
                  ) {
  def update(luckyMoney: Int) = {
    copy(
      remainAmount = remainAmount - luckyMoney
    )
  }
}