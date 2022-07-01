package tech.parasol.akka.workshop.http

object ExceptionResources {

  lazy val ResourceIsNotFound: String =
    s"""
       |{
       |  "trace": "",
       |  "errors": [
       |    {
       |      "code": "not_found",
       |      "message": "The requested resource is not found.",
       |      "target": {
       |        "type": "field",
       |        "name": "string"
       |      },
       |      "more_info": ""
       |    }
       |  ]
       |}
       |
     """.stripMargin

  lazy val AuthenticationFailure: String =
    s"""
       |{
       |  "trace": "string",
       |  "errors": [
       |    {
       |      "code": "authentication_failure",
       |      "message": "User name or password is invalid.",
       |      "target": {
       |        "type": "field",
       |        "name": "string"
       |      },
       |      "more_info": "User name or password is invalid."
       |    }
       |  ]
       |}
       |
     """.stripMargin


  lazy val TokenIsExpired: String =
    s"""
       |{
       |  "trace": "",
       |  "errors": [
       |    {
       |      "code": "authentication_failure",
       |      "message": "Access token is expired.",
       |      "target": {
       |        "type": "field",
       |        "name": "string"
       |      },
       |      "more_info": "Access token is expired."
       |    }
       |  ]
       |}
       |
     """.stripMargin


  lazy val ErrorPayload: String =
    s"""
       |{
       |  "trace": "string",
       |  "errors": [
       |    {
       |      "code": "ErrorPayload",
       |      "message": "Payload is incorrect.",
       |      "target": {
       |        "type": "field",
       |        "name": "string"
       |      },
       |      "more_info": "Pay load is incorrect."
       |    }
       |  ]
       |}
       |
     """.stripMargin


  lazy val InternalServerError: String =
    s"""
       |{
       |  "trace": "string",
       |  "errors": [
       |    {
       |      "code": "InternalServerError",
       |      "message": "InternalServerError.",
       |      "target": {
       |        "type": "field",
       |        "name": "string"
       |      },
       |      "more_info": "InternalServerError."
       |    }
       |  ]
       |}
       |
     """.stripMargin


  lazy val ServiceUnavailableError: String =
    s"""
       |{
       |  "trace": "string",
       |  "errors": [
       |    {
       |      "code": "UnreachableError",
       |      "message": "Resource is unreachableError.",
       |      "target": {
       |        "type": "field",
       |        "name": "string"
       |      },
       |      "more_info": "Resource is unreachableError."
       |    }
       |  ]
       |}
       |
     """.stripMargin

}
