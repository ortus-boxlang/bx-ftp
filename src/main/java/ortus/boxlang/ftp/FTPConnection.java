package ortus.boxlang.ftp;

import java.io.IOException;
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
			throw new BoxRuntimeException( "FTP error: " + client.getReplyString() );
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
