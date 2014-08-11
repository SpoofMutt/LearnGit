use strict;
use IO::Socket::INET;
use Digest::CRC qw(crc32);

# auto-flush on socket
$| = 1;
 
my $STRING      = 16;
my $COMMAND     = 10;
my $COMMANDREPLY= 45;
my $STATUSREQ   = 21;
my $STATUSREPLY = 33;

my $DOOR_CLOSED  = 0;
my $DOOR_OPENED  = 1;
my $DOOR_OPENING = 2;
my $DOOR_CLOSING = 3;

my $LIGHT_OFF   = 0;
my $LIGHT_ON    = 1;

my $OPEN_DOOR   = 0;
my $CLOSE_DOOR  = 1;

my $sfx = chr(1).chr(2).chr(3).chr(4);
my $req1 = chr(18).chr($STRING)."Hello world!".$sfx;
my $req2 = chr(7).chr($COMMAND).chr($CLOSE_DOOR).$sfx;
my $req3 = chr(6).chr($STATUSREQ).$sfx;
my $req4 = chr(7).chr($COMMAND).chr($OPEN_DOOR).$sfx;
 
# create a connecting socket
my $socket = new IO::Socket::INET (
    PeerHost => '76.25.98.106',
    PeerPort => '55555',
    Proto => 'tcp',
);
die "cannot connect to the server $!\n" unless $socket;
print "connected to the server\n";
 $, = ", ";
# data to send to a server
foreach my $req ($req1, $req2, $req3, $req4) {
	my $size = $socket->send($req);
	print ord(substr( $req,1,1))."_".ord(substr( $req,2,1)).": \t";
	print "sent data of length $size\n";
	 
	# receive a response of up to 1024 characters from server
	my $response = "";
	$socket->recv($response, 1024);
	
	my $action = substr( $response, 1, 1 );
	print $action.":\t".ord($action)."\n";
	if(ord($action) == $STRING) {
		print "String\n";
		my $strlength = length($response) - 6;
		my @vals = unpack("CCA${strlength}CCCC",$response);
		print @vals;
		print "\n";
	} elsif(ord($action) == $COMMANDREPLY){
		print "Command Reply\n";
		my @vals = unpack("CCCCCCC",$response);
		print @vals;
		print "\n";		
	} elsif(ord($action) == $STATUSREPLY){
		print "Status Reply\n";
		my @vals = unpack("CCCCCCCC",$response);
		print @vals;
		print "\n";		
	}
	print "received response: $response\n";
	sleep(5);
}
 
# notify server that request has been sent
shutdown($socket, 1);
 
$socket->close();
exit;

