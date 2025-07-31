package com.example.chatchat;

import android.util.Base64;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class ChatClient {

    private final String host;
    private final int port;
    private final Listener listener;

    private EventLoopGroup group;
    private Channel channel;
    private String binPassword =${binPassword}

    public interface Listener {
        void onConnected();
        void onMessageReceived(String msg);
        void onError(Throwable cause);
    }

    public ChatClient(String host, int port,

                      Listener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect() {
        group = new NioEventLoopGroup();

        try {
            // Шаг 1: заранее вычисленный pin:
            final String expectedPin = binPassword;

            // Шаг 2: создаём TrustManager, который сверяет pin
            TrustManager[] pins = new TrustManager[] {
                    new X509TrustManager() {
                        private final MessageDigest md = MessageDigest.getInstance("SHA-256");

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            // Берём сертификат сервера (первый в цепочке)
                            byte[] derKey = chain[0].getPublicKey().getEncoded();
                            byte[] digest = md.digest(derKey);
                            String pin = "sha256/" + Base64.encodeToString(digest, Base64.NO_WRAP);


                            if (!pin.equals(expectedPin)) {
                                throw new CertificateException("Server certificate pin mismatch! Got=" + pin);
                            }

                        }

                        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };

            // Шаг 3: инициализируем SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, pins, new SecureRandom());

            // Шаг 4: строим Netty Bootstrap с TLS и фреймингом
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            SSLEngine sslEngine = sslContext.createSSLEngine(host, port);
                            sslEngine.setUseClientMode(true);
                            ch.pipeline().addFirst(new SslHandler(sslEngine));

                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
                            ch.pipeline().addLast(new StringDecoder(StandardCharsets.UTF_8));
                            ch.pipeline().addLast(new LengthFieldPrepender(2));
                            ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));

                            ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    listener.onConnected();
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                    listener.onMessageReceived(msg);
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    listener.onError(cause);
                                    ctx.close();
                                }
                            });
                        }
                    });

            // Шаг 5:  надежда на успех
            ChannelFuture future = bootstrap.connect(host, port).sync();
            if (future.isSuccess()) {
                channel = future.channel();
            } else {
                listener.onError(future.cause());
            }

        } catch (Exception e) {
            listener.onError(e);
        }
    }


    public void sendMessage(String message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
        }
    }

    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}
