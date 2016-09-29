package com.rndmi.messaging.auth

import io.bigfast.messaging.auth.AuthService
import io.grpc.Metadata

import scala.util.Try

/**
  * Created by andy on 9/28/16.
  */
class RandomAuthService extends AuthService {
  override def doAuth(metadata: Metadata) = Try {
    val authorization = metadata.get[String](RandomAuthService.authorizationKey)
    val session = metadata.get[String](RandomAuthService.sessionKey)

    println(s"Pretending to check auth for $authorization, $session")

    ("18125", true)
  }
}

object RandomAuthService {
  val authorizationKey = Metadata.Key.of("AUTHORIZATION", Metadata.ASCII_STRING_MARSHALLER)
  val sessionKey = Metadata.Key.of("X-AUTHENTICATION", Metadata.ASCII_STRING_MARSHALLER)
}
