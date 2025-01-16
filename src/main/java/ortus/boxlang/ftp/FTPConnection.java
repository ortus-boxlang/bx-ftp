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

import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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

	/**
	 * --------------------------------------------------------------------------
	 * Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The FTPClient object used to communicate with the server.
	 */
	private FTPClient				client					= new FTPClient();

	/**
	 * If true, an exception will be thrown if an error occurs. If false, the
	 * error will be ignored.
	 */
	private boolean					stopOnError				= DEFAULT_STOP_ON_ERROR;

	/**
	 * The name of the connection.
	 */
	private Key						name;

	/**
	 * The username for the connection.
	 */
	private String					username;

	/**
	 * The BoxLang logger to use
	 */
	private BoxLangLogger			logger;

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
	 */
	public void remove( String remoteFile ) {
		try {
			client.deleteFile( remoteFile );
		} catch ( IOException e ) {
			this.handleError();
		}
		this.handleError();
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
	 * @return String
	 */
	public String getWorkingDirectory() throws IOException {
		return client.printWorkingDirectory();
	}

	/**
	 * Change the working directory on the FTP server
	 *
	 * @param dirName The name of the folder/path you want to cd into
	 *
	 * @throws IOException
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
	 */
	public FTPConnection createDir( String dirName ) {
		try {
			client.makeDirectory( dirName );
			this.handleError();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		return this;
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
	 */
	public void removeDir( String dirName ) {
		try {
			client.removeDirectory( dirName );

			this.handleError();
		} catch ( IOException e ) {
			this.handleError();
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
	 * @return Query The contents of the directory
	 *
	 * @throws IOException
	 */
	public Query listdir() throws IOException {
		FTPFile[] files = client.listFiles();

		if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
			throw new BoxRuntimeException( "FTP error listing a directory: " + client.getReplyCode() );
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

	/**
	 * Convert an FTPFile object to a struct
	 *
	 * @param file The FTPFile object to convert
	 *
	 * @return IStruct
	 */
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

	/**
	 * Get the mode of the file
	 *
	 * @param file
	 *
	 * @return
	 */
	public static String getMode( FTPFile file ) {
		// TODO complete mode representation
		return "000";
	}

	/**
	 * Get the raw representation of the file meta data
	 *
	 * @param file
	 *
	 * @return
	 */
	public static String getRaw( FTPFile file ) {
		// TODO complete mode representation: 'drwxrwxrwx 1 0 0 4096 Oct 29 02:54 a sub folder'
		return "drwxrwxrwx 1 0 0 4096 Oct 29 02:54 a sub folder";
	}

	/**
	 * Get the type of the file
	 *
	 * @param file
	 *
	 * @return
	 */
	public static String getType( FTPFile file ) {
		return switch ( file.getType() ) {
			case 0 -> "file";
			case 1 -> "directory";
			case 2 -> "symbolic link";
			default -> "unknown";
		};
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
}
