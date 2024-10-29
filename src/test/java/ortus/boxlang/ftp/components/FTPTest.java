package ortus.boxlang.ftp.components;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.ftp.FTPConnection;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class FTPTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can connect to an ftp server" )
	@Test
	public void testConnectWithUsernameAndPassword() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="result" username="test_user" password="testpass" server="127.0.0.1" />
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( FTPConnection.class );
	}

	@DisplayName( "It can list files" )
	@Test
	public void testListFiles() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="test_user" password="testpass" server="localhost" />
				<bx:ftp action="listdir" connection="conn" name="result"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( String[].class );

		String[]		arr				= ( String[] ) variables.get( result );
		List<String>	expectations	= List.of( "a sub folder", "file_a.txt", "something.txt" );

		for ( String file : arr ) {
			assertThat( file ).isIn( expectations );
		}

	}

}
