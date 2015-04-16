#!/bin/bash
set -o nounset
set -o errexit

du=dataused.tmp
echo "myself = $0"
mydir=`readlink -f $0`
myfile=`basename $mydir`
echo "link = $mydir"
mydir=`dirname $mydir` 
#script=`dirname $0`/${0%%.sh}.pl
script=${mydir}/${myfile%%.sh}.pl
#script=`readlink -f ${0%%.sh}.pl`
#curl -s http://add-on.ee.co.uk/status >  ${du}
perl ${script} $du

#cat ${du} | perl -ne 'if(m,"duration">([a-zA-Z0-9\.]+?)</span> days,){ print $1; }; if(m,"duration">([0-9]+)</span>(.+),){print " $1 $2";}'
#gb=`cat ${du} | perl -ne 'if(m,"data-used">([0-9\.]+?)([a-zA-Z]+)</span>,){ print "$1"; };'`
#unit=`cat ${du} | perl -ne 'if(m,"data-used">[0-9\.]+?([a-zA-Z]+)</span>,){ print "$1"; };'`
#days=`cat ${du} | perl -ne 'if(m,"duration">([0-9]+)</span> days,){print "$1";}'`
#hrs=`cat ${du} | perl -ne 'if(m,"duration">([0-9]+)</span> hrs,){print "$1";}'`
#echo "$gb $unit, $days days, $hrs hrs"
