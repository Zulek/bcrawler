sudo apt-get update
sudo apt-get install default-jre
sudo useradd -m kafka
sudo adduser kafka sudo

wget http://apache-mirror.rbc.ru/pub/apache/zookeeper/zookeeper-3.4.9/zookeeper-3.4.9.tar.gz
tar -zxf zookeeper-3.4.9.tar.gz

mkdir -p ~/kafka
cd kafka
wget http://mirror.cc.columbia.edu/pub/software/apache/kafka/0.8.2.1/kafka_2.11-0.8.2.1.tgz
tar xvzf kafka_2.11-0.8.2.1.tgz
sudo echo "export KAFKA_HOME=~/kafka_2.11-0.8.2.1" >> .bashrc