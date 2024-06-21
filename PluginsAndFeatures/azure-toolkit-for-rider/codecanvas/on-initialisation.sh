#!/bin/bash
set -e

echo "Installing SDKMAN!"
curl -s "https://get.sdkman.io" | bash
echo sdkman_auto_answer=true > $HOME/.sdkman/etc/config
source "/root/.sdkman/bin/sdkman-init.sh"

echo "Installing azure-maven-plugins"
sdk install java 8.0.412-amzn
curl -L -O https://github.com/microsoft/azure-maven-plugins/archive/endgame-202405.zip
unzip endgame-202405.zip -d /mnt/jetbrains/work/azure-maven-plugins
cd /mnt/jetbrains/work/azure-maven-plugins/azure-maven-plugins-endgame-202405
./mvnw clean install -f azure-toolkit-libs/pom.xml

echo "Installing Utils"
sdk install java 17.0.11-jbr
cd /mnt/jetbrains/work/azure-tools-for-intellij
./mvnw clean install -DskipTests -f Utils/pom.xml

echo "Building plugin"
cd /mnt/jetbrains/work/azure-tools-for-intellij/PluginsAndFeatures/azure-toolkit-for-rider
./gradlew buildPlugin