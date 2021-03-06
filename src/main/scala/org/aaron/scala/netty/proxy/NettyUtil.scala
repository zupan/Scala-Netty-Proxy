package org.aaron.scala.netty.proxy

import java.net.InetSocketAddress
import org.jboss.netty.channel.Channel
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelFutureListener

/**
 * Useful functions for working with Netty.
 */
object NettyUtil {

  private val addressPortRE = """^(.*):(.*)$""".r

  /**
   * Convert a string of the form address:port into an InetSocketAddress.
   */
  def parseAddressPortString(
    addressPortString: String): InetSocketAddress = {
    addressPortString match {
      case addressPortRE(addressString, portString) =>
        new InetSocketAddress(addressString, portString.toInt)

      case _ => throw new IllegalArgumentException(
        "Bad address:port string '" + addressPortString + "'")
    }
  }

  /**
   * Close the channel after all pending writes are complete.
   */
  def closeOnFlush(channel: Channel) {
    if (channel.isConnected) {
      channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(
        ChannelFutureListener.CLOSE)
    } else {
      channel.close
    }
  }

}