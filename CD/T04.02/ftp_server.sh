#!/bin/bash
#Obtener todas las configuraciones de los routers de la red
ftp -n $1 <<SCRIPT
user rcp rcp
binary
get startup-config "$1".conf
quit
SCRIPT