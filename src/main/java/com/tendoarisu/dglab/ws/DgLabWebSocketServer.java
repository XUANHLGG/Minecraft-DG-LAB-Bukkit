package com.tendoarisu.dglab.ws;

import com.tendoarisu.dglab.core.DgLabConnection;
import com.tendoarisu.dglab.core.DgLabConnectionManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DgLabWebSocketServer {

    private static final AttributeKey<String> CLIENT_ID_ATTR = AttributeKey.valueOf("clientId");
    private static final AttributeKey<DgLabConnection> CONN_ATTR = AttributeKey.valueOf("dglab_connection");

    private final JavaPlugin plugin;
    private final int port;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private volatile Channel serverChannel;

    public DgLabWebSocketServer(JavaPlugin plugin, String ignoredAddress, int port) {
        this.plugin = plugin;
        this.port = port;

    }

    public boolean isRunning() {
        return running.get();
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpServerCodec());
                        p.addLast(new HttpObjectAggregator(65536));

                        p.addLast(new DgLabWsHttpAdapter());

                        p.addLast(new IdleStateHandler(0, 20, 0, TimeUnit.SECONDS));

                        p.addLast(new WebSocketServerProtocolHandler(
                                "/", "/", true, 65536 * 10, true, true, true, Long.MAX_VALUE));

                        p.addLast(new DgLabWsHandler());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        Thread t = new Thread(() -> {
            try {
                ChannelFuture f = bootstrap.bind(port).sync();
                serverChannel = f.channel();
                plugin.getLogger().info("DgLab WebSocket 服务器已启动，端口：" + port);

                while (running.get()) {
                    Thread.sleep(1000);
                }

                try {
                    if (serverChannel != null) {
                        serverChannel.close().sync();
                    }
                } catch (Exception ignored) {
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                plugin.getLogger().severe("无法启动 WebSocket 服务器: " + e.getMessage());
                e.printStackTrace();
            } finally {
                shutdownGroups();
            }
        }, "dglab-ws");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        try {
            if (serverChannel != null) {
                serverChannel.close();
            }
        } catch (Exception ignored) {
        }

        shutdownGroups();
        plugin.getLogger().info("DgLab WebSocket 服务器已停止");
    }

    private void shutdownGroups() {
        try {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
                bossGroup = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
                workerGroup = null;
            }
        } catch (Exception ignored) {
        }
    }

    final class DgLabWsHttpAdapter extends io.netty.channel.ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(io.netty.channel.ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof FullHttpRequest req) {
                String clientId = req.uri();
                if (clientId.startsWith("/")) clientId = clientId.substring(1);
                ctx.channel().attr(CLIENT_ID_ATTR).set(clientId);
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void channelActive(io.netty.channel.ChannelHandlerContext ctx) {
            DgLabConnection connection = new DgLabConnection(plugin, ctx.channel());
            ctx.channel().attr(CONN_ATTR).set(connection);

            DgLabConnectionManager.putByChannel(ctx.channel(), connection);
        }

        @Override
        public void channelInactive(io.netty.channel.ChannelHandlerContext ctx) {
            cleanupAndNotify(ctx);
        }

        @Override
        public void exceptionCaught(io.netty.channel.ChannelHandlerContext ctx, Throwable cause) {

            cleanupAndNotify(ctx);
            try {
                ctx.close();
            } catch (Exception ignored) {
            }
        }

        private void cleanupAndNotify(io.netty.channel.ChannelHandlerContext ctx) {
            DgLabConnection conn = ctx.channel().attr(CONN_ATTR).getAndSet(null);
            if (conn != null) {
                try {
                    
                    DgLabConnectionManager.removeByChannel(ctx.channel());

                    try {
                        UUID uuid = UUID.fromString(conn.clientId());
                        DgLabConnectionManager.remove(uuid);
                    } catch (Exception ignored) {
                    }

                    conn.onChannelInactive();
                } catch (Exception ignored) {
                }
            }
        }
    }

    final class DgLabWsHandler extends io.netty.channel.SimpleChannelInboundHandler<TextWebSocketFrame> {

        @Override
        protected void channelRead0(io.netty.channel.ChannelHandlerContext ctx, TextWebSocketFrame msg) {
            DgLabConnection conn = ctx.channel().attr(CONN_ATTR).get();
            if (conn != null) {
                conn.handleTextFrame(msg.text());
            }
        }

        @Override
        public void userEventTriggered(io.netty.channel.ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                DgLabConnection conn = ctx.channel().attr(CONN_ATTR).get();
                if (conn != null) {
                    String clientId = ctx.channel().attr(CLIENT_ID_ATTR).get();
                    conn.onHandshakeComplete(clientId);

                    try {
                        UUID uuid = UUID.fromString(clientId);
                        DgLabConnectionManager.put(uuid, conn);
                    } catch (Exception ignored) {
                    }

                    plugin.getLogger().info("DgLab 握手完成，clientId=" + clientId);
                }
                return;
            }

            if (evt instanceof IdleStateEvent idle && idle.state() == IdleState.WRITER_IDLE) {
                
                ctx.writeAndFlush(new PingWebSocketFrame());
                return;
            }

            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(io.netty.channel.ChannelHandlerContext ctx, Throwable cause) {
            
            try {
                ctx.close();
            } catch (Exception ignored) {
            }
        }
    }
}
