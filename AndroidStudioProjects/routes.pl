use strict;
use Math::Trig 'great_circle_distance';

my @r1 = (
38.931272,-104.726207, # <= Lower Entry
38.931001,-104.725966,
38.930487,-104.72698,
38.930283,-104.726803, # <= Lower Approach
38.93012,-104.726524,
38.930128,-104.726057,
38.930316,-104.725623,
38.930532,-104.725232  # Home
);

my @r2 = (
38.932173,-104.724576, # <= Upper Entry
38.931852,-104.724324,
38.931581,-104.724137, # <= Upper Approach
38.931284,-104.724094,
38.931038,-104.724067,
38.931001,-104.724244,
38.930809,-104.724689,
38.930586,-104.725126  # Home
);

my @r3 = (
38.931451,-104.726333, # <= Bridle Pass Lower Entry
38.93229,-104.724676   # <= Bridle Pass Upper Entry
);

crunch(\@r1);
crunch(\@r2);
crunch(\@r3);


#print "v" x 60 . "\n";
#path(@r1,backwards(@r2), backwards(@r3));
#path(@r1,backwards(@r2), backwards(@r3));
#path(@r3,@r2,backwards(@r1));
#path(@r3,@r2,backwards(@r1));
path(@r1,backwards(@r2), backwards(@r3),@r1,backwards(@r2), backwards(@r3),
     @r3,@r2,backwards(@r1),@r3,@r2,backwards(@r1));

sub path {
  my @data = @_;
  crunch(\@data);
  my @lat = ();
  my @lon = ();
  my @acc = ();
  for my $ndx (0 .. $#data) {
    last if ($ndx * 2 > $#data);
    push @lat, $data[$ndx * 2];
    push @lon, $data[$ndx * 2 + 1];
    push @acc, (rand(1) + 3.0);
  }
  print "public static final double[] WAYPOINTS_LAT = {\n";
  for my $ndx (0 .. $#lat) {
    print $lat[$ndx];
    if($ndx < $#lat) {
      print ",\n";
    } else {
      print "};\n\n";
    }
  }
  print "public static final double[] WAYPOINTS_LNG = {\n";
  for my $ndx (0 .. $#lon) {
    print $lon[$ndx];
    if($ndx < $#lon) {
      print ",\n";
    } else {
      print "};\n\n";
    }
  }
  print "public static final float[] WAYPOINTS_ACCURACY = {\n";
  for my $ndx (0 .. $#acc) {
    print $acc[$ndx]."f";
    if($ndx < $#acc) {
      print ",\n";
    } else {
      print "};\n\n";
    }
  }
}

sub backwards {
  my @data = @_;
  my @newdata = ();
  for my $x (0 .. $#data) {
    my $nlat = $x * 2;
    my $nlon = $x * 2 + 1;
    last if($nlon >= $#data);
    unshift @newdata, ($data[$nlat],$data[$nlon]);
  }
  return @newdata;
}

#Lat/Lon to rad
sub NESW { deg2rad($_[0]), deg2rad(90 - $_[1]) }

sub crunch {
  my $ary = shift;
  my @data = @$ary;
  my $again = 1;
  while($again) {
    my @tmp = ();
    for my $x (0 .. $#data) {
      my $nlat1 = $x * 2;
      my $nlon1 = $x * 2 + 1;
      my $nlat2 = ($x + 1) * 2;
      my $nlon2 = ($x + 1) * 2 + 1;
      push @tmp, ($data[$nlat1],$data[$nlon1]);
      last if($nlon2 > $#data);
#      print $data[$nlat1]."\t".$data[$nlon1]."\t\t";
#      print $data[$nlat2]."\t".$data[$nlon2]."\t\t";
      my $dist = haversine_m($data[$nlat1],$data[$nlon1],$data[$nlat2],$data[$nlon2]);
#      print $dist."\n";
      if ($dist > 40) {
        my $tlat = ($data[$nlat1] + $data[$nlat2])/2.0;
        my $tlon = ($data[$nlon1] + $data[$nlon2])/2.0;
#        print "\t\tPushing :\t".$tlat."\t".$tlon."\n";
        push @tmp, ($tlat, $tlon);
      }
    }
    if($#tmp == $#data) {
      $again = 0;
    } else {
#      print "=" x 60 . "\n";
      @data = @tmp;
    }
  }
  @$ary = @data;
}

sub haversine_m
{
  my ($lat1, $long1, $lat2, $long2) = @_;
  my $d2r = Math::Trig::pi/180.0;
  my $dlong = ($long2 - $long1) * $d2r;
  my $dlat = ($lat2 - $lat1) * $d2r;
  my $a = (sin($dlat/2.0) ** 2) + cos($lat1*$d2r) * cos($lat2*$d2r) * (sin($dlong/2.0) ** 2);
  my $c = 2 * atan2(sqrt($a), sqrt(1-$a));
  my $d = 6367 * $c;
  return $d*1000;
}
