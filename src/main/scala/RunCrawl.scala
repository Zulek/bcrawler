/**
  * Created by Zulek on 27.08.2017.
  */
class RunCrawl {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Usage: Crawl <apiKey / ID ВК приложения> <apiSecret / Защищённый ключ>")
      System.exit(1)
    }
    val crawler = Crawl(args(0),args(1))
    crawler.start()
  }
}
