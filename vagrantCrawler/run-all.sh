sudo ~/spark-2.2.0-bin-hadoop2.7/bin/spark-class org.apache.spark.deploy.master.Master -h localhost &
sudo ~/spark-2.2.0-bin-hadoop2.7/bin/spark-class org.apache.spark.deploy.worker.Worker  spark://localhost:7077 -c 1 -m 1024M &
sudo ~/kafka-2.11-0.8.2.1/bin/zookeeper-server-start.sh ~/kafka-2.11-0.8.2.1/config/zookeeper.properties &
sudo ~/kafka-2.11-0.8.2.1/bin/kafka-server-start.sh ~/kafka-2.11-0.8.2.1/config/server.properties &
sudo $REDIS_HOME/src/redis-server