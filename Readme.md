# Crawler
Test crawling on VK, takes only user ids.

![Arch](https://github.com/Zulek/bcrawler/blob/master/arch.png)

1. Application consumes from Apache Kafka  

2. For consumed id fetches friends list

![2](https://github.com/Zulek/bcrawler/blob/master/vkfetch.png)

3. Turns json response to `List[String]`

![3](https://github.com/Zulek/bcrawler/blob/master/json.png)

4. Appends each id in list to Redis Set and if it not exists already in Set appends to Apache Kafka thus only unique id is produced  

![4](https://github.com/Zulek/bcrawler/blob/master/rediskafkaprod.png)


## BFS
Breadth First Search algorithm works for social networks as almost all users have lists of friends. We can use algorithm and assume user a node and it has edges to other nodes (friends list).
## Commands
First install [Apache Maven](https://maven.apache.org/download.cgi)
### How to build
Use `mvn package` inside folder to build project, it will create jar with dependencies in /target/ folder

Install [Apache Spark 2.2.0](https://spark.apache.org/downloads.html)

### How to run

Download [Apache Kafka](https://kafka.apache.org/downloads) and [Redis](https://redis.io/download) ([on Windows](https://github.com/MicrosoftArchive/redis))

Start Zookeeper and Kafka, data will be stored in /tmp/ folder

inside Kafka folder run commands

`sudo ./bin/zookeeper-server-start.sh ./config/zookeeper.properties`

`sudo ./bin/kafka-server-start.sh ./config/server.properties`

or on Windows in CMD/PowerShell

`./bin/windows/zookeeper-server-start.bat ./config/zookeeper.properties`

`./bin/windows/kafka-server-start.bat ./config/server.properties`

Build and start redis

`sudo ./src/redis-server`

Finally provide 2 arguments: API Key (ID приложения), API Secret (Защищённый ключ) of your VK application and run command inside Spark folder

`./spark-sumbit --class Crawl --master local[*] /path/to/bcrawler/target/crawer-1.1-jar-with-dependencies.jar <APIKEY> <APISECRET>`
