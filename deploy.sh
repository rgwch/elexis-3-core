#!/usr/bin/env bash


PRODUCTS=${WEBSPACE}/products/elexis-core
P2=${WEBSPACE}/p2/elexis-core

mkdir -p ${PRODUCTS}/${BUILD_NUMBER}
mkdir -p ${P2}/${BUILD_NUMBER}

cp ch.elexis.core.p2site/target/products/*.zip ${PRODUCTS}/${BUILD_NUMBER}
unzip ch.elexis.core.p2site/target/repository/content.jar -d ch.elexis.core.p2site/target/repository/
cp -R ch.elexis.core.p2site/target/repository/* ${P2}/${BUILD_NUMBER}

rm ${PRODUCTS}/latest
ln -s ${PRODUCTS}/${BUILD_NUMBER} ${PRODUCTS}/latest
rm ${P2}/latest
ln -s ${P2}/${BUILD_NUMBER} ${P2}/latest
