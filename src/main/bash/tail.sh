 tail -f  wanif.data | while read l ; do   vals=$(echo "${l}" | perl -ne 'm/(\d+?):.*?"upload":(\d+?),"download":(\d+?),.*"usage":(\d+?),/; print "$1:$4:$4";');   echo ${vals};   rrdtool update alcately800.rrd ${vals}; done