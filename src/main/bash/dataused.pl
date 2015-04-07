open IN, "<dataused.tmp";

$days = 0;
$hours = 0;
while (<IN>) {
    if(m,"data-used">([0-9\.]+?)([a-zA-Z]+)</span>,){   
        $used = $1; 
    };
    if(m,"data-used">[0-9\.]+?([a-zA-Z]+)</span>,){ 
        $unit = $1; 
    };
    if(m,"duration">([0-9]+)</span> days?,){ 
        $days = $1;
    };
    if(m,"duration">([0-9]+)</span> hrs,){ 
        $hours =  $1;
    };
}
print "$used $unit, $days days, $hours hrs\n";
$megs = $used;
if ($unit eq "GB") {
    $megs = $used * 1024;
}

$hoursLeft = $hours + $days * 24;
printf ("Hours left this period: %d\n" , $hoursLeft) ;
if ($hoursLeft != 0) {
    $allowanceLeftPerHour = $megs / $hoursLeft;
    $allowanceLeftPerDay = $allowanceLeftPerHour * 24;
    if ($days > 0) {
        printf ("Allowance/day : %d MB\n" , ($allowanceLeftPerDay )) ;
    } else {
        print "Allowance left : " . $megs . "\n";
    }

}
