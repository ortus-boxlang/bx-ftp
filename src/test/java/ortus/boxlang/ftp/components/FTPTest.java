package ortus.boxlang.ftp.components;

import static com.google.common.truth.Truth.assertThat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

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

		try {
			System.out.println( System.getProperty( "user.dir" ) );
			System.getProperties().load( new FileInputStream( "./resources/.env" ) );
		} catch ( FileNotFoundException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		variables.put( "username", System.getProperty( "BOXLANG_FTP_USERNAME" ) );
		variables.put( "password", System.getProperty( "BOXLANG_FTP_PASSWORD" ) );
		variables.put( "server", System.getProperty( "BOXLANG_FTP_SERVER", "localhost" ) );
		variables.put( "port", System.getProperty( "BOXLANG_FTP_PORT", "21" ) );
	}

	@DisplayName( "It can connect to an ftp server in active mode" )
	@Test
	public void testConnectWithUsernameAndPassword() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( "conn" ) ).isInstanceOf( FTPConnection.class );
	}

	@DisplayName( "It can connect to an ftp server in passive mode" )
	@Test
	public void testConnectWithUsernameAndPasswordPassive() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="true"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( "conn" ) ).isInstanceOf( FTPConnection.class );
	}

	@DisplayName( "It can list files" )
	@Test
	public void testListFiles() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"/>
				<bx:ftp action="listdir" connection="conn" name="result"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );

		Query			arr				= ( Query ) variables.get( result );
		List<String>	expectations	= List.of( "a_sub_folder", "file_a.txt", "something.txt" );

		for ( IStruct file : arr ) {
			assertThat( file.get( Key._name ) ).isIn( expectations );
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

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );

		Query			arr				= ( Query ) variables.get( result );

		List<String>	expectations	= List.of( "a_sub_folder", "file_a.txt", "something.txt" );

		for ( IStruct file : arr ) {
			assertThat( file.get( Key._name ) ).isIn( expectations );
		}

	}

	@DisplayName( "It can create a folder" )
	@Test
	public void testCreateDirActive() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"/>
				<bx:ftp action="removeDir" item="new_folder" connection="conn" stopOnError=false />
				<bx:ftp action="createDir" new="new_folder" connection="conn" stopOnError=true />
				<bx:ftp action="listdir" connection="conn" name="result"/>
				<bx:ftp action="removeDir" item="new_folder" connection="conn" stopOnError=false />
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Query arr = ( Query ) variables.get( result );

		assertThat( Arrays.asList( arr.getColumnData( Key._name ) ) ).contains( "new_folder" );

	}

	@DisplayName( "It can delete a folder" )
	@Test
	public void testDeleteDirActive() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"/>
				<bx:ftp action="removeDir" item="new_folder" connection="conn" stopOnError=false />
				<bx:ftp action="createDir" new="new_folder" connection="conn" stopOnError=true />
				<bx:ftp action="removeDir" item="new_folder" connection="conn" stopOnError=true />
				<bx:ftp action="listdir" connection="conn" name="result"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Query arr = ( Query ) variables.get( result );

		assertThat( Arrays.asList( arr.getColumnData( Key._name ) ) ).doesNotContain( "new_folder" );

	}

	@DisplayName( "It can change the working directory" )
	@Test
	public void testChangeWorkingDirectory() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"/>
				<bx:ftp action="changedir" connection="conn" directory="a_sub_folder"/>
				<bx:ftp action="listdir" connection="conn" name="result"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		FTPConnection conn = ( FTPConnection ) variables.get( "conn" );

		assertThat( conn ).isInstanceOf( FTPConnection.class );

		try {
			assertThat( conn.getWorkingDirectory() ).contains( "a_sub_folder" );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Query arr = ( Query ) variables.get( result );

		assertThat( Arrays.asList( arr.getColumnData( Key._name ) ) ).contains( "a-sub-file.md" );

	}

}
