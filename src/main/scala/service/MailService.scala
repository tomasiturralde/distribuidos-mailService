package service

import io.grpc.{ManagedChannel, ManagedChannelBuilder, Status, StatusRuntimeException}
import proto.mail.{MailReply, MailRequest, MailServiceGrpc}
import proto.user.{GetUserRequest, GetUserResponse, UserServiceGrpc}
import proto.product.{ProductRequest, ProductServiceGrpc}
import proto.user.UserServiceGrpc.UserServiceStub

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class MailService(userServiceStub: UserServiceStub)(implicit ec: ExecutionContext) extends MailServiceGrpc.MailService{

  override def sendMail(request: MailRequest): Future[MailReply] = {

    val result: Future[GetUserResponse] = userServiceStub.getUser(GetUserRequest(request.userId))

    val future: Try[GetUserResponse] = Await.ready(result, Duration.apply(5, "second")).value.get

    future match {
      case Success(value) =>
        print("mail sent to: " + value.email + "\n Content: \n")
        request.products.foreach(prod => {print("\t" + prod.name + "\n")})
        Future.successful(MailReply("mail sent"))
      case Failure(exception: StatusRuntimeException) =>
        if(exception.getStatus.getCode == Status.Code.UNAVAILABLE) {
          println("Get another stub")
          sendMail(request)
        } else throw exception
    }
  }

  /*private def getUserStub: Future[UserServiceGrpc.UserServiceStub] = {
    serviceManager.getAddress("user").map{
      case Some(value) =>
        println(value.port)
        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(value.address, value.port)
          .usePlaintext(true)
          .build()
        UserServiceGrpc.stub(channel)
      case None => throw new RuntimeException("No user services running")
    }
  }*/
}
