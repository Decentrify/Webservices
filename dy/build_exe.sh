#!/bin/bash

set -e

# This reads the pom.xml file in the current directory, and extracts the first version element in the xml version element.
version=`grep -o -a -m 1 -h -r "version>.*</version" pom.xml | head -1 | sed "s/version//g" | sed "s/>//" | sed "s/<\///g"`

echo "version is: $version"

dist=dy-$version
certs_dir=/home/jdowling/Dropbox/karamel/certs

mvn clean package
cd target

#create linux archive
cp -r appassembler/* $dist/
#cp ../README.linux $dist/README.txt
tar zcf ${dist}.tgz $dist
mv $dist ${dist}-linux


#create jar archive
mkdir ${dist}-jar
cp dy-webui-${version}-shaded.jar ${dist}-jar/dy-webui-${version}.jar
cp -r appassembler/conf/* ${dist}-jar/ 
#cp ../README.jar ${dist}-jar/README.txt 
zip -r ${dist}-jar.zip $dist-jar

#scp ${dist}.tgz glassfish@snurran.sics.se:/var/www/karamel.io/sites/default/files/downloads/
#scp ${dist}-jar.zip glassfish@snurran.sics.se:/var/www/karamel.io/sites/default/files/downloads/

echo "Now building windows distribution"
cd ..
mvn -Dwin clean package
cd target

#
# Instructions for signing the .exe were taken from here:
# http://development.adaptris.net/users/lchan/blog/2013/06/07/signing-windows-installers-on-linux/
#
osslsigncode -spc ${certs_dir}/authenticode.spc -key ${certs_dir}/authenticode.key -t http://timestamp.verisign.com/scripts/timstamp.dll -in dy.exe -out dy-signed.exe
mv dy-signed.exe $dist/dy.exe
#create windows archive
#cp ../README.windows $dist/README.txt
cp -r ../src/main/resources/icons $dist/
zip -r ${dist}.zip $dist

mv ${dist} ${dist}-windows

#scp ${dist}.zip glassfish@snurran.sics.se:/var/www/karamel.io/sites/default/files/downloads/
cd ..

echo "finished releasing karamel"