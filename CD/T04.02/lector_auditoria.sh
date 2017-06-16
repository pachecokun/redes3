#!/bin/bash
source IP.ini
for valor in ${ROUTERS[*]}
do
./telnet.sh $valor
./auditar_enrutador.sh $valor
done