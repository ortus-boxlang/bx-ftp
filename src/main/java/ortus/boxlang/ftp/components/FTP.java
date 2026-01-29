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
package ortus.boxlang.ftp.components;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import ortus.boxlang.ftp.BaseFTPConnection;
import ortus.boxlang.ftp.FTPConnection;
import ortus.boxlang.ftp.IFTPConnection;
import ortus.boxlang.ftp.FTPKeys;
import ortus.boxlang.ftp.FTPResult;
import ortus.boxlang.ftp.services.FTPService;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = false )
public class FTP extends Component {

	/**
	 * FTP service
	 */
	private FTPService				ftpService		= ( FTPService ) runtime.getGlobalService( FTPKeys.FTPService );

	/**
	 * The FTP logger
	 */
	BoxLangLogger					logger;

	/**
	 * The actions that can be performed by this component
	 */
	public static final String[]	VALID_ACTIONS	= new String[] {
	    "changedir",
	    "close",
	    "createDir",
	    "existsDir",
	    "existsFile",
	    "getCurrentDir",
	    "getfile",
	    "listdir",
	    "open",
	    "putfile",
	    "removeDir",
	    "remove",
	    "removeFile",
	    "renameFile",
	    "renameDir"
	};

	/**
	 * Constructor
	 */
	public FTP() {
		super();
		declaredAttributes	= new Attribute[] {
		    // The action to perform
		    new Attribute( Key.action, "string", Set.of( Validator.REQUIRED, Validator.valueOneOf( VALID_ACTIONS ) ) ),
		    // Name of the variable to store the result, if not passed, the result will be stored in a variable named 'bxftp'
		    new Attribute( Key.result, "string" ),
		    // Connection Attributes
		    new Attribute( FTPKeys.connection, "string", Set.of( Validator.REQUIRED ) ),
		    new Attribute( Key.server, "string" ),
		    new Attribute( Key.port, "numeric", FTPConnection.DEFAULT_PORT ),
		    new Attribute( Key.username, "string" ),
		    new Attribute( Key.password, "string" ),
		    new Attribute( FTPKeys.stopOnError, "boolean" ),
		    new Attribute( FTPKeys.passive, "boolean", FTPConnection.DEFAULT_PASSIVE ),
		    new Attribute( FTPKeys.timeout, "numeric", FTPConnection.DEFAULT_TIMEOUT.toSeconds() ),
		    // this is the proxy server to use, it can include the port number as well
		    new Attribute( Key.proxyServer, "string" ),
		    // Directory on which to performan an operation. Required for actions: changeDir, createDir, listDir, existsDir
		    new Attribute( Key.directory, "string" ),
		    // Query variable name when doing variable operations. Required for actions: listDir
		    new Attribute( Key._name, "string" ),
		    // The return type of the operation. Required for actions: listDir
		    new Attribute( Key.returnType, "string", "query", Set.of( Validator.valueOneOf( "query", "array" ) ) ),
		    // New name of the file/directory on the remote server. Required for actions: rename
		    new Attribute( FTPKeys._new, "string" ),
		    // The name of the file on the remote server. Required for actions: getFile, putFile, existsFile
		    new Attribute( FTPKeys.remoteFile, "string" ),
		    // Name of the file on the local file system. Required for actions: getFile, putFile
		    new Attribute( FTPKeys.localFile, "string" ),
		    // The existing file to rename. Required for actions: renameFile
		    new Attribute( FTPKeys.existing, "string" ),
		    // failIfExists (true) - If a local file with same name exists, should it be overwritten with action = getFile. Default is true
		    new Attribute( FTPKeys.failIfExists, "boolean", true ),
		    // SFTP-specific attributes
		    // secure (false) - FTP or SFTP if true
		    new Attribute( FTPKeys.secure, "boolean", false ),
		    // fingerprint - fingerprint of the server's public key for SFTP
		    new Attribute( FTPKeys.fingerprint, "string" ),
		    // key - absolute path to private key file for SFTP
		    new Attribute( FTPKeys.key, "string" ),
		    // passphrase - passphrase for private key for SFTP
		    new Attribute( FTPKeys.passphrase, "string" )

			// Pending Attributes, not sure if we need to do them.
			// ASCIIExtensionList - Delimited list of file extensions that force ASCII transfer mode, if transferMode = "auto".
			// systemType - windows or unix
			// transferMode - auto, ascii, binary
			// retrycount (1) - Number of times to retry an operation
			// bufferSize - Buffer size in bytes
		};
		this.logger			= ftpService.getLogger();
	}

