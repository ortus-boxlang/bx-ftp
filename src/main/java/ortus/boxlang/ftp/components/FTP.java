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
		    new Attribute( Key.action, "string", Set.of( Validator.REQUIRED, Validator.valueOneOf( VALID_ACTIONS ) ) ),
		    new Attribute( Key._name, "string" ),
		    new Attribute( Key.username, "string" ),
		    new Attribute( Key.password, "string" ),
		    new Attribute( Key.port, "numeric", 21 ),
		    new Attribute( Key.server, "string" ),
		    new Attribute( Key.item, "string" ),
		    new Attribute( FTPKeys._new, "string" ),
		    new Attribute( FTPKeys.stopOnError, "boolean" ),
		    new Attribute( FTPKeys.passive, "boolean", false ),
		    new Attribute( FTPKeys.connection, "string", Set.of( Validator.REQUIRED ) ),
		    new Attribute( FTPKeys.remoteFile, "string" ),
		    new Attribute( FTPKeys.localFile, "string" ),
		    new Attribute( FTPKeys.timeout, "numeric", 30 )
		};
		this.logger			= ftpService.getLogger();
	}

	/**
	 * An FTP component that allows you to interact with an FTP server.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.name The name of the person greeting us.
	 *
	 * @attribute.location The location of the person.
	 *
	 * @attribute.shout Whether the person is shouting or not.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		FTPConnection	ftpConnection		= findOrInitializeConnection( context, attributes );
		FTPResult		ftpResult			= new FTPResult( ftpConnection );
		String			action				= StringCaster.cast( attributes.get( Key.action ) ).toLowerCase();
		Boolean			stopOnErrorValue	= attributes.containsKey( FTPKeys.stopOnError ) ? BooleanCaster.cast( attributes.get( FTPKeys.stopOnError ) )
		    : false;
		Object			returnValue			= null;

		if ( stopOnErrorValue != false ) {
			ftpConnection.setStopOnError( stopOnErrorValue );
		}

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
			if ( attributes.containsKey( Key.result ) && attributes.get( Key.result ) instanceof String s ) {
				context.getDefaultAssignmentScope().put( Key.of( s ), ftpResult.toStruct() );
			} else {
				context.getDefaultAssignmentScope().put( FTPKeys.bxftp, ftpResult.toStruct() );
			}

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
