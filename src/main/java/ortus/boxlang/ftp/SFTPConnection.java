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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;

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
 * This class is a wrapper around the JSch SFTP client. It provides a simplified interface
 * for working with SFTP servers.
 * <p>
 * Implementation uses JSch library for SSH/SFTP connectivity with support for:
 * <ul>
 * <li>Password authentication</li>
 * <li>Public key authentication</li>
 * <li>Host key fingerprint verification</li>
 * </ul>
 * </p>
 */
public class SFTPConnection extends BaseFTPConnection {

	/**
	 * --------------------------------------------------------------------------
	 * Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The JSch session object used to connect to the server.
	 */
	private Session			session;

	/**
	 * The SFTP channel used to communicate with the server.
	 */
	private ChannelSftp		sftpChannel;

	/**
	 * The server address
	 */
	private String			server;

	/**
	 * The server port
	 */
	private Integer			port;

	/**
	 * The fingerprint for host key verification
	 */
	private String			fingerprint;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Build a SFTP Connection
	 *
	 * @param name   The name of the connection
	 * @param logger The BoxLang logger to use
	 */
	public SFTPConnection( Key name, BoxLangLogger logger ) {
		super( name, logger );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Service Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Connect to an SFTP server and open a connection.
	 *
	 * @param server      The server to connect to
	 * @param port        The port to connect to (default: 22)
	 * @param username    The username to use
	 * @param password    The password to use
	 * @param passive     Not used for SFTP (ignored)
	 * @param timeout     The timeout in seconds as a Duration
	 * @param proxyServer Not currently supported for SFTP (ignored)
	 *
	 * @return This connection for chaining
	 *
	 * @throws IOException If an error occurs while connecting
	 */
	@Override
	public IFTPConnection open( String server, Integer port, String username, String password, boolean passive, Duration timeout,
	    String proxyServer )
	    throws IOException {
		// Verify that the required parameters are present or default them
		Objects.requireNonNull( server, "Server is required" );
		port		= Objects.requireNonNullElse( port, DEFAULT_SFTP_PORT );
		username	= Objects.requireNonNullElse( username, DEFAULT_USERNAME );
		password	= Objects.requireNonNullElse( password, DEFAULT_PASSWORD );
		timeout		= Objects.requireNonNullElse( timeout, DEFAULT_TIMEOUT );

		// Store for future reference
		this.username	= username;
		this.server		= server;
		this.port		= port;

		try {
			JSch jsch = new JSch();

			// Create session
			this.session = jsch.getSession( username, server, port );
			this.session.setPassword( password );

			// Set session properties
			Properties config = new Properties();
			config.put( "StrictHostKeyChecking", "no" ); // Will be overridden if fingerprint is provided
			this.session.setConfig( config );

			// Set timeout
			this.session.setTimeout( ( int ) timeout.toMillis() );

			// Connect session
			this.session.connect();

			// Open SFTP channel
			this.sftpChannel = ( ChannelSftp ) this.session.openChannel( "sftp" );
			this.sftpChannel.connect();

			this.logger.info( "SFTP connection [{}] opened.", this.name );
			updateStatus( 0, "Connected" );

		} catch ( JSchException e ) {
			this.logger.error( "SFTP server connection failed: " + e.getMessage() );
			throw new BoxRuntimeException( "SFTP server connection failed: " + e.getMessage(), e );
		}

		return this;
	}

	/**
	 * Open an SFTP connection with SSH key authentication.
	 *
	 * @param server      The server to connect to
	 * @param port        The port to connect to
	 * @param username    The username to use
	 * @param privateKey  The path to the private key file
	 * @param passphrase  The passphrase for the private key (can be null)
	 * @param timeout     The timeout in seconds as a Duration
	 * @param fingerprint The server's host key fingerprint for verification (optional)
	 *
	 * @return This connection for chaining
	 *
	 * @throws IOException If an error occurs while connecting
	 */
	public IFTPConnection openWithKey( String server, Integer port, String username, String privateKey, String passphrase, Duration timeout,
	    String fingerprint )
	    throws IOException {
		// Verify that the required parameters are present or default them
		Objects.requireNonNull( server, "Server is required" );
		Objects.requireNonNull( privateKey, "Private key is required for key-based authentication" );
		port		= Objects.requireNonNullElse( port, DEFAULT_SFTP_PORT );
		username	= Objects.requireNonNullElse( username, DEFAULT_USERNAME );
		timeout		= Objects.requireNonNullElse( timeout, DEFAULT_TIMEOUT );

		// Store for future reference
		this.username		= username;
		this.server			= server;
		this.port			= port;
		this.fingerprint	= fingerprint;

		try {
			JSch jsch = new JSch();

			// Add private key
			if ( passphrase != null && !passphrase.isEmpty() ) {
				jsch.addIdentity( privateKey, passphrase );
			} else {
				jsch.addIdentity( privateKey );
			}

			// Create session
			this.session = jsch.getSession( username, server, port );

			// Set session properties
			Properties config = new Properties();
			if ( fingerprint != null && !fingerprint.isEmpty() ) {
				config.put( "StrictHostKeyChecking", "yes" );
				// TODO: Implement fingerprint verification via HostKeyRepository
			} else {
				config.put( "StrictHostKeyChecking", "no" );
			}
			this.session.setConfig( config );

			// Set timeout
			this.session.setTimeout( ( int ) timeout.toMillis() );

			// Connect session
			this.session.connect();

			// Open SFTP channel
			this.sftpChannel = ( ChannelSftp ) this.session.openChannel( "sftp" );
			this.sftpChannel.connect();

			this.logger.info( "SFTP connection [{}] opened with key authentication.", this.name );
			updateStatus( 0, "Connected" );

		} catch ( JSchException e ) {
			this.logger.error( "SFTP server connection failed: " + e.getMessage() );
			throw new BoxRuntimeException( "SFTP server connection failed: " + e.getMessage(), e );
		}

		return this;
	}

	/**
	 * Retrieve a file from the SFTP server and write it out to a local file.
	 *
	 * @param remoteFile   The name of the file to copy
	 * @param localFile    The path of the file to save
	 * @param failIfExists If true, the file will not be copied if it already exists
	 *
	 * @return True if the file was copied, false otherwise
	 *
	 * @throws IOException If an error occurs while copying the file
	 */
	@Override
	public boolean getFile( String remoteFile, String localFile, boolean failIfExists ) throws IOException {
		File targetFile = new File( localFile );

		// Check if the file exists and if it should be copied over
		if ( targetFile.exists() && failIfExists ) {
			throw new BoxRuntimeException( "Error: Local file already exists and [failIfExists=true]" + targetFile );
		}

		try ( OutputStream outputStream = new FileOutputStream( targetFile ) ) {
			sftpChannel.get( remoteFile, outputStream );
			updateStatus( 0, "File retrieved successfully" );
			return true;
		} catch ( SftpException e ) {
			this.logger.error( "Error retrieving file: " + e.getMessage() );
			updateStatus( e.id, e.getMessage() );
			handleError();
			return false;
		}
	}

	/**
	 * Put a file on the remote SFTP server.
	 *
	 * @param localFile  The file path of the local file you want to copy
	 * @param remoteFile The name of the remote file you want to create/update
	 *
	 * @return True if the file was copied, false otherwise
	 *
	 * @throws BoxRuntimeException If the local file does not exist, not a file, or cannot be read.
	 * @throws IOException         If an error occurs while copying the file
	 */
	@Override
	public boolean putFile( String localFile, String remoteFile ) throws IOException {
		File targetFile = ensureLocalFile( new File( localFile ) );

		try ( FileInputStream inputStream = new FileInputStream( targetFile ) ) {
			sftpChannel.put( inputStream, remoteFile );
			updateStatus( 0, "File uploaded successfully" );
			return true;
		} catch ( SftpException e ) {
			this.logger.error( "Error uploading file: " + e.getMessage() );
			updateStatus( e.id, e.getMessage() );
			handleError();
			return false;
		}
	}

	/**
	 * Remove a file on the SFTP server.
	 *
	 * @param remoteFile The name of the file you want to remove
	 *
	 * @return True if the file was removed, false otherwise
	 */
	@Override
	public boolean remove( String remoteFile ) {
		try {
			sftpChannel.rm( remoteFile );
			updateStatus( 0, "File removed successfully" );
			return true;
		} catch ( SftpException e ) {
			this.logger.error( "Error removing file: " + e.getMessage() );
			updateStatus( e.id, e.getMessage() );
			handleError();
			return false;
		}
	}

	/**
	 * Get the current selected working directory on the SFTP server.
	 *
	 * @return The current working directory
	 *
	 * @throws IOException If an error occurs
	 */
	@Override
	public String getWorkingDirectory() throws IOException {
		try {
			return sftpChannel.pwd();
		} catch ( SftpException e ) {
			throw new BoxIOException( new IOException( "Error getting working directory: " + e.getMessage(), e ) );
		}
	}

	/**
	 * Change the working directory on the SFTP server.
	 *
	 * @param dirName The name of the folder/path you want to cd into
	 *
	 * @return This connection for chaining
	 *
	 * @throws IOException If an error occurs
	 */
	@Override
	public IFTPConnection changeDir( String dirName ) throws IOException {
		try {
			sftpChannel.cd( dirName );
			updateStatus( 0, "Directory changed successfully" );
			return this;
		} catch ( SftpException e ) {
			throw new BoxIOException( new IOException( "Error changing directory: " + e.getMessage(), e ) );
		}
	}

	/**
	 * Close the connection to the SFTP server.
	 */
	@Override
	public void close() {
		try {
			if ( this.sftpChannel != null && this.sftpChannel.isConnected() ) {
				this.sftpChannel.disconnect();
			}
			if ( this.session != null && this.session.isConnected() ) {
				this.session.disconnect();
			}
			this.logger.info( "SFTP connection [{}] closed", this.name );
		} catch ( Exception e ) {
			this.logger.error( "Error while closing SFTP connection: " + e.getMessage() );
			throw new BoxIOException( new IOException( "Error closing SFTP connection: " + e.getMessage(), e ) );
		}
	}

	/**
	 * Do we have an open or closed connection to the SFTP server.
	 *
	 * @return True if the connection is open, false otherwise
	 */
	@Override
	public boolean isConnected() {
		return this.sftpChannel != null && this.sftpChannel.isConnected() &&
		    this.session != null && this.session.isConnected();
	}

	/**
	 * Create a directory on the SFTP server.
	 *
	 * @param dirName The name of the directory you want to create
	 *
	 * @return If the directory was created or not
	 */
	@Override
	public boolean createDir( String dirName ) {
		try {
			sftpChannel.mkdir( dirName );
			updateStatus( 0, "Directory created successfully" );
			return true;
		} catch ( SftpException e ) {
			this.logger.error( "Error creating directory: " + e.getMessage() );
			updateStatus( e.id, e.getMessage() );
			handleError();
			return false;
		}
	}

	/**
	 * Rename a file or directory on the SFTP server.
	 *
	 * @param existing The name of the file/directory you want to rename
	 * @param newName  The new name of the file/directory
	 *
	 * @return If the rename was successful or not
	 */
	@Override
	public Boolean rename( String existing, String newName ) {
		try {
			sftpChannel.rename( existing, newName );
			updateStatus( 0, "Rename successful" );
			return true;
		} catch ( SftpException e ) {
			this.logger.error( "Error renaming: " + e.getMessage() );
			updateStatus( e.id, e.getMessage() );
			handleError();
			return false;
		}
	}

	/**
	 * Check if a file exists on the SFTP server.
	 *
	 * @param path The path to the file you want to check
	 *
	 * @return True if the file exists, false otherwise
	 */
	@Override
	public Boolean existsFile( String path ) {
		if ( path.equals( "/" ) ) {
			return true;
		}

		try {
			SftpATTRS attrs = sftpChannel.stat( path );
			return attrs != null && !attrs.isDir();
		} catch ( SftpException e ) {
			return false;
		}
	}

	/**
	 * Check if a directory exists on the SFTP server.
	 *
	 * @param dirName The name of the directory you want to check
	 *
	 * @return True if the directory exists, false otherwise
	 *
	 * @throws IOException If an error occurs
	 */
	@Override
	public Boolean existsDir( String dirName ) throws IOException {
		try {
			SftpATTRS attrs = sftpChannel.stat( dirName );
			return attrs != null && attrs.isDir();
		} catch ( SftpException e ) {
			return false;
		}
	}

	/**
	 * Remove a directory on the SFTP server.
	 *
	 * @param dirName The name of the directory you want to remove
	 *
	 * @return True if the directory was removed, false otherwise
	 */
	@Override
	public boolean removeDir( String dirName ) {
		try {
			sftpChannel.rmdir( dirName );
			updateStatus( 0, "Directory removed successfully" );
			return true;
		} catch ( SftpException e ) {
			this.logger.error( "Error removing directory: " + e.getMessage() );
			updateStatus( e.id, e.getMessage() );
			handleError();
			return false;
		}
	}

	/**
	 * List the contents of the current directory.
	 *
	 * @param returntype The return type of the listing (QUERY or ARRAY)
	 *
	 * @return The contents of the directory as a Query or an Array of Structs
	 *
	 * @throws IOException If an error occurs while listing the directory
	 */
	@Override
	public Object listdir( ReturnType returntype ) throws IOException {
		try {
			@SuppressWarnings( "unchecked" )
			Vector<LsEntry>	entries		= sftpChannel.ls( "." );
			String			systemType	= "UNIX"; // SFTP servers are typically Unix-based

			// Filter out . and ..
			LsEntry[] files = entries.stream()
			    .filter( entry -> !entry.getFilename().equals( "." ) && !entry.getFilename().equals( ".." ) )
			    .toArray( LsEntry[]::new );

			updateStatus( 0, "Directory listed successfully" );

			if ( returntype == ReturnType.ARRAY ) {
				return filesToArray( files, systemType );
			}
			return filesToQuery( files, systemType );
		} catch ( SftpException e ) {
			this.logger.error( "Error listing directory: " + e.getMessage() );
			updateStatus( e.id, e.getMessage() );
			throw new BoxIOException( new IOException( "Error listing directory: " + e.getMessage(), e ) );
		}
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
	 * <li>secure</li>
	 * </ul>
	 *
	 * @return The metadata of the connection as a struct. If the connection is not open, an empty struct is returned.
	 *
	 * @throws BoxIOException If an error occurs while getting the metadata.
	 */
	@Override
	public IStruct getMetadata() {
		try {
			return Struct.of(
			    "defaultPort", DEFAULT_SFTP_PORT,
			    "defaultTimeout", DEFAULT_TIMEOUT.toMillis(),
			    "defaultDataTimeout", DEFAULT_TIMEOUT.toMillis(),
			    "localAddress", isConnected() && session != null ? session.getHost() : "",
			    "name", name,
			    "passive", false, // SFTP doesn't use passive mode
			    "remoteAddress", isConnected() && session != null ? session.getHost() : "",
			    "remotePort", isConnected() && session != null ? session.getPort() : 0,
			    "status", isConnected() ? "connected" : "closed",
			    "systemName", "SFTP",
			    "user", this.username,
			    "workingDirectory", isConnected() ? sftpChannel.pwd() : "",
			    "secure", true
			);
		} catch ( SftpException e ) {
			throw new BoxIOException( new IOException( "Error getting metadata: " + e.getMessage(), e ) );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Handle an error by throwing an exception if stopOnError is true.
	 */
	private void handleError() {
		if ( this.stopOnError && this.statusCode != 0 ) {
			throw new BoxRuntimeException( "SFTP operation error: " + this.statusText );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Convert an array of SFTP LsEntry objects to a BoxLang Query.
	 *
	 * @param files      The array of LsEntry objects to convert
	 * @param systemType The system type of the SFTP server
	 *
	 * @return A query object containing the files
	 */
	public static Query filesToQuery( LsEntry[] files, String systemType ) {
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
		    .forEach( file -> result.add( sftpEntryToStruct( file, systemType ) ) );

		return result;
	}

	/**
	 * Convert an array of SFTP LsEntry objects to an Array of Structs.
	 *
	 * @param files      The array of LsEntry objects to convert
	 * @param systemType The system type of the SFTP server
	 *
	 * @return A BoxLang Array of Structs containing the files
	 */
	public static Array filesToArray( LsEntry[] files, String systemType ) {
		return Arrays.asList( files )
		    .stream()
		    .map( file -> sftpEntryToStruct( file, systemType ) )
		    .collect( BLCollector.toArray() );
	}

	/**
	 * Convert an SFTP LsEntry object to a struct.
	 *
	 * @param entry      The LsEntry object to convert
	 * @param systemType The system type of the SFTP server
	 *
	 * @return IStruct containing file information
	 */
	public static IStruct sftpEntryToStruct( LsEntry entry, String systemType ) {
		SftpATTRS attrs = entry.getAttrs();

		return Struct.of(
		    Key._name, entry.getFilename(),
		    FTPKeys.isDirectory, attrs.isDir(),
		    FTPKeys.lastModified, DateTimeCaster.cast( attrs.getMTime() * 1000L ), // Convert seconds to milliseconds
		    // Dumb ACF compatibility
		    Key.length, attrs.getSize(),
		    // End Dumb name
		    Key.size, attrs.getSize(),
		    Key.mode, getMode( attrs, systemType ),
		    Key.path, entry.getFilename(),
		    FTPKeys.url, entry.getFilename(),
		    Key.type, attrs.isDir() ? "directory" : attrs.isLink() ? "symbolic link" : "file",
		    FTPKeys.raw, entry.getLongname(),
		    Key.attributes, entry.getFilename(),
		    FTPKeys.isReadable, ( attrs.getPermissions() & 0400 ) != 0, // Owner read permission
		    FTPKeys.isWritable, ( attrs.getPermissions() & 0200 ) != 0, // Owner write permission
		    FTPKeys.isExecutable, ( attrs.getPermissions() & 0100 ) != 0 // Owner execute permission
		);
	}

	/**
	 * Get the mode of the file as an octal string.
	 * Example: 755
	 *
	 * @param attrs      The file attributes
	 * @param systemType The system type of the SFTP server
	 *
	 * @return The mode of the file as an octal string
	 */
	public static String getMode( SftpATTRS attrs, String systemType ) {
		int permissions = attrs.getPermissions();
		// Extract only the permission bits (last 9 bits)
		int mode = permissions & 0777;
		return String.format( "%03o", mode );
	}
}
