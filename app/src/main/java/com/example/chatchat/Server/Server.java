package org.example.Android;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Server {
    private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void main(String[] args) throws Exception {
        File serverCert = new File("cert2/server-cert.pem");
        File serverKey  = new File("cert2/server-key.pem");


        SslContext sslCtx = SslContextBuilder
                .forServer(serverCert, serverKey)
                .build();

        int port = 5001;
        EventLoopGroup bossGroup   = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            // TLS
                            p.addLast(sslCtx.newHandler(ch.alloc()));
                            // фрейминг
                            p.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
                            p.addLast(new LengthFieldPrepender(2));
                            // ретранслятор + логирование
                            p.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    // лог подключения
                                    System.out.println("New client connected: " + ctx.channel().remoteAddress());
                                    channels.add(ctx.channel());
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                    byte[] data = new byte[msg.readableBytes()];
                                    msg.readBytes(data);
                                    String text = new String(data, StandardCharsets.UTF_8).trim();

                                    // Логируем сообщение
                                    System.out.println("[" + ctx.channel().remoteAddress() + "] → " + text);

                                    // Ретранслируем всем
                                    ByteBuf out = Unpooled.wrappedBuffer(
                                            (ctx.channel().remoteAddress() + ": " + text)
                                                    .getBytes(StandardCharsets.UTF_8)
                                    );
                                    for (Channel ch : channels) {
                                        ch.writeAndFlush(out.retainedDuplicate());
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    cause.printStackTrace();
                                    ctx.close();
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) {
                                    System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
                                    channels.remove(ctx.channel());
                                }
                            });
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
            System.out.println("Server listening on 0.0.0.0:" + port);
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
