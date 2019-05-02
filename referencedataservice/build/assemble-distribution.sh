#!/usr/bin/env bash
echo '#######################################################'
echo '# AWS Cloud Build Assemble the Distribution           #'
echo '#######################################################'
echo "Build running from ->$(pwd)"
mkdir referencedataservice-distribution
cd referencedataservice-distribution
mkdir auditstore
mkdir bin
mkdir conf
mkdir fxRates
mkdir logs
mkdir participants
mkdir partyCache
mkdir partyEvents
mkdir partyStartup
mkdir users
mkdir src
cd ..
ls -lf
cp -R auditstore/*   referencedataservice-distribution/auditstore/
cp -R bin/*          referencedataservice-distribution/bin/
cp -R conf/*         referencedataservice-distribution/conf/
cp -R fxRates/*      referencedataservice-distribution/fxRates/
cp -R logs/*         referencedataservice-distribution/logs/
cp -R participants/* referencedataservice-distribution/participants/
cp -R partyCache/*   referencedataservice-distribution/partyCache/
cp -R partyEvents/*  referencedataservice-distribution/partyEvents/
cp -R partyStartup/* referencedataservice-distribution/partyStartup/
cp -R users/*        referencedataservice-distribution/users/
cp -R ./src/*        referencedataservice-distribution/src/
cp   pom.xml         referencedataservice-distribution
cp   README.md       referencedataservice-distribution

cp target/droitfintech-referencedataservice.jar referencedataservice-distribution/bin/
tar -czvf  target/referencedataservice-distribution.tar.gz referencedataservice-distribution/








