package com.rndmi.messaging.auth

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}

import scala.util.Try

/**
  * Akka HTTP Header definition for X-AUTHENTICATION
  */


object SessionHeader extends ModeledCustomHeaderCompanion[SessionHeader] {
  override val name = "X-AUTHENTICATION"

  override def parse(value: String) = Try(new SessionHeader(value))
}

final class SessionHeader(token: String) extends ModeledCustomHeader[SessionHeader] {
  override val companion = SessionHeader

  override def value: String = token

  override def renderInResponses(): Boolean = true

  override def renderInRequests(): Boolean = true
}
