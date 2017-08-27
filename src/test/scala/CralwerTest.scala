import java.util

import com.github.scribejava.apis.VkontakteApi
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.{OAuthRequest, Verb}
import com.github.scribejava.core.oauth.OAuth20Service
import com.holdenkarau.spark.testing.{StreamingActionBase, StreamingSuiteBase}
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.spark.Accumulator
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka.KafkaTestUtils
import org.json4s.{JsonInput, string2JsonInput}
import org.json4s.native.JsonMethods.parse
import org.scalatest.{BeforeAndAfterAllConfigMap, ConfigMap, FunSuite}

class CralwerTest extends FunSuite with StreamingSuiteBase with StreamingActionBase {

  test("json parsing test") {
    val input = List(List(
      string2JsonInput("""
        {"response":{"count":136,"items":[11,22,33]}}
      """)))
    val expected = List(List(List(11,22,33)))
    testOperation(input, jsonTest _, expected, ordered = false)
  }
  def jsonTest(f: DStream[JsonInput]): DStream[List[Int]] = {
    f.map(friendsList => (parse(friendsList) \\ "items").children.map(js => js.values.toString.toInt))
  }

  test("redis set pop test") {
    val input = List(List("hi"), List("bye"))
    val acc = sc.accumulator(0)
    val cw = countWordsLength(acc)
    runAction(input, cw)
    assert(2 === acc.value)
  }

  def countWordsLength(acc: Accumulator[Int]): (DStream[String] => Unit) = {
    def c(input: DStream[String]): Unit = {
      input.foreachRDD ( rdd => {
        rdd.foreach { message =>
            acc+=RedisConnection.conn.sadd("redistest", message).get.toInt
            RedisConnection.conn.spop("redistest")
        }
      })
    }
    c
  }
}