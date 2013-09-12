#!/bin/bash
# This script configures a simple test setup of fully functional SICX
# software but using only local servers.
#
# @author Seppo Heikkila <Seppo.Heikkila@cern.ch>

echo "SICX simple example setup started..."

# Check requirements
requirements=( mvn java git screen openssl sed pushd popd ) 
for req in "${requirements[@]}"
do
    hash $req 2>/dev/null || { echo >&2 "Error: you don't have '${req} installed. Please install ${req}."; exit 1; }
done

# Lets start fresh screen
for session in $(screen -ls | grep Detached | grep hax | sed 's/ *\([0-9]\)\..*/\1/')
do
    echo "Killing screen: $session"
    screen -S "${session}" -X quit;
done
screen -dmS "hax"
rootpath=`pwd`

#### -= Setup meta server =-
if [ ! -d "meta" ]; then
    git clone https://github.com/jhahkala/meta.git
    pushd meta
    mvn package
    popd
fi
rm -rf meta/meta-storage.dat meta/users-storage.dat
screen -S "hax" -p 0 -X stuff "cd meta && java -Djavax.net.debug=all -jar target/meta.jar src/test/meta.conf$(printf \\r)"

#### -= Setup hydras =-
if [ ! -d "hhydra" ]; then
    git clone https://github.com/jhahkala/hhydra.git
    pushd hhydra
    mvn package
    popd
    cp hhydra/src/test/cert/*.pem hhydra/
    echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<infinispan xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
            xmlns=\"urn:infinispan:config:5.0\">
  <global />
  <default />
  <namedCache name=\"hhydra\">
    <loaders shared=\"false\">
      <loader
          class=\"org.infinispan.loaders.file.FileCacheStore\"
          fetchPersistentState=\"true\" ignoreModifications=\"false\"
          purgeOnStartup=\"false\">
        <properties>
          <property name=\"location\" value=\"hhydra-storage.dat\" />
        </properties>
      </loader>
    </loaders>
  </namedCache>
  <namedCache name=\"hydraUsers\">
    <loaders shared=\"false\">
      <loader
          class=\"org.infinispan.loaders.file.FileCacheStore\"
          fetchPersistentState=\"true\" ignoreModifications=\"false\"
          purgeOnStartup=\"false\">
        <properties>
          <property name=\"location\" value=\"hhydra-storage.dat\" />
        </properties>
      </loader>
    </loaders>
  </namedCache>
</infinispan>" > hhydra/hhydra-storage.xml
    echo "port=50201
storeConfigFile=hhydra-storage.xml
host=pchip10.cern.ch
sslCertFile=hostcert.pem
sslKey=hostkey.pem
trustStoreDir=${rootpath}/meta/src/test/certificates/
superuser=CN=trusted client,OU=Relaxation,O=Utopia,L=Tropic,C=UG" > hhydra/hhydra.conf
    cp -r hhydra hhydra2
    cp -r hhydra hhydra3
    sed 's/50201/50202/' hhydra/hhydra.conf > hhydra2/hhydra.conf
    sed 's/50201/50203/' hhydra/hhydra.conf > hhydra3/hhydra.conf
fi
# Configure required hostname
etchosts=`grep "pchip10.cern.ch" /etc/hosts | wc -l`
if [ "x${etchosts}" == "x0" ]
then
    echo -n "Updating /etc/hosts..."
    echo "127.0.0.1       pchip10.cern.ch" | sudo tee -a /etc/hosts &> /dev/null
    echo " done."
fi
screen -S "hax" -X screen
screen -S "hax" -p 1 -X stuff "cd hhydra && java -Djavax.net.debug=all -cp target/hhydra.jar org.hydra.server.HydraServer hhydra.conf$(printf \\r)"
screen -S "hax" -X screen
screen -S "hax" -p 2 -X stuff "cd hhydra2 && java -Djavax.net.debug=all -cp target/hhydra.jar org.hydra.server.HydraServer hhydra.conf$(printf \\r)"
screen -S "hax" -X screen
screen -S "hax" -p 3 -X stuff "cd hhydra3 && java -Djavax.net.debug=all -cp target/hhydra.jar org.hydra.server.HydraServer hhydra.conf$(printf \\r)"

#### -= Setup client =-
if [ ! -d "target" ]; then
    mvn package
fi
homeuser=`whoami`
echo "sslKey=/home/${homeuser}/.sicx_data/trusted_client.priv
sslCertFile=/home/${homeuser}/.sicx_data/trusted_client.cert
trustStoreDir=/home/${homeuser}/.sicx_data/truststore
metaService=https\://localhost\:40669/MetaService
tmpPath=/tmp/
sslKeyPasswd=changeit
folder.local=/home/${homeuser}/sicx
hydraConfig=/home/${homeuser}/.sicx_data/hydras.properties" > /home/${homeuser}/.sicx
mkdir -p ~/.sicx_data/
cp ./meta/src/test/cert/trusted_client.cert ~/.sicx_data/
openssl rsa -passin pass:changeit -in ./meta/src/test/cert/trusted_client.priv > ~/.sicx_data/trusted_client.priv
ln -f -s ${rootpath}/meta/src/test/certificates ~/.sicx_data/truststore
pushd meta 
sleep 3 # Sleep so that meta has time to launch properly
java -cp target/meta.jar org.joni.test.meta.client.MetaClient -c src/test/meta-client-trusted.conf addUser --name "CN=trusted client,OU=Relaxation,O=Utopia,L=Tropic,C=UG" --root SecureRoot --sla Open 2>&1 | grep -v SLF4J
popd
echo "servers=test1,test2,test3
test1.url=https://pchip10.cern.ch:50201/HydraService
test2.url=https://pchip10.cern.ch:50202/HydraService
test3.url=https://pchip10.cern.ch:50203/HydraService" > ~/.sicx_data/hydras.properties

echo ""
echo "Setup done."
echo "SICX client and meta+key servers are now running in screen."
echo "You can access this screen e.g. by typing: \"screen -RRDD\"."
echo "To start using SICX, please log in the Vaadin interface (GUI)"
echo "using empty username and password."

screen -S "hax" -X screen
screen -S "hax" -p 4 -X stuff "cd target/jnlp && javaws sicx.jnlp$(printf \\r)"

# Finish by killing everything when requrested
read -p "Press [Enter] to terminate..."
for session in $(screen -ls | grep hax | sed 's/ *\([0-9]\)\..*/\1/')
do
    echo "Killing screen: $session"
    screen -S "${session}" -X quit;
done