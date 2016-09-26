package io.bigfast.chat

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall
import io.grpc._
import io.grpc.stub.MetadataUtils

/**
  * Created by andy on 9/19/16.
  */
class HeaderServerInterceptor extends ServerInterceptor {
  private val customHeadKey = Metadata.Key.of("AUTHORIZATION", Metadata.ASCII_STRING_MARSHALLER)

  override def interceptCall[RespT, ReqT](call: ServerCall[RespT, ReqT], requestHeaders: Metadata, next: ServerCallHandler[RespT, ReqT]) = {
    println(s"header received from client: $requestHeaders")
    val authorization = requestHeaders.get[String](customHeadKey)

    println(s"extracted auth header: $authorization")

    val context = Context.current().withValue(
      Context.key("AUTHORIZATION"),
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
