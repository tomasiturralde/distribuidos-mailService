package server

import io.grpc.{ManagedChannel, ManagedChannelBuilder, ServerBuilder}
import proto.mail.MailServiceGrpc
import proto.user.UserServiceGrpc
import service.MailService

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object MailServer extends App {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  /*val stubManager = new ServiceManager
  stubManager.startConnection("0.0.0.0", 50001, "mail")*/

  val server = ServerBuilder.forPort(50001)
    .addService(MailServiceGrpc.bindService(new MailService(new StubManager), ExecutionContext.global))
    .build()

  server.start()
  println("Running...")

  server.awaitTermination()
}
