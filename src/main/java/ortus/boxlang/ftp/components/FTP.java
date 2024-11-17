package ortus.boxlang.ftp.components;

import java.io.IOException;
import java.util.Set;

import ortus.boxlang.ftp.FTPConnection;
import ortus.boxlang.ftp.FTPKeys;
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
	    "removeDir"
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
		    new Attribute( FTPKeys.connection, "string" )
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
		FTPConnection	ftpConnection		= findConnection( context, attributes );
		String			action				= StringCaster.cast( attributes.get( Key.action ) ).toLowerCase();
		Boolean			stopOnErrorValue	= attributes.containsKey( FTPKeys.stopOnError ) ? BooleanCaster.cast( attributes.get( FTPKeys.stopOnError ) )
		    : null;

		if ( stopOnErrorValue != null ) {
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

					if ( attributes.containsKey( FTPKeys.connection ) && attributes.get( FTPKeys.connection ) instanceof String s ) {
						context.getDefaultAssignmentScope().put( Key.of( s ), ftpConnection );
					}

					break;

				case "createdir" :
					ftpConnection.createDir( StringCaster.cast( attributes.get( FTPKeys._new ) ) );
					break;

				case "removedir" :
					ftpConnection.removeDir( StringCaster.cast( attributes.get( Key.item ) ) );
					break;

				case "listdir" :
					Query files = ftpConnection.listdir();

					context.getDefaultAssignmentScope().put( Key.of( attributes.get( Key._name ) ), files );
					break;
			}
			;
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return DEFAULT_RETURN;
	}

	private FTPConnection findConnection( IBoxContext context, IStruct attributes ) {
		if ( !attributes.containsKey( FTPKeys.connection ) ) {
			return new FTPConnection();
		}

		Object ftpConnection = attributes.get( FTPKeys.connection );

		if ( ftpConnection instanceof FTPConnection f ) {
			return f;
		} else if ( ftpConnection instanceof String s ) {
			ScopeSearchResult result = context.scopeFindNearby( Key.of( s ), context.getDefaultAssignmentScope() );

			if ( result.value() instanceof FTPConnection f ) {
				return f;
			} else if ( result.value() == null ) {
				return new FTPConnection();
			}

			throw new BoxRuntimeException( "connection is not a valid FTPConnection" );
		} else if ( ftpConnection == null ) {
			return new FTPConnection();
		}

		throw new BoxRuntimeException( "connection is not a valid FTPConnection" );
	}
}
