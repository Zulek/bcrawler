sudo apt-get install -y pdsh
sudo wget http://apache-mirror.rbc.ru/pub/apache/hadoop/common/hadoop-2.8.1/hadoop-2.8.1.tar.gz
sudo tar -xzvf hadoop-2.8.1.tar.gz
sudo mv hadoop-2.8.1 /usr/local/hadoop
mkdir ~/input
cp /usr/local/hadoop/etc/hadoop/*.xml ~/input
sudo echo "export HADOOP_HOME=/usr/local/hadoop/" >> .bashrc