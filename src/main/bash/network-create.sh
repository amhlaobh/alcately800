rrdtool.exe create alcately800.rrd --step=5  DS:usage:COUNTER:30:U:U DS:upload:COUNTER:30:U:U DS:download:COUNTER:30:U:U \
DS:usageGauge:GAUGE:30:U:U DS:uploadGauge:GAUGE:30:U:U DS:download:GAUGEGauge:30:U:U \
RRA:AVERAGE:0.5:1:17280  \
RRA:AVERAGE:0.5:17280:31 \
RRA:MAX:0.5:1:17280  \
RRA:MAX:0.5:17280:31

while true;do rrdtool update alcately800.rrd N:$(curl --silent http://192.168.1.1/goform/getWanInfo | perl -ne 'm/"upload":(\d+?),.*"download":(\d+?),.*"usage":(\d+?),/; print "$1:$2:$3";') ; sleep 2; done
     while true;do rrdtool update alcately800.rrd N:$(curl --silent http://192.168.1.1/goform/getWanInfo | perl -ne 'm/"upload":(\d+?),.*"download":(\d+?),.*"usage":(\d+?),/; print "$3:$3";') ;  rrdtool.exe graph alcately800-day.png --start -600 DEF:usage=alcately800.rrd:usage:AVERAGE DEF:rate=alcately800.rrd:rate:AVERAGE LINE1:usage#0000FF:"Usage" LINE2:rate#FF0000:"Rate";sleep 2; echo . ; done


