package tech.parasol.akka.workshop.jdbc

final case class DatabasePoolConfig(
                                     dbType: String,
                                     dbUrl: String,
                                     dbName: String,
                                     dbUser: String,
                                     dbPassword: String,
                                     queueSize: Int = 1000,
                                     numThreads: Int = 50,
                                     connectionTimeout: Int = 20000,
                                     maximumPoolSize: Int = 10
                                   )
