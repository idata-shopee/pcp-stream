package io.github.shopee.idata.pcpstream

import io.github.shopee.idata.pcp.{ BoxFun, PcpClient, PcpServer, Sandbox }

class PcpStreamTest extends org.scalatest.FunSuite {
  test("base") {
    val pcpClient    = new PcpClient()
    val streamClient = StreamClient()

    val clientSide = new PcpServer(
      new Sandbox(
        Map[String, BoxFun](
          "stream_accept" -> streamClient.getPcpStreamAcceptBoxFun()
        )
      )
    )

    val streamServer = StreamServer("stream_accept", (command: String, timeout: Int) => {
      clientSide.execute(command)
    })

    val serverSide = new PcpServer(
      new Sandbox(
        Map[String, BoxFun](
          "streamApi" -> streamServer.streamApi(
            (streamProducer: StreamProducer, params: List[Any], pcpServer: PcpServer) => {
              val seed = params(0).asInstanceOf[String]
              streamProducer.sendData(seed + "1", 10)
              streamProducer.sendData(seed + "2", 10)
              streamProducer.sendData(seed + "3", 10)
              streamProducer.sendEnd()
              null
            }
          )
        )
      )
    )

    var result = ""

    val callExp = streamClient.streamCall("streamApi", "(", (t: Int, d: Any) => {
      if (t == PcpStream.STREAM_DATA) {
        result += d.asInstanceOf[String]
      }
    })
    val cmd = pcpClient.toJson(callExp)
    serverSide.execute(cmd)

    assert(result == "(1(2(3")
    streamClient.clean()
  }
}