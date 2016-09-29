package io.bigfast.messaging.auth

import io.grpc.Metadata

import scala.util.Try

/**
  * Created by andy on 9/28/16.
  */
class NoOpAuthService extends AuthService {
  override def doAuth(metadata: Metadata): Try[(String, Boolean)] = Try {
    val userId = metadata.get[String](NoOpAuthService.userKey)
    (userId, false)
  }
}

object NoOpAuthService {
  val userKey = Metadata.Key.of("X-USER", Metadata.ASCII_STRING_MARSHALLER)
}