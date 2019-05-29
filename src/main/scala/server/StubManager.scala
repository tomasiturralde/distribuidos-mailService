package server

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import proto.user.UserServiceGrpc

class StubManager {

  def userChannel: ManagedChannel = ManagedChannelBuilder.forAddress("user", 50000)
    .usePlaintext(true)
    .build()

  def userStub: UserServiceGrpc.UserServiceStub = UserServiceGrpc.stub(userChannel)

}
