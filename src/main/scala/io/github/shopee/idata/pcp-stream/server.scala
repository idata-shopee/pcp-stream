package io.github.shopee.idata.pcpstream

import io.github.shopee.idata.pcp.{ BoxFun, PcpClient, PcpServer, Sandbox }

object StreamServer {
  type CallFunc = (String, Int) => _
}

case class StreamServer(clientAcceptName: String, callFun: StreamServer.CallFunc) {
  private val pcpClient = new PcpClient()

  def sendData(streamId: String, data: Any, timeout: Int) =
    callFun(
      pcpClient.toJson(
        pcpClient.call(clientAcceptName, streamId, PcpStream.STREAM_DATA, data)
      ),
      timeout
    )

  def sendEnd(streamId: String, timeout: Int) =
    callFun(
      pcpClient.toJson(
        pcpClient.call(clientAcceptName, streamId, PcpStream.STREAM_END, null)
      ),
      timeout
    )

  def sendError(streamId: String, errMsg: String, timeout: Int) =
    callFun(
      pcpClient.toJson(
        pcpClient.call(clientAcceptName, streamId, PcpStream.STREAM_ERROR, errMsg)
      ),
      timeout
    )

  def streamApi(
      handle: (StreamProducer, List[Any], PcpServer) => _
  ): BoxFun =
    Sandbox.toSanboxFun((params: List[Any], pcpServer: PcpServer) => {
      if (params.length < 1) {
        throw new Exception("missing stream id at the stream request")
      }

      val streamId       = params.last.asInstanceOf[String]
      val streamProducer = StreamProducer(streamId, this)

      handle(streamProducer, params.slice(0, params.length - 1), pcpServer)
    })
}

case class StreamProducer(streamId: String, ss: StreamServer) {
  def sendData(data: Any, timeout: Int = 5 * 60 * 1000) = ss.sendData(streamId, data, timeout)

  def sendEnd(timeout: Int = 5 * 60 * 1000) = ss.sendEnd(streamId, timeout)

  def sendError(errMsg: String, timeout: Int = 5 * 60 * 1000) = ss.sendError(streamId, errMsg, timeout)
}
