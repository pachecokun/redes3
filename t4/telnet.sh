#!/bin/bash
# Abre una sesion de telnet para obtener la configuracion
{
echo open $1
sleep 1
echo rcp
sleep 1
echo rcp
sleep 1
echo enable
sleep 1
echo copy running-config startup-config
sleep 1
echo exit
sleep 1
} | telnet