object RunCrawler {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Usage: Crawl <apiKey / ID ВК приложения> <apiSecret / Защищённый ключ>")
      System.exit(1)
    }
    val crawler = Crawl(args(1),args(2))
    crawler.start()
  }
}
