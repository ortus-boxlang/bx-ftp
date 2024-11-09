package ortus.boxlang.ftp.components;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
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

		// variables.put( "username", System.getProperty( "BOXLANG_FTP_USERNAME" ) );
		// variables.put( "password", System.getProperty( "BOXLANG_FTP_PASSWORD" ) );
		variables.put( "username", "test_user" );
		variables.put( "password", "testpass" );
	}

	@DisplayName( "It can connect to an ftp server in active mode" )
	@Test
	public void testConnectWithUsernameAndPassword() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="result" username="#variables.username#" password="#variables.password#" server="127.0.0.1" />
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( FTPConnection.class );
	}

	@DisplayName( "It can connect to an ftp server in passive mode" )
	@Test
	public void testConnectWithUsernameAndPasswordPassive() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="result" username="#variables.username#" password="#variables.password#" server="127.0.0.1" passive="true" />
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

	@DisplayName( "It can list files" )
	@Test
	public void testListFilesPassive() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="test_user" password="testpass" server="localhost" passive=true />
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

	@DisplayName( "It can create a folder" )
	@Test
	public void testCreateDirActive() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="test_user" password="testpass" server="localhost"/>
				<bx:ftp action="removeDir" item="new_folder" connection="conn" stopOnError=false />
				<bx:ftp action="createDir" new="new_folder" connection="conn" stopOnError=true />
				<bx:ftp action="listdir" connection="conn" name="result"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( String[].class );

		String[]		arr				= ( String[] ) variables.get( result );
		List<String>	expectations	= List.of( "a sub folder", "file_a.txt", "something.txt", "new_folder" );

		assertThat( Arrays.asList( arr ) ).contains( "new_folder" );

	}

	@DisplayName( "It can delete a folder" )
	@Test
	public void testDeleteDirActive() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="test_user" password="testpass" server="localhost"/>
				<bx:ftp action="removeDir" item="new_folder" connection="conn" stopOnError=false />
				<bx:ftp action="createDir" new="new_folder" connection="conn" stopOnError=true />
				<bx:ftp action="removeDir" item="new_folder" connection="conn" stopOnError=true />
				<bx:ftp action="listdir" connection="conn" name="result"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( String[].class );

		String[] arr = ( String[] ) variables.get( result );

		assertThat( Arrays.asList( arr ) ).doesNotContain( "new_folder" );

	}

}
