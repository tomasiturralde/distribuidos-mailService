package service

import io.grpc.{ManagedChannel, ManagedChannelBuilder, Status, StatusRuntimeException}
import proto.mail.{MailReply, MailRequest, MailServiceGrpc}
import proto.user.{GetUserRequest, GetUserResponse, UserServiceGrpc}
import proto.product.{ProductRequest, ProductServiceGrpc}
import server.ServiceManager

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class MailService(serviceManager: ServiceManager)(implicit ec: ExecutionContext) extends MailServiceGrpc.MailService{

  override def sendMail(request: MailRequest): Future[MailReply] = {
    getUserStub.flatMap( stub => {
      val result: Future[GetUserResponse] = stub.getUser(GetUserRequest(request.userId))

      val future: Try[GetUserResponse] = Await.ready(result, Duration.apply(5, "second")).value.get

      future match {
        case Success(value) => getProducts(value, request)
        case Failure(exception: StatusRuntimeException) =>
          if(exception.getStatus.getCode == Status.Code.UNAVAILABLE) {
            println("Get another stub")
            sendMail(request)
          } else throw exception
      }
    })
  }

  private def getProducts(user: GetUserResponse, request: MailRequest): Future[MailReply] = {
    getProductStub.flatMap(stub => {
      val result = request.productsId
        .map(id => stub.getProduct(ProductRequest(id)))

      val result2 = Future.sequence(result)

      val future = Await.ready(result2, Duration.apply(5, "second")).value.get

      future match {
        case Success(value) =>
          print("mail sent to: " + user.email + "\n Content: \n")
          value.foreach(prod => {print("\t" + prod.name + "\n")})
          Future.successful(MailReply("mail sent"))
        case Failure(exception: StatusRuntimeException) =>
          if(exception.getStatus.getCode == Status.Code.UNAVAILABLE) {
            println("Get another stub")
            getProducts(user, request)
          } else throw exception
      }
    })
  }

  private def getProductStub: Future[ProductServiceGrpc.ProductServiceStub] = {
    serviceManager.getAddress("product").map{
      case Some(value) =>
        println(value.port)
        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(value.address, value.port)
          .usePlaintext(true)
          .build()
        ProductServiceGrpc.stub(channel)
      case None => throw new RuntimeException("No product services running")
    }
  }

  private def getUserStub: Future[UserServiceGrpc.UserServiceStub] = {
    serviceManager.getAddress("user").map{
      case Some(value) =>
        println(value.port)
        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(value.address, value.port)
          .usePlaintext(true)
          .build()
        UserServiceGrpc.stub(channel)
      case None => throw new RuntimeException("No user services running")
    }
  }
}
