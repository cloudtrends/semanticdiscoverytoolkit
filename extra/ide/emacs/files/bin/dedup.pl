#!/usr/bin/perl
#
# dedup.pl
# by Spence Koehler
#
# Script to print out deduplicate strings delimited by a colon, printing those
# that are unique before those that are duplicated while otherwise preserving
# original ordering.
#
# For example:
#   echo "a:b:c:d:b:a" | dedup.pl
# yields:
#   c:d:a:b
#
# This script is used by "cpgen" to remove deplicate elements from a generated
# classpath.
#

my %allhash = ();
my %duphash = ();
my @all = ();

while (<STDIN>) {
		chomp();
    @pieces = split(/:/);
		$j = 0;
		while ($pieces[$j]) {
				if (exists $allhash{$pieces[$j]}) {
						$duphash{$pieces[$j]} = [];
				}
				else {
						$allhash{$pieces[$j]} = [];
						push(@all, $pieces[$j]);
				}
				$j++;
		}
}

# print out unique items
$i = 0;
$c = "";
while ($all[$i]) {
    if (! exists $duphash{$all[$i]}) {
				print "$c";
				print "$all[$i]";
				$c = ":";
    }
    $i++;
}

# print out duplicated items
$i = 0;
while ($all[$i]) {
    if (exists $duphash{$all[$i]}) {
				print "$c";
				print "$all[$i]";
				$c = ":";
    }
    $i++;
}
