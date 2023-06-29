package tech.parasol.akka.workshop.jdbc


import java.util.Properties

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.slf4j.{Logger, LoggerFactory}

object ConnectionHelper {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def getDataSource(dbConfig: DatabasePoolConfig, properties: Option[Properties] = None) = {

    val config = new HikariConfig()
    config.setJdbcUrl(dbConfig.dbUrl)
    config.setUsername(dbConfig.dbUser)
    config.setPassword(dbConfig.dbPassword)
    config.setMaximumPoolSize(dbConfig.maximumPoolSize)
    config.setValidationTimeout(1000)
    config.setMaxLifetime(1800000)
    config.setConnectionTimeout(dbConfig.connectionTimeout)
    //config.setIdleTimeout(60000)
    properties.map(p => config.setDataSourceProperties(p))

    new HikariDataSource(config)
  }
}
