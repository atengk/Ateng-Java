package io.github.atengk.grpc.client;

import io.github.atengk.grpc.GreeterGrpc;
import io.github.atengk.grpc.HelloReply;
import io.github.atengk.grpc.HelloRequest;
import io.grpc.Channel;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;


@Service
public class GreeterClient {

    private final GreeterGrpc.GreeterBlockingStub stub;

    public GreeterClient(GrpcChannelFactory channelFactory) {
        Channel channel = channelFactory.createChannel("greeter");

        this.stub = GreeterGrpc.newBlockingStub(channel);
    }

    public String sayHello(String name) {
        HelloReply reply = stub.sayHello(
                HelloRequest.newBuilder().setName(name).build()
        );
        return reply.getMessage();
    }
}