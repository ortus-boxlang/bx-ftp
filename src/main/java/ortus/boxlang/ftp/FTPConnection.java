package ortus.boxlang.ftp;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class FTPConnection {

	private FTPClient		client	= new FTPClient();
	private FTPClientConfig	config	= new FTPClientConfig();

	public void open( String server, String username, String password ) throws IOException {
		open( server, 22, username, password );
	}

	public void open( String server, Integer port, String username, String password ) throws IOException {
		client.connect( server, port );
		client.login( username, password );

		if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
			client.disconnect();
			throw new BoxRuntimeException( "FTP server refused connection: " + client.getReplyString() );
		}

		client.enterLocalPassiveMode();
	}

	public void close() throws IOException {
		client.logout();
	}

	public String[] listdir() throws IOException {
		FTPFile[] files = client.listFiles();

		if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
			client.disconnect();
			throw new BoxRuntimeException( "FTP error: " + client.getReplyString() );
		}

		return Arrays.asList( files )
		    .stream()
		    .map( FTPFile::getName )
		    .toArray( ( x ) -> new String[ x ] );
	}
}
