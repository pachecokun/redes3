#!/bin/bash
#Automatizar scripts que obtienen la configuracion más actual de los dispositivos
source IP.ini
for valor in ${ROUTERS[*]}
do
./telnet.sh $valor
./ftp_server.sh $valor
done