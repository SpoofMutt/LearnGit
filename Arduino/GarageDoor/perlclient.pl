use strict;
use IO::Socket::INET;
use Digest::CRC qw(crc32);

# auto-flush on socket
$| = 1;
 
my $VERSION     = 1;

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
my $req1 = chr(19).chr(1).chr($STRING)."Hello world!".$sfx;
my $req2 = chr(8).chr(1).chr($COMMAND).chr($CLOSE_DOOR).$sfx;
my $req3 = chr(7).chr(1).chr($STATUSREQ).$sfx;
my $req4 = chr(8).chr(1).chr($COMMAND).chr($OPEN_DOOR).$sfx;
 
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
	print "\n";
	print ord(substr( $req,2,1))."_".ord(substr( $req,3,1)).": \t";
	print "sent data of length $size\n";
	 
	# receive a response of up to 1024 characters from server
	my $response = "";
	$socket->recv($response, 1024);
	print "Response.\n";
	my $length = substr( $response, 0, 1 );
	print "Length: ".ord($length)."\t";
	my $action = substr( $response, 2, 1 );
	print "Action:\t".$action.":\t".ord($action)."\n";
	if(ord($action) == $STRING) {
		print "String\n";
		my $strlength = length($response) - 7;
		my @vals = unpack("CCCA${strlength}CCCC",$response);
		print @vals;
		print "\n";
	} elsif(ord($action) == $COMMANDREPLY){
		print "Command Reply\n";
		my @vals = unpack("CCCCCCCC",$response);
		print @vals;
		print "\n";		
	} elsif(ord($action) == $STATUSREPLY){
		print "Status Reply\n";
		my @vals = unpack("CCCCCCCCC",$response);
		print @vals;
		print "\n";		
	}
	print "received response: ".substr($response,0,ord($length))."\n";
	sleep(5);
}
 
# notify server that request has been sent
shutdown($socket, 1);
 
$socket->close();
exit;

