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

import ortus.boxlang.ftp.FTPConnection;
import ortus.boxlang.ftp.FTPKeys;
import ortus.boxlang.ftp.FTPResult;
import ortus.boxlang.ftp.services.FTPService;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
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
	    "remove"
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
		    new Attribute( FTPKeys.timeout, "numeric" ),

		    // Directory on which to performan an operation. Required for actions: changeDir, createDir, listDir, existsDir
		    new Attribute( Key.directory, "string" ),

		    // The target file/directory. Required for actions: exists, remove
		    new Attribute( Key.item, "string" ),

		    // Query variable name when doing variable operations. Required for actions: listDir
		    new Attribute( Key._name, "string" ),

		    // New name of the file/directory on the remote server. Required for actions: rename
		    new Attribute( FTPKeys._new, "string" ),
		    // The name of the file on the remote server. Required for actions: getFile, putFile, existsFile
		    new Attribute( FTPKeys.remoteFile, "string" ),
		    // Name of the file on the local file system. Required for actions: getFile, putFile
		    new Attribute( FTPKeys.localFile, "string" ),

			// Pending Attributes, not sure if we need to do them.
			// ASCIIExtensionList - Delimited list of file extensions that force ASCII transfer mode, if transferMode = "auto".
			// existing - Current name of the file/directory on the remote server. Required for action = rename
			// failIfExists (true) - If a local file with same name exists, should it be overwritten with action = getFile. Default is true
			// proxyServer - name of the proxy server to use
			// systemType - windows or unix
			// transferMode - auto, ascii, binary
			// retrycount (1) - Number of times to retry an operation
			// passphrase - passphrase for private key
			// key - absolute path to private key file
			// fingerprint - fingerprint of the server's public key
			// bufferSize - Buffer sie in bytes
			// secure (false) - FTP or SFTP if true
		};
		this.logger			= ftpService.getLogger();
	}

	/**
	 * An FTP component that allows you to interact with an FTP server.
	 * <p>
	 * The available actions are:
	 * <ul>
	 * <li>changedir</li>
	 * <li>close</li>
	 * <li>createDir</li>
	 * <li>existsDir</li>
	 * <li>existsFile</li>
	 * <li>getCurrentDir</li>
	 * <li>getfile</li>
	 * <li>listdir</li>
	 * <li>open</li>
	 * <li>putfile</li>
	 * <li>removeDir</li>
	 * <li>remove</li>
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
		FTPConnection	ftpConnection	= findOrInitializeConnection( context, attributes );
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
		    Struct.of(
		        "connection", ftpConnection,
		        "action", action,
		        "result", ftpResult,
		        "attributes", attributes
		    )
		);

		try {
			switch ( action.toLowerCase() ) {
				case "open" :
					runtime.announce(
					    FTPKeys.onFTPConnectionOpen,
					    Struct.of(
					        FTPKeys.connection, ftpConnection,
					        "attributes", attributes
					    )
					);
					ftpConnection.open(
					    StringCaster.cast( attributes.get( Key.server ) ),
					    IntegerCaster.cast( attributes.get( Key.port ) ),
					    StringCaster.cast( attributes.get( Key.username ) ),
					    StringCaster.cast( attributes.get( Key.password ) ),
					    BooleanCaster.cast( attributes.get( FTPKeys.passive ) ),
					    Duration.ofSeconds( IntegerCaster.cast( attributes.get( FTPKeys.timeout ) ) )
					);
					break;
				case "close" :
					runtime.announce(
					    FTPKeys.onFTPConnectionClose,
					    Struct.of( FTPKeys.connection, ftpConnection )
					);
					ftpConnection.close();
					break;
				case "changedir" :
					ftpConnection.changeDir( StringCaster.cast( attributes.get( Key.directory ) ) );
					break;
				case "createdir" :
					ftpConnection.createDir( StringCaster.cast( attributes.get( FTPKeys._new ) ) );
					break;
				case "getfile" :
					ftpConnection.getFile(
					    StringCaster.cast( attributes.get( FTPKeys.remoteFile ) ),
					    StringCaster.cast( attributes.get( FTPKeys.localFile ) )
					);
					break;
				case "remove" :
					ftpConnection.remove( StringCaster.cast( attributes.get( Key.item ) ) );
					break;
				case "removedir" :
					ftpConnection.removeDir( StringCaster.cast( attributes.get( Key.item ) ) );
					break;
				case "listdir" :
					Query files = ftpConnection.listdir();
					context.getDefaultAssignmentScope().put( Key.of( attributes.get( Key._name ) ), files );
					break;
				case "getcurrentdir" :
					returnValue = ftpConnection.getWorkingDirectory();
					break;
				case "existsfile" :
					returnValue = ftpConnection.existsFile( StringCaster.cast( attributes.get( FTPKeys.remoteFile ) ) );
					break;
				case "existsdir" :
					returnValue = ftpConnection.existsDir( StringCaster.cast( attributes.get( FTPKeys.directory ) ) );
					break;
				case "putfile" :
					// TODO: event onFTPPutFile
					ftpConnection.putFile(
					    StringCaster.cast( attributes.get( FTPKeys.localFile ) ),
					    StringCaster.cast( attributes.get( FTPKeys.remoteFile ) )
					);
			}
			;

			// Set our connection variable in the context
			if ( attributes.containsKey( FTPKeys.connection ) && attributes.get( FTPKeys.connection ) instanceof String s ) {
				context.getDefaultAssignmentScope().put( Key.of( s ), ftpConnection );
			}

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
			    Struct.of(
			        "connection", ftpConnection,
			        "action", action,
			        "result", ftpResult,
			        "attributes", attributes
			    )
			);

		} catch ( IOException e ) {
			String message = String.format( "Error executing action [%s] -> [%s]", action, e.getMessage() );
			this.logger.error( message, e );
			throw new BoxIOException( message, e );
		}

		return DEFAULT_RETURN;
	}

	/**
	 * Find or initialize a connection to the FTP server
	 *
	 * @param context    The context in which the Component is being invoked
	 * @param attributes The attributes to the Component
	 *
	 * @return The FTP connection
	 */
	private FTPConnection findOrInitializeConnection( IBoxContext context, IStruct attributes ) {
		String connectionName = attributes.getAsString( FTPKeys.connection ).trim();
		return this.ftpService.getOrBuildConnection( Key.of( connectionName ) );
	}
}
