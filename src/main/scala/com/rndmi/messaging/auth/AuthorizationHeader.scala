package com.rndmi.messaging.auth

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}

import scala.util.Try

/**
  * Akka HTTP Header definition for AUTHORIZATION
  */


object AuthorizationHeader extends ModeledCustomHeaderCompanion[AuthorizationHeader] {
  override val name = "AUTHORIZATION"

  override def parse(value: String) = Try(new AuthorizationHeader(value))
}

final class AuthorizationHeader(token: String) extends ModeledCustomHeader[AuthorizationHeader] {
  override val companion = AuthorizationHeader

  override def value: String = token

  override def renderInResponses(): Boolean = true

  override def renderInRequests(): Boolean = true
}