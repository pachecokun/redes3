#!/bin/bash
HOST=$1
USER='rcp'
PASSWD='rcp'
FILE='startup-config'
ftp -n $HOST <<SCRIPT
quote user $USER
quote pass $PASSWD
binary
get $FILE $HOST.chng
quit
SCRIPT
diff $HOST.conf $HOST.chng > $HOST.diff
exit 0
