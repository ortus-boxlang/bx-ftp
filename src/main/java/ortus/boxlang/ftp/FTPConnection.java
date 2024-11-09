package ortus.boxlang.ftp;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class FTPConnection {

	private FTPClient		client		= new FTPClient();
	private FTPClientConfig	config		= new FTPClientConfig();
	private boolean			stopOnError	= false;

	public void open( String server, Integer port, String username, String password, boolean passive ) throws IOException {
		client.connect( server, port );
		client.login( username, password );

		if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
			client.disconnect();
			throw new BoxRuntimeException( "FTP server refused connection: " + client.getReplyString() );
		}

		if ( passive ) {
			client.enterLocalPassiveMode();
		}
	}

	public void close() throws IOException {
		client.logout();
	}

	public void setStopOnError( boolean stopOnError ) {
		this.stopOnError = stopOnError;
	}

	public void createDir( String dirName ) {
		try {
			client.makeDirectory( dirName );

			this.handleError();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void removeDir( String dirName ) {
		try {
			client.removeDirectory( dirName );

			this.handleError();
		} catch ( IOException e ) {
			this.handleError();
		}
	}

	private void handleError() {
		if ( FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
			return;
		}

		if ( stopOnError ) {
			throw new BoxRuntimeException( "FTP error: " + client.getReplyString() );
		}
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
