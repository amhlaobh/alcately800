set -o errexit
set -o nounset

# echo "create table network (timestamp integer primary key, usage integer,usageRate integer);" | ./sqlite3.exe wanif.db
# echo "create index tsindex on network(timestamp);" | ./sqlite3.exe wanif.db

# import csv file
# create table network (timestamp integer, usage integer,usageRate integer);
# .separator ":"
# .import wanif.rrdinput.uniq network
# .import wanif.rrdinput.20131025 network
# .import wanif.rrdinput.20131026 network
# .import wanif.rrdinput.20131027 network


for i in `cat ${1} `; do 
     v=` echo $i | tr : , `
     echo "insert into network values (${v});" | ./sqlite3.exe wanif.db 
done

# echo -e ".separator :\nselect \"update alcately800.rrd \",* from network;" | ./sqlite3.exe wanif.db > tmp
