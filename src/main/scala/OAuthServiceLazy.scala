import com.github.scribejava.apis.VkontakteApi
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.{OAuthRequest, Verb}
import com.github.scribejava.core.oauth.OAuth20Service

class OAuthServiceLazy(createProducer: () => OAuth20Service) extends Serializable {

  lazy val service: OAuth20Service = createProducer()

  /** Fetches friends list of a user
    *
    *  @param id VK user id
    *  @return Option[String]. Contains friends list JSON response.
    */
  def send(id:String): Option[String] = {
    val request = new OAuthRequest(Verb.GET, s"https://api.vk.com/method/friends.get?user_id=$id&v=5.68")
    val response = service.execute(request)
    if (!response.isSuccessful) None
    else
      response.getBody match {
        case a: String if a.contains("error") => None
        case a: String => Some(a)
      }
  }
}

object OAuthServiceLazy {
  def apply(key: String, secret: String): OAuthServiceLazy = {
    val f = () => {
      val service = (new ServiceBuilder).apiKey(key).apiSecret(secret).build(VkontakteApi.instance)

      sys.addShutdownHook {
        service.close()
      }

      service
    }
    new OAuthServiceLazy(f)
  }
}
