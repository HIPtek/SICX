#!/bin/bash
#
# Setups ownCloud to to useSICX. 
# 
# This script setups two ownCloud servers, one for configuring
# external storages and one for the end users. You need to have
# already meta, hydra and sicx started by executing: 
# bash setup_example_sicx.sh
#
# @author Seppo Heikkila <Seppo.Heikkila@cern.ch>

rootpath=`pwd`
homeuser=`whoami`

# Parameters - configure these!
ocver='5.0.10'; # OwnCloud version
#mysqlrootpassword='changeit'; # MySQL root password
mysqlrootpassword='seesam'; # MySQL root password

# Autogenerate parameters
path_to_ownclouds="/var/www/";
path_to_jnlp="${rootpath}/target/jnlp/";
path_to_jnlp_log="${path_to_jnlp}/sicx.debug.txt";
path_to_meta="${rootpath}/meta/";
execute_these=(1 1 1); # owncloud, mysql, mount
eti=0;

# Check that configuration makes sense
if [ ! -d "${path_to_ownclouds}" ]; 
then
    echo "Error: www server path does not exist ${path_to_ownclouds}."
    echo "Please configure valid path to your web server."
    exit -1;
fi
reqdavfs=`which mount.davfs 2>&1 | grep "no mount.davfs in" | wc -l`
if [ "x${reqdavfs}" == "x1" ]
then
    echo "Error: you don't have mount.davfs installed. Please install davfs."
    exit -1;
fi
mysqlpass=`mysql -uroot -p${mysqlrootpassword} -e "show databases" 2>&1 | grep "Access denied for user" |wc -l`
if [ "x${mysqlpass}" == "x1" ]
then
    echo "Error: invalid mysql password provided. Please set mysql password."
    exit -1;
fi

# Extract new versions (better run this as root)
if [ ${execute_these[${eti}]} -eq 1 ]
then
sudo bash -c "pushd ${path_to_ownclouds}
echo -n \"Downloading ownCloud...\"
if [ ! -f \"owncloud-${ocver}.tar.bz2\" ]; 
then
  wget http://download.owncloud.org/community/owncloud-${ocver}.tar.bz2
fi
echo \"Downloading ownCloud finished.\"
if [ ! -f \"owncloud-${ocver}.tar.bz2\" ]; 
then
    echo \"Error: failed to download ownCloud.\"
    exit -1;
fi
echo -n \"Extracting ownCloud for users...\"
tar xvjf owncloud-${ocver}.tar.bz2 &> /dev/null
mv owncloud oc${ocver}a
ln -s oc${ocver}a sicx
echo \" done.\"
echo -n \"Extracting ownCloud for external storages...\"
cp -R oc${ocver}a oc${ocver}b
ln -s oc${ocver}b esec
echo \" done.\"
sudo chown -R www-data:www-data sicx esec oc${ocver}a oc${ocver}b
ls -laF sicx
ls -laF esec
popd"
fi

# Setup mysql
let "eti++";
if [ ${execute_these[${eti}]} -eq 1 ]
then
echo -n "Configuring mysql...";
ocv=${ocver//[.]/};
mysql -uroot -p${mysqlrootpassword} <<QUERY_INPUT
CREATE DATABASE sicx_oc${ocv}a;
GRANT ALL ON sicx_oc${ocv}a.* TO sicx_oc${ocv}a@localhost IDENTIFIED BY "oc${ocv}a";
QUERY_INPUT
mysql -uroot -p${mysqlrootpassword} <<QUERY_INPUT
CREATE DATABASE sicx_oc${ocv}b;
GRANT ALL ON sicx_oc${ocv}b.* TO sicx_oc${ocv}b@localhost IDENTIFIED BY "oc${ocv}b";
QUERY_INPUT
echo " done.";
#echo "Database and username (sicx): 'sicx_oc${ocv}a', password: 'oc${ocv}a'.";
#echo "Database and username (esec): 'sicx_oc${ocv}b', password: 'oc${ocv}b'.";
fi

# Mount the SICX
let "eti++";
if [ ${execute_these[${eti}]} -eq 1 ]
then
echo "Mounting SICX as ownCloud backend..."
sudo mkdir -p ${path_to_ownclouds}/oc${ocver}a/data
sudo chown -R www-data:www-data ${path_to_ownclouds}/oc${ocver}a/data
printf "\n\n" | sudo mount.davfs -o gid=www-data,uid=www-data http://localhost:8081/webdav/SecureRoot ${path_to_ownclouds}/oc${ocver}a/data 
sudo chown -R www-data:www-data ${path_to_ownclouds}/oc${ocver}a/data
sudo chmod 0770 ${path_to_ownclouds}/oc${ocver}a/data
echo "Mounting done."
fi

echo ""
echo "Setup done."
echo "You can now access ownCloud with SICX backend at:"
echo "http://localhost/sicx/"
echo "Use the following configuration:"
echo "Database and username (sicx): 'sicx_oc${ocv}a', password: 'oc${ocv}a'.";
echo ""
echo "P.S. ownCloud external storages"
echo "If you want to share external storage (such as Dropbox),"
echo "configure the following username/password account at:"
echo "http://localhost/esec/"
echo "username: estorage"
echo "password: sicx"
echo "Use the following configuration:"
echo "Database and username (esec): 'sicx_oc${ocv}b', password: 'oc${ocv}b'.";
echo "Before you can store stripes to this ownCloud external"
echo "storage, you have to visit once the admin GUI:"
echo "http://localhost:8081/"
