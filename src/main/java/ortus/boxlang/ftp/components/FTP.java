package ortus.boxlang.ftp.components;

import java.io.IOException;
import java.util.Set;

import ortus.boxlang.ftp.FTPConnection;
import ortus.boxlang.ftp.FTPKeys;
import ortus.boxlang.ftp.FTPResult;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = false )
public class FTP extends Component {

	public final static String[] actions = new String[] {
	    "open",
	    "listdir",
	    "createDir",
	    "removeDir",
	    "changedir",
	    "getCurrentDir",
	    "close",
	    "existsFile",
	    "existsDir"
	};

	public FTP() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.action, "string", Set.of( Validator.REQUIRED, Validator.valueOneOf( actions ) ) ),
		    new Attribute( Key._name, "string" ),
		    new Attribute( Key.username, "string" ),
		    new Attribute( Key.password, "string" ),
		    new Attribute( Key.port, "numeric", 21 ),
		    new Attribute( Key.server, "string" ),
		    new Attribute( Key.item, "string" ),
		    new Attribute( FTPKeys._new, "string" ),
		    new Attribute( FTPKeys.stopOnError, "boolean" ),
		    new Attribute( FTPKeys.passive, "boolean", false ),
		    new Attribute( FTPKeys.connection, "string", Set.of( Validator.REQUIRED ) )
		};
	}

	/**
	 * An example component that says hello
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
					ftpConnection.open(
					    StringCaster.cast( attributes.get( Key.server ) ),
					    IntegerCaster.cast( attributes.get( Key.port ) ),
					    StringCaster.cast( attributes.get( Key.username ) ),
					    StringCaster.cast( attributes.get( Key.password ) ),
					    BooleanCaster.cast( attributes.get( FTPKeys.passive ) )
					);
					break;
				case "close" :
					ftpConnection.close();
					break;
				case "changedir" :
					ftpConnection.changeDir( StringCaster.cast( attributes.get( Key.directory ) ) );
					break;
				case "createdir" :
					ftpConnection.createDir( StringCaster.cast( attributes.get( FTPKeys._new ) ) );
					break;
				case "removedir" :
					ftpConnection.removeDir( StringCaster.cast( attributes.get( Key.item ) ) );
					break;
				case "listdir" :
					try {
						Query files = ftpConnection.listdir();
						context.getDefaultAssignmentScope().put( Key.of( attributes.get( Key._name ) ), files );
					} catch ( IOException e ) {

						e.printStackTrace();
					}
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
				context.getDefaultAssignmentScope().put( Key.of( s ), ftpResult );
			} else {
				context.getDefaultAssignmentScope().put( FTPKeys.bxftp, ftpResult );
			}

		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return DEFAULT_RETURN;
	}

	private FTPConnection findOrInitializeConnection( IBoxContext context, IStruct attributes ) {
		Object ftpConnection = attributes.get( FTPKeys.connection );

		if ( ftpConnection instanceof FTPConnection f ) {
			return f;
		}

		if ( ! ( ftpConnection instanceof String ) ) {
			throw new BoxRuntimeException( "connection is not a valid FTPConnection" );
		}

		String				connectionName	= StringCaster.cast( ftpConnection );

		ScopeSearchResult	result			= context.scopeFindNearby( Key.of( connectionName ), context.getDefaultAssignmentScope() );

		if ( result.value() instanceof FTPConnection f ) {
			return f;
		}

		FTPConnection conn = new FTPConnection();

		context.getDefaultAssignmentScope().assign( context, Key.of( connectionName ), conn );

		return conn;
	}
}
