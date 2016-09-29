package io.bigfast.messaging.auth

import io.grpc.Metadata

import scala.util.Try

/**
  * AuthService
  * doAuth returns (userId, isPrivileged)
  * userId: String ID for user to propagate through Context
  * isPrivileged: Boolean flag for whether user is admin
  */
trait AuthService {
  def doAuth(metadata: Metadata): Try[(String, Boolean)]
}
