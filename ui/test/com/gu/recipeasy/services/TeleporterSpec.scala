package services

import org.scalatest.{ FlatSpec, Matchers }
import play.api.test.WsTestClient.withClient

import scala.concurrent.Await
import scala.concurrent.duration._

class TeleporterSpec extends FlatSpec with Matchers {

  it should "return correctly the composer code of a published article" in {
    withClient { client =>

      import scala.concurrent.ExecutionContext.Implicits.global

      val teleporter = new Teleporter(client)
      val code = teleporter.getInternalComposerCode("info/developer-blog/2016/nov/29/the-guardian-has-moved-to-https")
      val result = Await.result(code, 30.seconds)
      result should be("57e2852fe4b05d653ca5f3d6")
    }
  }

}