	/**
	 * An FTP component that allows you to interact with an FTP server.
	 * <p>
	 * The available actions are:
	 * <ul>
	 * <li>changeDir</li>
	 * <li>createDir</li>
	 * <li>close</li>
	 * <li>open</li>
	 * <li>existsDir</li>
	 * <li>existsFile</li>
	 * <li>getCurrentDir</li>
	 * <li>getfile</li>
	 * <li>listDir</li>
	 * <li>putFile</li>
	 * <li>removeDir</li>
	 * <li>removeFile</li>
	 * <li>renameFile</li>
	 * <li>renameDir</li>
	 * </ul>
	 *
	 * <h2>Examples:</h2>
	 *
	 * <pre>
	 * {@code
	 * <bx:ftp action="open" server="ftp.server.com" port="21" username="user" password="password" connection="myConnection" >
	 * <bx:ftp action="changedir" directory="newDir" connection="myConnection">
	 * <bx:ftp action="close" connection="myConnection>
	 * }
	 * </pre>
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.action The name of the person greeting us.
	 *
	 * @attribute.location The location of the person.
	 *
	 * @attribute.shout Whether the person is shouting or not.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IFTPConnection	ftpConnection	= findOrInitializeConnection( context, attributes );
		FTPResult		ftpResult		= new FTPResult( ftpConnection );
		String			action			= attributes.getAsString( Key.action ).toLowerCase();
		Object			returnValue		= null;

		// Some flags to set in the connection for operation if they are present
		if ( attributes.containsKey( FTPKeys.stopOnError ) ) {
			ftpConnection.setStopOnError( attributes.getAsBoolean( FTPKeys.stopOnError ) );
		}

		// Announce the FTP Action event
		runtime.announce(
		    FTPKeys.beforeFTPCall,
		    () -> Struct.ofNonConcurrent(
		        "connection", ftpConnection,
		        "action", action,
		        "result", ftpResult,
		        "attributes", attributes
		    )
		);

		try {
			switch ( action.toLowerCase() ) {
				// Connection Actions
				case "open" :
					runtime.announce(
					    FTPKeys.onFTPConnectionOpen,
					    Struct.of(
					        FTPKeys.connection, ftpConnection,
					        "attributes", attributes
					    )
					);

					// Determine the port to use based on whether this is secure (SFTP) or not
					Integer connectionPort = attributes.get( Key.port ) != null
					    ? IntegerCaster.cast( attributes.get( Key.port ) )
					    : ( attributes.getAsBoolean( FTPKeys.secure )
					        ? BaseFTPConnection.DEFAULT_SFTP_PORT
					        : BaseFTPConnection.DEFAULT_PORT );

					// Check if this is an SFTP connection with key-based authentication
					if ( attributes.getAsBoolean( FTPKeys.secure ) && attributes.containsKey( FTPKeys.key ) ) {
						// SFTP with key authentication
						if ( ftpConnection instanceof ortus.boxlang.ftp.SFTPConnection sftpConn ) {
							sftpConn.openWithKey(
							    attributes.getAsString( Key.server ),
							    connectionPort,
							    attributes.getAsString( Key.username ),
							    attributes.getAsString( FTPKeys.key ),
							    attributes.getAsString( FTPKeys.passphrase ),
							    Duration.ofSeconds( IntegerCaster.cast( attributes.get( FTPKeys.timeout ) ) ),
							    attributes.getAsString( FTPKeys.fingerprint )
							);
						}
					} else {
						// Standard FTP or SFTP with password
						ftpConnection.open(
						    attributes.getAsString( Key.server ),
						    connectionPort,
						    attributes.getAsString( Key.username ),
						    attributes.getAsString( Key.password ),
						    BooleanCaster.cast( attributes.get( FTPKeys.passive ) ),
						    Duration.ofSeconds( IntegerCaster.cast( attributes.get( FTPKeys.timeout ) ) ),
						    attributes.getAsString( Key.proxyServer )
						);
					}
					break;
				case "close" :
					runtime.announce(
					    FTPKeys.onFTPConnectionClose,
					    Struct.of( FTPKeys.connection, ftpConnection )
					);
					ftpConnection.close();
					break;

				// Directory Actions
				case "changedir" :
					ftpConnection.changeDir( attributes.getAsString( Key.directory ) );
					break;
				case "createdir" :
					returnValue = ftpConnection.createDir( attributes.getAsString( FTPKeys._new ) );
					break;
				case "removedir" :
					String targetDirectory = attributes.getAsString( FTPKeys.directory );
					// Legacy compatibility
					if ( attributes.containsKey( Key.item ) && !attributes.getAsString( Key.item ).isBlank() ) {
						targetDirectory = attributes.getAsString( Key.item );
					}
					returnValue = ftpConnection.removeDir( targetDirectory );
					break;
				case "listdir" :
					Object files = ftpConnection
					    .changeDir( attributes.getAsString( Key.directory ) )
					    .listdir(
					        attributes.getAsString( Key.returnType ).equalsIgnoreCase( "query" )
					            ? IFTPConnection.ReturnType.QUERY
					            : IFTPConnection.ReturnType.ARRAY
					    );
					returnValue = files;
					context.getDefaultAssignmentScope().put( Key.of( attributes.get( Key._name ) ), files );
					break;
				case "getcurrentdir" :
					returnValue = ftpConnection.getWorkingDirectory();
					break;
				case "existsdir" :
					returnValue = ftpConnection.existsDir( attributes.getAsString( FTPKeys.directory ) );
					break;

				// File Actions
				case "getfile" :
					returnValue = ftpConnection.getFile(
					    attributes.getAsString( FTPKeys.remoteFile ),
					    attributes.getAsString( FTPKeys.localFile ),
					    BooleanCaster.cast( attributes.get( FTPKeys.failIfExists ) )
					);
					break;
				case "renameFile", "renameDir" :
					returnValue = ftpConnection.rename(
					    attributes.getAsString( FTPKeys.existing ),
					    attributes.getAsString( FTPKeys._new )
					);
					break;
				case "remove", "removeFile" :
					String targetFile = attributes.getAsString( FTPKeys.remoteFile );
					// Legacy compatibility
					if ( attributes.containsKey( Key.item ) && !attributes.getAsString( Key.item ).isBlank() ) {
						targetFile = attributes.getAsString( Key.item );
					}
					returnValue = ftpConnection.remove( targetFile );
					break;
				case "existsfile" :
					returnValue = ftpConnection.existsFile( attributes.getAsString( FTPKeys.remoteFile ) );
					break;
				case "putfile" :
					returnValue = ftpConnection.putFile(
					    attributes.getAsString( FTPKeys.localFile ),
					    attributes.getAsString( FTPKeys.remoteFile )
					);
			}
			;

			// Set our connection variable in the context
			context.getDefaultAssignmentScope().put(
			    attributes.getAsString( FTPKeys.connection ),
			    ftpConnection
			);

			// Check if there is a return value to set in our ftp result
			if ( returnValue != null ) {
				ftpResult.setReturnValue( returnValue );
			}

			// Either assign a 'result' variable or return the result as a 'bxftp' variable
			if ( attributes.get( Key.result ) instanceof String targetResult && !targetResult.isBlank() ) {
				context.getDefaultAssignmentScope().put( Key.of( targetResult ), ftpResult.toStruct() );
			} else {
				context.getDefaultAssignmentScope().put( FTPKeys.bxftp, ftpResult.toStruct() );
			}

			// Announce the FTP Action event
			runtime.announce(
			    FTPKeys.afterFTPCall,
			    () -> Struct.ofNonConcurrent(
			        "connection", ftpConnection,
			        "action", action,
			        "result", ftpResult,
			        "attributes", attributes
			    )
			);

		} catch ( IOException e ) {
			String message = String.format( "Error executing action [%s] -> [%s]", action, e.getMessage() );
			this.logger.error( message, e );

			// Announce the FTP Error event
			runtime.announce(
			    FTPKeys.onFTPError,
			    Struct.ofNonConcurrent(
			        "connection", ftpConnection,
			        "action", action,
			        "error", e,
			        "attributes", attributes
			    )
			);

			throw new BoxIOException( message, e );
		}

		return DEFAULT_RETURN;
	}

	/**
	 * Find or initialize a connection to the FTP/SFTP server
	 *
	 * @param context    The context in which the Component is being invoked
	 * @param attributes The attributes to the Component
	 *
	 * @return The FTP/SFTP connection
	 */
	private IFTPConnection findOrInitializeConnection( IBoxContext context, IStruct attributes ) {
		String	connectionName	= attributes.getAsString( FTPKeys.connection ).trim();
		boolean	secure			= attributes.getAsBoolean( FTPKeys.secure );
		return this.ftpService.getOrBuildConnection( Key.of( connectionName ), secure );
	}
}
