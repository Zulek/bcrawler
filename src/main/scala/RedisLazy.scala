import com.redis.RedisClient

class RedisLazy(createProducer: () => RedisClient) extends Serializable {

  lazy val conn: RedisClient = createProducer()

  /** Adds message to Redis Set "test"
    *
    *  @param message Message
    *  @return Option[Long]. If Some(1L) value was added, Some(0L) already exists in Set.
    */
  def send(message: String): Option[Long] = {
    conn.sadd("test",message)
  }
}

object RedisLazy {
  def apply(): RedisLazy = {
    val f = () => {
      val redisConn = new RedisClient("localhost", 6379)

      sys.addShutdownHook {
        redisConn.quit
      }

      redisConn
    }
    new RedisLazy(f)
  }
}