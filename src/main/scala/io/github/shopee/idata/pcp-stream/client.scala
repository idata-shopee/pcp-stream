package io.github.shopee.idata.pcpstream

import java.util.concurrent.ConcurrentHashMap
import java.util.UUID.randomUUID
import scala.collection.convert.decorateAsScala._
import io.github.shopee.idata.pcp.{ BoxFun, CallResult, PcpClient, PcpServer, Sandbox }

object StreamClient {
  type StreamCallbackFunc = (Int, Any) => _
}

case class StreamClient() {
  private val callbackMap = new ConcurrentHashMap[String, StreamClient.StreamCallbackFunc]().asScala
  private val pcpClient   = new PcpClient()

  def clean() {
    callbackMap.clear()
  }

  // register callback
  def streamCallback(callbackFunc: StreamClient.StreamCallbackFunc): String = {
    val id = randomUUID().toString
    callbackMap(id) = callbackFunc
    id
  }

  // accept stream response from server
  def accept(sid: String, t: Int, d: Any) {
    if (t != PcpStream.STREAM_DATA &&
        t != PcpStream.STREAM_END &&
        t != PcpStream.STREAM_ERROR) {
      throw new Exception("unepxpected stream chunk type.")
    }

    if (!callbackMap.contains(sid)) {
      throw new Exception(s"missing stream callback function for id: ${sid}")
    }

    val fun = callbackMap(sid)

    // when finished, remove callback from map
    if (t == PcpStream.STREAM_END || t == PcpStream.STREAM_ERROR) {
      callbackMap.remove(sid)
    }

    fun(t, d)
  }

  // define the stream accept sandbox function at client
  def getPcpStreamAcceptBoxFun(): BoxFun =
    Sandbox.toSanboxFun((params: List[Any], pcpServer: PcpServer) => {
      if (params.length < 3) {
        throw new Exception(streamFormatError(params))
      }

      accept(params(0).asInstanceOf[String], params(1).asInstanceOf[Int], params(2))
    })

  private def streamFormatError(params: List[Any]): String =
    s"stream chunk format: [streamId: string, t: int, d: interface{}]. args=${params}"

  // simple conversion for stream calling
  // (streamFunName, ...params, streamCallback)
  def streamCall(streamFunName: String, params: Any*): CallResult = {
    if (params.length < 1) {
      throw new Exception("missing stream callback function for stream call.");
    }

    val callbackFun   = params.last.asInstanceOf[StreamClient.StreamCallbackFunc]
    val callbackParam = streamCallback(callbackFun)

    val args = params.slice(0, params.length - 1) :+ callbackParam

    pcpClient.call(streamFunName, args: _*)
  }
}
