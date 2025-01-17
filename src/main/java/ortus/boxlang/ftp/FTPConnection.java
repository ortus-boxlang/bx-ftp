/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.ftp;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;

/**
 * This class is a wrapper around the Apache Commons Net FTPClient class. It
 * provides a simplified interface for working with FTP servers.
 */
public class FTPConnection {

	/**
	 * --------------------------------------------------------------------------
	 * Defaults
	 * --------------------------------------------------------------------------
	 */

	public static final int			DEFAULT_PORT			= 21;
	public static final boolean		DEFAULT_PASSIVE			= false;
	public static final String		DEFAULT_USERNAME		= "anonymous";
	public static final String		DEFAULT_PASSWORD		= "anonymous";
	public static final boolean		DEFAULT_STOP_ON_ERROR	= true;
	// In Seconds
	public static final Duration	DEFAULT_TIMEOUT			= Duration.ofSeconds( 30 );

	// Enum for return types: query and array
	public enum ReturnType {
		QUERY,
		ARRAY
	}

	/**
	 * --------------------------------------------------------------------------
	 * Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The FTPClient object used to communicate with the server.
	 */
	private FTPClient		client		= new FTPClient();

	/**
	 * If true, an exception will be thrown if an error occurs. If false, the
	 * error will be ignored.
	 */
	private boolean			stopOnError	= DEFAULT_STOP_ON_ERROR;

	/**
	 * The name of the connection.
	 */
	private Key				name;

	/**
	 * The username for the connection.
	 */
	private String			username;

