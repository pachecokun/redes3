#!/bin/bash
HOST=$1
USER='rcp'
PASSWD='rcp'
FILE='startup-config'
ftp -n $HOST <<SCRIPT
quote user $USER
quote pass $PASSWD
binary
put $HOST.conf $FILE
quit
SCRIPT
exit 0
