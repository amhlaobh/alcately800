dir=.
if [ "$#" != "0" ] ; then dir=$1 ; fi
echo "Analysing $dir"
java  -DVERBOSE=true -cp .. AnalyseData ${dir}/wanif.rrdinput.*
