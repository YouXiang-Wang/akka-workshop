package tech.parasol.akka.workshop.model


import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude}
import com.fasterxml.jackson.annotation.JsonInclude.Include


final case class RestResult(
                             code: Int = 1,
                             httpCode: Int = 200,

                             @JsonIgnore
                             @JsonInclude(Include.NON_ABSENT)
                             msg: Option[String] = None,

                             data: AnyRef
                           )


final case class RestResult2[T](
                                 code: Int = 1,
                                 httpCode: Int = 200,

                                 @JsonIgnore
                                 @JsonInclude(Include.NON_ABSENT)
                                 msg: Option[String] = None,

                                 data: T
                               )


final case class ResponseStringResult(
                                       successful: Boolean = true,
                                       responseCode: Int = 200,

                                       @JsonIgnore
                                       @JsonInclude(Include.NON_ABSENT)
                                       data: Option[String] = None,

                                       @JsonIgnore
                                       @JsonInclude(Include.NON_ABSENT)
                                       errCode: Option[String] = None,

                                       @JsonIgnore
                                       @JsonInclude(Include.NON_ABSENT)
                                       message: Option[String] = None
                                     )


final case class ResponseResult[T](
                                    successful: Boolean = true,
                                    responseCode: Int = 200,

                                    @JsonIgnore
                                    @JsonInclude(Include.NON_ABSENT)
                                    data: Option[T] = None,

                                    @JsonIgnore
                                    @JsonInclude(Include.NON_ABSENT)
                                    errorCode: Option[String] = None,

                                    @JsonIgnore
                                    @JsonInclude(Include.NON_ABSENT)
                                    message: Option[String] = None
                                  )