	/**
	 * The BoxLang logger to use
	 */
	private BoxLangLogger	logger;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Build a Connection
	 *
	 * @param name The name of the connection
	 */
	public FTPConnection( Key name, BoxLangLogger logger ) {
		this.name	= name;
		this.logger	= logger;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Service Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Connect to an FTP server and open a connection. If the username/password are null, the default anonymous user will be used.
	 * If passive is null, the default is false.
	 *
	 * @param server   The server to connect to
	 * @param port     The port to connect to
	 * @param username The username to use
	 * @param password The password to use
	 * @param passive  Whether to use passive mode
	 * @param timeout  The timeout in seconds as a Duration
	 *
	 * @throws IOException If an error occurs while connecting
	 */
	public FTPConnection open( String server,
	    Integer port,
	    String username,
	    String password,
	    boolean passive,
	    Duration timeout ) throws IOException {
		// Verify that the required parameters are present or default them
		Objects.requireNonNull( server, "Server is required" );
		Objects.requireNonNullElse( port, DEFAULT_PORT );
		Objects.requireNonNullElse( passive, DEFAULT_PASSIVE );
		Objects.requireNonNullElse( username, DEFAULT_USERNAME );
		Objects.requireNonNullElse( password, DEFAULT_PASSWORD );
		Objects.requireNonNullElse( timeout, DEFAULT_TIMEOUT );

		// Store for future reference
		this.username = username;

		// Connect to the server
		this.client.connect( server, port );
		this.client.setDataTimeout( timeout );

		// Login with username and password
		if ( !this.client.login( username, password ) ) {
			this.client.disconnect();
			this.logger.error( "FTP server refused connection: " + this.client.getReplyString() );
			throw new BoxRuntimeException( "FTP server refused connection: " + this.client.getReplyString() );
		}

		// Check for a positive response
		if ( !FTPReply.isPositiveCompletion( this.client.getReplyCode() ) ) {
			this.client.disconnect();
			this.logger.error( "FTP server refused connection: " + this.client.getReplyString() );
			throw new BoxRuntimeException( "FTP server refused connection: " + this.client.getReplyString() );
		}

		int mode = this.client.getDataConnectionMode();

		if ( passive ) {
			if ( FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE != mode ) {
				this.client.enterLocalPassiveMode();
			}
		} else {
			if ( FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE != mode ) {
				this.client.enterLocalActiveMode();
			}
		}

		this.logger.info( "FTP connection [{}] opened in [{}] mode.", this.name, passive ? "passive" : "active" );

		return this;
	}

	/**
	 * Retrieve a file from the FTP server and write it out to a local file.
	 *
	 * @param remoteFile The name of the file to copy
	 * @param localFile  The path of hte file to save
	 *
	 * @throws IOException
	 */
	public void getFile( String remoteFile, String localFile ) throws IOException {
		java.io.File file = new java.io.File( localFile );
		if ( file.exists() ) {
			throw new BoxRuntimeException( "Error: Local file already exists: " + localFile );
		}
		try ( OutputStream outputStream = new FileOutputStream( localFile ) ) {
			client.retrieveFile( remoteFile, outputStream );
		}
		this.handleError();
	}

	/**
	 * Put a file on the remote server
	 *
	 * @param localFile  The file path of the local file you want to copy
	 * @param remoteFile The name of the remote file you want to create/update
	 *
	 * @throws IOException
	 */
	public void putFile( String localFile, String remoteFile ) throws IOException {
		java.io.File file = new java.io.File( localFile );
		if ( !file.exists() ) {
			throw new BoxRuntimeException( "Error: Local file does not exist: " + localFile );
		}
		try ( java.io.FileInputStream inputStream = new java.io.FileInputStream( localFile ) ) {
			client.storeFile( remoteFile, inputStream );
		}
		this.handleError();
	}

	/**
	 * Remove a file on the FTP server
	 *
	 * @param remoteFile The name of the file you want to remove
	 *
	 * @return True if the file was removed, false otherwise
	 */
	public boolean remove( String remoteFile ) {
		try {
			return client.deleteFile( remoteFile );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Return the most recent reply code from the FTP Server
	 *
	 * @return
	 */
	public int getStatusCode() {
		return client.getReplyCode();
	}

	/**
	 * Get the status text associated with the most recent reply from the FTP server
	 *
	 * @return
	 */
	public String getStatusText() {
		return client.getReplyString();
	}

	/**
	 * Get the current selected working directory on the FTP server.
	 *
	 * @return The current working directory
	 */
	public String getWorkingDirectory() throws IOException {
		return client.printWorkingDirectory();
	}

	/**
	 * Change the working directory on the FTP server
	 *
	 * @param dirName The name of the folder/path you want to cd into
	 *
	 * @return FTPConnection for chaining
	 */
	public FTPConnection changeDir( String dirName ) throws IOException {
		client.changeWorkingDirectory( dirName );
		return this;
	}

	/**
	 * Close the connection to the FTP server
	 *
	 * @throws BoxIOException If an error occurs while closing the connection
	 */
	public void close() {
		try {
			// Check if the connection is open or not first
			if ( this.client.isConnected() ) {
				this.client.logout(); // Logout before disconnecting
				this.client.disconnect(); // Close the connection
				this.logger.info( "FTP connection [{}] closed", this.name );
			}
		} catch ( IOException e ) {
			this.logger.error( "Error while closing FTP connection: " + e.getMessage() );
			throw new BoxIOException( e );
		}
	}

	/**
	 * Do we have an open or closed connection to the FTP server
	 *
	 * @return True if the connection is open, false otherwise
	 */
	public boolean isConnected() {
		return this.client != null && this.client.isConnected();
	}

	/**
	 * Set the stopOnError flag
	 *
	 * @param stopOnError
	 */
	public FTPConnection setStopOnError( boolean stopOnError ) {
		this.stopOnError = stopOnError;
		return this;
	}

	/**
	 * Create a directory on the FTP server
	 *
	 * @param dirName The name of the directory you want to create
	 *
	 * @return If the directory was created or not
	 */
	public boolean createDir( String dirName ) {
		try {
			return client.makeDirectory( dirName );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Rename a file or diretory on the FTP server
	 *
	 * @param existing The name of the file/directory you want to rename
	 * @param newName  The new name of the file/directory
	 *
	 * @return If the rename was successful or not
	 */
	public Boolean rename( String existing, String newName ) {
		try {
			return client.rename( existing, newName );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Check if a file exists on the FTP server
	 *
	 * @param path The path to the file you want to check
	 *
	 * @return Boolean
	 */
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

	/**
	 * Check if a directory exists on the FTP server
	 *
	 * @param dirName The name of the directory you want to check
	 *
	 * @return True if the directory exists, false otherwise
	 */
	public Boolean existsDir( String dirName ) throws IOException {
		String pwd = null;
		try {
			pwd = client.printWorkingDirectory();
			// If it works, then it's valid, else throws an exception
			return client.changeWorkingDirectory( dirName );
		} catch ( IOException e ) {
			return false;
		} finally {
			if ( pwd != null ) {
				client.changeWorkingDirectory( pwd );
			}
		}
	}

	/**
	 * Remove a directory on the FTP server
	 *
	 * @param dirName The name of the directory you want to remove
	 *
	 * @return True if the directory was removed, false otherwise
	 */
	public boolean removeDir( String dirName ) {
		try {
			return client.removeDirectory( dirName );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Handle an error by throwing an exception if stopOnError is true and
	 * looking for a positive completion code.
	 */
	private FTPConnection handleError() {

		if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) && stopOnError ) {
			throw new BoxRuntimeException( "FTP error: " + client.getReplyString() );
		}

		return this;
	}

	/**
	 * List the contents of the current directory
	 *
	 * @param returnType The return type of the listing
	 *
	 * @throws BoxRuntimeException If an error occurs while listing the directory
	 *
	 * @return The contents of the directory as a Query or an Array of Structs
	 */
	public Object listdir( ReturnType returntype ) throws IOException {
		FTPFile[]	files		= this.client.listFiles();
		String		systemType	= this.client.getSystemType().toUpperCase();

		if ( !FTPReply.isPositiveCompletion( this.client.getReplyCode() ) ) {
			throw new BoxRuntimeException( "FTP error listing a directory: " + this.client.getReplyCode() );
		}

		if ( returntype == ReturnType.ARRAY ) {
			return filesToArray( files, systemType );
		}
		return filesToQuery( files, systemType );
	}

	/**
	 * Convert an array of FTPFile objects to an Array of Structs
	 *
	 * @param files      The array of FTPFile objects to convert
	 * @param systemType The system type of the FTP server
	 *
	 * @return A BoxLang Array of Structs containing the files
	 */
	public static Array filesToArray( FTPFile[] files, String systemType ) {
		return Arrays.asList( files )
		    .stream()
		    .map( file -> FTPFileToStruct( file, systemType ) )
		    .collect( BLCollector.toArray() );
	}

	/**
	 * Get the connection metadata:
	 * <ul>
	 * <li>name</li>
	 * <li>defaultPort</li>
	 * <li>defaultTimeout</li>
	 * <li>localAddress</li>
	 * <li>remoteAddress</li>
	 * <li>remotePort</li>
	 * <li>status</li>
	 * <li>systemName</li>
	 * <li>user</li>
	 * <li>passive</li>
	 * </ul>
	 *
	 * @throws BoxIOException If an error occurs while getting the metadata.
	 *
	 * @return The metadata of the connection as a struct. If the connection is not open, an empty struct is returned.
	 */
	public IStruct getMetadata() {
		try {
			return Struct.of(
			    "defaultPort", this.client.getDefaultPort(),
			    "defaultTimeout", this.client.getDefaultTimeout(),
			    "defaultDataTimeout", this.client.getDataTimeout(),
			    "localAddress", isConnected() ? this.client.getLocalAddress() : "",
			    "name", name,
			    "passive", this.client.getPassiveHost() != null ? true : false,
			    "remoteAddress", isConnected() ? this.client.getRemoteAddress() : "",
			    "remotePort", isConnected() ? this.client.getRemotePort() : "",
			    "status", isConnected() ? this.client.getStatus() : "closed",
			    "systemName", isConnected() ? this.client.getSystemType() : "",
			    "user", this.username,
			    "workingDirectory", isConnected() ? this.client.printWorkingDirectory() : ""
			);
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * A string representation of the connection.
	 */
	@Override
	public String toString() {
		return getMetadata().toString();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Convert an array of FTPFile objects to a BoxLang Query
	 *
	 * @param files      The array of FTPFile objects to convert
	 * @param systemType The system type of the FTP server
	 *
	 * @return A query object containing the files
	 */
	public static Query filesToQuery( FTPFile[] files, String systemType ) {
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
		    .forEach( file -> result.add( FTPFileToStruct( file, systemType ) ) );

		return result;
	}

	/**
	 * Convert an FTPFile object to a struct
	 *
	 * @param file       The FTPFile object to convert
	 * @param systemType The system type of the FTP server
	 *
	 * @return IStruct
	 *
	 * @throws IOException
	 */
	public static IStruct FTPFileToStruct( FTPFile file, String systemType ) {
		return Struct.of(
		    Key._name, file.getName(),
		    FTPKeys.isDirectory, file.isDirectory(),
		    FTPKeys.lastModified, DateTimeCaster.cast( file.getTimestamp() ),
		    // Dumb ACF compatibility
		    Key.length, file.getSize(),
		    // End Dumb name
		    Key.size, file.getSize(),
		    Key.mode, getMode( file, systemType ),
		    Key.path, file.getName(),
		    FTPKeys.url, file.getName(),
		    Key.type, getType( file ),
		    FTPKeys.raw, file.getRawListing(),
		    Key.attributes, file.getName(),
		    FTPKeys.isReadable, file.hasPermission( FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION ),
		    FTPKeys.isWritable, file.hasPermission( FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION ),
		    FTPKeys.isExecutable, file.hasPermission( FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION )
		);
	}

	/**
	 * Get the mode of the file as an octal string
	 * Example: 755
	 *
	 * @param file       The file to get the mode of
	 * @param systemType The system type of the FTP server
	 *
	 * @return The mode of the file as an octal string
	 */
	public static String getMode( FTPFile file, String systemType ) {
		if ( systemType.contains( "UNIX" ) ) {
			return convertToOctal( file.getRawListing().substring( 0, 10 ) );
		}
		return "000";
	}

	/**
	 * Convert a string of permissions to an octal string
	 *
	 * @param permissions The permissions string to convert
	 *
	 * @return The permissions as an octal string. If the permissions string is less than 10 characters, "000" is returned.
	 */
	public static String convertToOctal( String permissions ) {
		if ( permissions.length() < 10 ) {
			return "000";
		}

		StringBuilder octal = new StringBuilder();

		for ( int i = 1; i < permissions.length(); i += 3 ) {
			int value = 0;
			if ( permissions.charAt( i ) == 'r' )
				value += 4;
			if ( permissions.charAt( i + 1 ) == 'w' )
				value += 2;
			if ( permissions.charAt( i + 2 ) == 'x' )
				value += 1;
			octal.append( value );
		}

		return octal.toString();
	}

	/**
	 * Get the type of the file according to its type code
	 *
	 * @param file The file to get the type of
	 *
	 * @return The type of the file as a string
	 */
	public static String getType( FTPFile file ) {
		return switch ( file.getType() ) {
			case 0 -> "file";
			case 1 -> "directory";
			case 2 -> "symbolic link";
			default -> "unknown";
		};
	}
}
