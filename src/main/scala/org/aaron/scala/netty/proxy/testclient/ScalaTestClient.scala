package org.aaron.scala.netty.proxy.testclient

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import org.aaron.scala.netty.proxy.NettyUtil
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.channel.socket.ClientSocketChannelFactory
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.logging.InternalLoggerFactory
import org.jboss.netty.logging.Slf4JLoggerFactory
import org.jboss.netty.util.HashedWheelTimer
import org.jboss.netty.util.Timeout
import org.jboss.netty.util.TimerTask

import com.weiglewilczek.slf4s.Logger

/**
 * Simple TCP client using Netty.
 */
class ScalaTestClient(
  clientSocketChannelFactory: ClientSocketChannelFactory,
  timer: HashedWheelTimer,
  serverAddressPortString: String,
  reconnectDelaySeconds: Int = 1,
  numConnectionsToCreate: Int = 1,
  numMessagesToSend: Int = 10) {

  private val log = Logger(getClass)

  val clientBootstrap = new ClientBootstrap(
    clientSocketChannelFactory)

  private class ClientHandler extends SimpleChannelUpstreamHandler {

    override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      log.info("channelOpen " + e.getChannel)
    }

    override def channelConnected(ctx: ChannelHandlerContext,
      e: ChannelStateEvent) {
      log.info("channelConnected " + e.getChannel)
      val channelPort = e.getChannel
        .getLocalAddress.asInstanceOf[InetSocketAddress].getPort
      for (i <- 0 until numMessagesToSend) {
        e.getChannel.write("port " + channelPort + " message " + i)
      }
    }

    override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      log.info("channelClosed " + e.getChannel)
      timer.newTimeout(new TimerTask {
        override def run(timeout: Timeout) {
          log.info("before connect")
          clientBootstrap.connect
        }
      }, reconnectDelaySeconds, TimeUnit.SECONDS)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      log.warn("exceptionCaught " + e.getChannel, e.getCause)
      e.getChannel.close
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      log.info("messageReceived from " + e.getChannel + " message = '"
        + e.getMessage + "'")
    }

  }

  def start() {
    InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

    clientBootstrap.setPipelineFactory(
      new TestProtocol.TestProtocolPipelineFactory(
        new ClientHandler))

    clientBootstrap.setOption("remoteAddress",
      NettyUtil.parseAddressPortString(serverAddressPortString))

    for (i <- 0 until numConnectionsToCreate) {
      log.info("before connect")
      clientBootstrap.connect
    }
  }

}

object ScalaTestClientMain extends App {

  private val log = Logger(getClass)

  if (args.size == 1) {
    val executor = Executors.newCachedThreadPool
    val timer = new HashedWheelTimer
    val clientSocketChannelFactory = new NioClientSocketChannelFactory(
      executor, executor)
    new ScalaTestClient(
      timer = timer,
      clientSocketChannelFactory = clientSocketChannelFactory,
      serverAddressPortString = args(0)).start
  } else {
    log.warn("Usage: <server address>")
    sys.exit(1)
  }

}