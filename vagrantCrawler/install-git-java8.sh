sudo apt-get -y update
sudo apt-get install -y git
git clone https://github.com/Zulek/bcrawler
sudo apt-get install -y maven
cd bcrawler
mvn package
cd ~
echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main" | \
  sudo tee /etc/apt/sources.list.d/webupd8team-java.list
echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main" | \
  sudo tee -a /etc/apt/sources.list.d/webupd8team-java.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
sudo apt-get update
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | sudo debconf-set-selections
sudo apt-get install -y oracle-java8-installer
