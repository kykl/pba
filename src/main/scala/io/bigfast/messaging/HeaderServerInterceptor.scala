package io.bigfast.messaging

import io.bigfast.messaging.auth.AuthService
import io.grpc.Context.Key
import io.grpc._

import scala.util.{Failure, Success}

/**
  * Created by andy on 9/19/16.
  */
class HeaderServerInterceptor(implicit authService: AuthService) extends ServerInterceptor {

  override def interceptCall[RespT, ReqT](call: ServerCall[RespT, ReqT], requestHeaders: Metadata, next: ServerCallHandler[RespT, ReqT]) = {
    authService.doAuth(requestHeaders) match {
      case Success((userId, isPrivileged)) =>
        val context = Context.current().withValues(
          HeaderServerInterceptor.userIdKey,
          userId,
          HeaderServerInterceptor.privilegedKey,
          isPrivileged
        )
        Contexts.interceptCall(
          context,
          call,
          requestHeaders,
          next
        )
      case Failure(exception)              =>
        println(exception)
        throw exception
    }
  }
}

object HeaderServerInterceptor {
  val userIdKey: Key[String] = Context.key("userId")
  val privilegedKey: Key[Boolean] = Context.key("isPrivileged")
}