package tech.parasol.akka.workshop.jdbc


import java.sql.{Connection, PreparedStatement}

object PerformanceTest {

  val dbName = "binlog_test"
  val username = "root"
  val password = "wyx1005"


  def buildConfig(host: String, port: Int, dbName: String) = {
    DatabasePoolConfig(
      dbType = "mysql",
      dbUrl = s"jdbc:mysql://${host}:${port}/${dbName}?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useSSL=false&socketTimeout=10000",
      dbName = dbName,
      dbUser = "root",
      dbPassword = "wyx1005"
    )
  }

  val dataSource1 = ConnectionHelper.getDataSource(buildConfig("127.0.0.1", 3306, dbName))
  val dataSource2 = ConnectionHelper.getDataSource(buildConfig("127.0.0.1", 3307, dbName))
  val dataSource3 = ConnectionHelper.getDataSource(buildConfig("127.0.0.1", 3308, dbName))

  val dataSources = Seq(dataSource1, dataSource2, dataSource3)


  def insertData(connection: Connection, id1: String, id2: String, age: Int, name: String) = {
    try {
      val sql: String= "INSERT INTO bidi_t1(id1, id2, age, name) values(?,?,?,?)"
      val p: PreparedStatement = connection.prepareStatement(sql)

      p.setString(1, id1)
      p.setString(2, id2)
      p.setInt(3, age)
      p.setString(4, name)
      p.execute()
    } catch {
      case e: Exception => {
        e.printStackTrace()
      }
    } finally {
      connection.close()
    }
  }

  def main(args: Array[String]): Unit = {
    val count = 100
    for(i <- 1 to count) {
      val index = {
        if(i % 3 == 0) 3 else i % 3
      }

      val id1 = s"test${index}_00${i}"
      val id2 = s"test${index}_00${i}"
      val age = 10
      val name = s"name_00${i}"

      val connection = dataSources(index - 1).getConnection()
      insertData(connection, id1, id2, age, name)
    }
  }
}
