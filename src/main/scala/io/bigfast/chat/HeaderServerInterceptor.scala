package io.bigfast.chat

import io.grpc.Context.Key
import io.grpc._

/**
  * Created by andy on 9/19/16.
  */
class HeaderServerInterceptor extends ServerInterceptor {

  override def interceptCall[RespT, ReqT](call: ServerCall[RespT, ReqT], requestHeaders: Metadata, next: ServerCallHandler[RespT, ReqT]) = {
    val authorization = requestHeaders.get[String](HeaderServerInterceptor.metadataKey)

    val context = Context.current().withValue(
      HeaderServerInterceptor.contextKey,
      authorization
    )

    Contexts.interceptCall(
      context,
      call,
      requestHeaders,
      next
    )
  }
}

object HeaderServerInterceptor {
  val metadataKey = Metadata.Key.of("AUTHORIZATION", Metadata.ASCII_STRING_MARSHALLER)

  val contextKey: Key[String] = Context.key("AUTHORIZATION")
}