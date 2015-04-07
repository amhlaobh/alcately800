rrdtool create alcately800.rrd \
 --step 5 \
 --start 1381816217 \
 DS:usage:GAUGE:20:0:U \
 DS:usageRate:COUNTER:20:0:U \
 RRA:AVERAGE:0.5:1:17280 \
 RRA:AVERAGE:0.5:1440:112 \
 RRA:AVERAGE:0.5:17280:30 \
 RRA:MAX:0.5:1:17280 \
 RRA:MAX:0.5:1440:112 \
 RRA:MAX:0.5:17280:30 


cat wanif.data | while read l ; do
  vals=$(echo "${l}" | perl -ne 'm/(\d+?):.*?"upload":(\d+?),"download":(\d+?),.*"usage":(\d+?),/; print "$1:$4:$4";')
  echo ${vals}
  rrdtool update alcately800.rrd ${vals}
done
