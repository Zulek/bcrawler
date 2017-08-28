sudo apt-get install -y build-essential
sudo apt-get install -y tcl8.5
wget http://download.redis.io/redis-stable.tar.gz
tar xvzf redis-stable.tar.gz
cd redis-stable
make
cd ~
sudo echo "export REDIS_HOME=~/redis-stable" >> .bashrc

