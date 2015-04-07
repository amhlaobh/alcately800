rrdtool graph alcatel-abs.png --title "2 hour max" --start -2hour --vertical-label "bytes" --lower-limit 0 --width 1000 --height 300 DEF:usage=alcately800.rrd:usage:MAX LINE2:usage#FF0000:"Usage" 
rrdtool graph alcatel-abs5min.png --title "5 min max" --start -5minutes --vertical-label "bytes" --lower-limit 0 --width 100 --height 300 DEF:usage=alcately800.rrd:usage:MAX LINE2:usage#FF0000:"Usage" 

rrdtool graph alcatel-rate.png --title "1 hour rate" --start -1hour --vertical-label "bytes/second" --lower-limit 0 --width 1000 --height 300 DEF:usageRate=alcately800.rrd:usageRate:AVERAGE VDEF:avg=usageRate,AVERAGE LINE2:avg#00FF00:"Avg" LINE1:usageRate#FF0000:"Usage Rate" 
rrdtool graph alcatel-rate1min.png --title "1 min rate" --start -1minutes --vertical-label "bytes/second" --lower-limit 0 --width 100 --height 300 DEF:usageRate=alcately800.rrd:usageRate:AVERAGE VDEF:avg=usageRate,AVERAGE LINE2:avg#00FF00:"Avg" LINE1:usageRate#FF0000:"Usage Rate" 

rrdtool graph alcatel-abs-week.png --title "1 week average" --start "20140313" --vertical-label "bytes" --lower-limit 0 DEF:usage=alcately800.rrd:usage:MAX  VDEF:avg=usage,AVERAGE LINE2:avg#00FF00:"Avg" LINE2:usage#FF0000:"Usage" 

rrdtool graph alcatel-abs-days.png --title "1day max" --start -1days --vertical-label "bytes" --lower-limit 0 DEF:usage=alcately800.rrd:usage:MAX LINE2:usage#FF0000:"Usage" 

rrdtool graph alcatel-rate-days.png --title "1day rate" --start -1days --vertical-label "bytes/second" --lower-limit 0 DEF:usageRate=alcately800.rrd:usageRate:AVERAGE LINE1:usageRate#FF0000:"Usage Rate" 

