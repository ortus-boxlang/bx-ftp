package ortus.boxlang.ftp;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class FTPConnection {

	private FTPClient	client		= new FTPClient();
	private boolean		stopOnError	= false;

	public void open( String server, Integer port, String username, String password, boolean passive ) throws IOException {
		// Connect to the server
		client.connect( server, port );
		Duration timeout = Duration.ofSeconds( 0 );
		client.setDataTimeout( timeout );

		// Login with username and password
		if ( !client.login( username, password ) ) {
			client.disconnect();
			throw new BoxRuntimeException( "FTP server refused connection: " + client.getReplyString() );
		}

		// Check for a positive response
		if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
			client.disconnect();
			throw new BoxRuntimeException( "FTP server refused connection: " + client.getReplyString() );
		}

		int mode = client.getDataConnectionMode();

		if ( passive ) {
			if ( FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE != mode ) {
				client.enterLocalPassiveMode();
			}
		} else {
			if ( FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE != mode ) {
				client.enterLocalActiveMode();
			}
		}
	}

	public int getStatusCode() {
		return client.getReplyCode();
	}

	public String getStatusText() {
		return client.getReplyString();
	}

	public String getWorkingDirectory() throws IOException {
		return client.printWorkingDirectory();
	}

	public void changeDir( String dirName ) throws IOException {
		client.changeWorkingDirectory( dirName );
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

	public Boolean existsFile( String path ) {

		if ( path.equals( "/" ) ) {
			return true;
		}

		FTPFile[] files = null;
		try {
			final String currentPath = client.printWorkingDirectory().trim();
			files = client.listFiles( currentPath );

			if ( files != null ) {
				Boolean result = Arrays.asList( files ).stream()
				    .anyMatch( file -> file.getName().equalsIgnoreCase( path ) );
				return result;
			}
		} catch ( IOException e ) {
			this.handleError();
			return false;
		}

		return false;
	}

	public Boolean existsDir( String dirName ) throws IOException {
		String	pwd		= null;
		Boolean	result	= null;
		try {
			pwd		= client.printWorkingDirectory();
			result	= client.changeWorkingDirectory( dirName );
			this.handleError();
			return result;
		} catch ( IOException e ) {
			this.handleError();
			return false;
		} finally {
			if ( pwd != null )
				client.changeWorkingDirectory( pwd );
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

	public Query listdir() throws IOException {
		FTPFile[] files = client.listFiles();

		if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
			client.disconnect();
			throw new BoxRuntimeException( "FTP error: " + client.getReplyCode() );
		}

		Query result = new Query();

		result.addColumn( Key._name, QueryColumnType.VARCHAR );
		result.addColumn( FTPKeys.isDirectory, QueryColumnType.BIT );
		result.addColumn( FTPKeys.lastModified, QueryColumnType.TIMESTAMP );
		result.addColumn( Key.length, QueryColumnType.INTEGER );
		result.addColumn( Key.mode, QueryColumnType.INTEGER );
		result.addColumn( Key.path, QueryColumnType.VARCHAR );
		result.addColumn( FTPKeys.url, QueryColumnType.VARCHAR );
		result.addColumn( Key.type, QueryColumnType.VARCHAR );
		result.addColumn( FTPKeys.raw, QueryColumnType.VARCHAR );
		result.addColumn( Key.attributes, QueryColumnType.VARCHAR );

		Arrays.asList( files )
		    .stream()
		    .forEach( ( file ) -> result.add( FTPFileToStruct( file ) ) );

		return result;
	}

	public static IStruct FTPFileToStruct( FTPFile file ) {
		return Struct.of(
		    Key._name, file.getName(),
		    FTPKeys.isDirectory, file.isDirectory(),
		    FTPKeys.lastModified, file.getTimestamp(),
		    Key.length, file.getSize(),
		    Key.mode, getMode( file ),
		    Key.path, file.getName(),
		    FTPKeys.url, file.getName(),
		    Key.type, getType( file ),
		    FTPKeys.raw, file.getName(),
		    Key.attributes, file.getName()
		);
	}

	public static String getMode( FTPFile file ) {
		// TODO complete mode representation
		return "000";
	}

	public static String getRaw( FTPFile file ) {
		// TODO complete mode representation: 'drwxrwxrwx 1 0 0 4096 Oct 29 02:54 a sub folder'
		return "drwxrwxrwx 1 0 0 4096 Oct 29 02:54 a sub folder";
	}

	public static String getType( FTPFile file ) {
		return switch ( file.getType() ) {
			case 0 -> "file";
			case 1 -> "directory";
			case 2 -> "symbolic link";
			default -> "unknown";
		};
	}
}
