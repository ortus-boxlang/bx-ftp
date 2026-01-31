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

import java.io.IOException;
import java.time.Duration;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Interface for FTP and SFTP connections.
 * <p>
 * This interface defines the contract for both FTP and SFTP connection implementations,
 * allowing them to be used interchangeably by the FTPService and FTP component.
 * </p>
 */
public interface IFTPConnection {

	/**
	 * Enum for return types: query and array
	 */
	public enum ReturnType {
		QUERY,
		ARRAY
	}

	/**
	 * Connect to an FTP/SFTP server and open a connection.
	 *
	 * @param server      The server to connect to
	 * @param port        The port to connect to
	 * @param username    The username to use
	 * @param password    The password to use
	 * @param passive     Whether to use passive mode (FTP only)
	 * @param timeout     The timeout in seconds as a Duration
	 * @param proxyServer The proxy server to use, if not null or empty
	 *
	 * @return This connection for chaining
	 *
	 * @throws IOException If an error occurs while connecting
	 */
	IFTPConnection open( String server, Integer port, String username, String password, boolean passive, Duration timeout, String proxyServer )
	    throws IOException;

	/**
	 * Retrieve a file from the server and write it out to a local file.
	 *
	 * @param remoteFile   The name of the file to copy
	 * @param localFile    The path of the file to save
	 * @param failIfExists If true, the file will not be copied if it already exists
	 *
	 * @return True if the file was copied, false otherwise
	 *
	 * @throws IOException If an error occurs while copying the file
	 */
	boolean getFile( String remoteFile, String localFile, boolean failIfExists ) throws IOException;

	/**
	 * Put a file on the remote server.
	 *
	 * @param localFile  The file path of the local file you want to copy
	 * @param remoteFile The name of the remote file you want to create/update
	 *
	 * @return True if the file was copied, false otherwise
	 *
	 * @throws IOException If an error occurs while copying the file
	 */
	boolean putFile( String localFile, String remoteFile ) throws IOException;

	/**
	 * Remove a file on the server.
	 *
	 * @param remoteFile The name of the file you want to remove
	 *
	 * @return True if the file was removed, false otherwise
	 */
	boolean remove( String remoteFile );

	/**
	 * Return the most recent status code from the server.
	 *
	 * @return The status code
	 */
	int getStatusCode();

	/**
	 * Get the status text associated with the most recent reply from the server.
	 *
	 * @return The status text
	 */
	String getStatusText();

	/**
	 * Get the current selected working directory on the server.
	 *
	 * @return The current working directory
	 *
	 * @throws IOException If an error occurs
	 */
	String getWorkingDirectory() throws IOException;

	/**
	 * Change the working directory on the server.
	 *
	 * @param dirName The name of the folder/path you want to cd into
	 *
	 * @return This connection for chaining
	 *
	 * @throws IOException If an error occurs
	 */
	IFTPConnection changeDir( String dirName ) throws IOException;

	/**
	 * Close the connection to the server.
	 */
	void close();

	/**
	 * Do we have an open or closed connection to the server.
	 *
	 * @return True if the connection is open, false otherwise
	 */
	boolean isConnected();

	/**
	 * Set the stopOnError flag.
	 *
	 * @param stopOnError Whether to stop on error
	 *
	 * @return This connection for chaining
	 */
	IFTPConnection setStopOnError( boolean stopOnError );

	/**
	 * Create a directory on the server.
	 *
	 * @param dirName The name of the directory you want to create
	 *
	 * @return If the directory was created or not
	 */
	boolean createDir( String dirName );

	/**
	 * Rename a file or directory on the server.
	 *
	 * @param existing The name of the file/directory you want to rename
	 * @param newName  The new name of the file/directory
	 *
	 * @return If the rename was successful or not
	 */
	Boolean rename( String existing, String newName );

	/**
	 * Check if a file exists on the server.
	 *
	 * @param path The path to the file you want to check
	 *
	 * @return True if the file exists, false otherwise
	 */
	Boolean existsFile( String path );

	/**
	 * Check if a directory exists on the server.
	 *
	 * @param dirName The name of the directory you want to check
	 *
	 * @return True if the directory exists, false otherwise
	 *
	 * @throws IOException If an error occurs
	 */
	Boolean existsDir( String dirName ) throws IOException;

	/**
	 * Remove a directory on the server.
	 *
	 * @param dirName The name of the directory you want to remove
	 *
	 * @return True if the directory was removed, false otherwise
	 */
	boolean removeDir( String dirName );

	/**
	 * List the contents of the current directory.
	 *
	 * @param returntype The return type of the listing (QUERY or ARRAY)
	 *
	 * @return The contents of the directory as a Query or an Array of Structs
	 *
	 * @throws IOException If an error occurs while listing the directory
	 */
	Object listdir( ReturnType returntype ) throws IOException;

	/**
	 * Get the connection metadata.
	 *
	 * @return The metadata of the connection as a struct
	 */
	IStruct getMetadata();

	/**
	 * Get the connection name.
	 *
	 * @return The name of the connection
	 */
	Key getName();
}
