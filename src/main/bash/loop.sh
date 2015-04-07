#!/bin/sh

while true; do 
    vals=$(curl --silent http://192.168.1.1/goform/getWanInfo | perl -ne 'm/"upload":(\d+?),.*"download":(\d+?),.*"usage":(\d+?),/; print "$1:$2:$3";') 
    date=$(date +%s)
    rrdtool update alcately800.rrd ${date}:${vals}
    sleep 5; 
done

while true;do rrdtool update alcately800.rrd N:$(curl --silent http://192.168.1.1/goform/getWanInfo | perl -ne 'm/"upload":(\d+?),.*"download":(\d+?),.*"usage":(\d+?),/; print "$3:$3";') ;  rrdtool.exe graph alcately800-day.png --start -600 DEF:usage=alcately800.rrd:usage:AVERAGE DEF:rate=alcately800.rrd:rate:AVERAGE LINE1:usage#0000FF:"Usage" LINE2:rate#FF0000:"Rate";sleep 2; echo . ; done

