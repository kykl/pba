package com.rndmi.messaging.auth

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import io.bigfast.messaging.MessagingServer._
import io.bigfast.messaging.auth.AuthService
import io.grpc.Metadata

import scala.util.Try

/**
  * Created by andy on 9/28/16.
  */
class RandomAuthService extends AuthService {

  import RandomAuthService._

  override def doAuth(metadata: Metadata) = Try {

    val authorization = metadata.get[String](RandomAuthService.authorizationKey)
    val session = metadata.get[String](RandomAuthService.sessionKey)

    println(s"Checking auth for $authorization, $session")

    http.singleRequest(
      HttpRequest(
        uri = "https://dev-api.rndmi.com:443/v1/profiles/me?fields=null"
      ).withHeaders(
        AuthorizationHeader(authorization),
        SessionHeader(session)
      )
    )(materializer) map { response =>
      println(s"Got response: $response")
    }

    ("18125", true)
  }
}

object RandomAuthService {
  val authorizationKey = Metadata.Key.of("AUTHORIZATION", Metadata.ASCII_STRING_MARSHALLER)
  val sessionKey = Metadata.Key.of("X-AUTHENTICATION", Metadata.ASCII_STRING_MARSHALLER)
  val http = Http()
  val materializer = ActorMaterializer()
}
