package ortus.boxlang.ftp.components;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
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
import ortus.boxlang.ftp.FTPResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class FTPTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result		= new Key( "result" );
	static Key			myResult	= new Key( "myResult" );
	static String		FTPMode		= "active";

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		String SystemFTPMode = System.getenv( "ftpMode" );
		if ( SystemFTPMode != null ) {
			FTPMode = SystemFTPMode;
		}
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );

		try {
			String	dir		= System.getProperty( "user.dir" );
			String	envFile	= "./resources/.env";

			if ( dir.endsWith( "bx-ftp" ) ) {
				envFile = dir + "/src/test/resources/.env";
			}

			System.getProperties().load( new FileInputStream( envFile ) );
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
		// variables.put( "mode", FTPMode );
		variables.put( "ftpMode", "passive" );
	}

	@DisplayName( "It can connect to an ftp server in active mode" )
	@Test
	public void testConnectWithUsernameAndPassword() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( "conn" ) ).isInstanceOf( FTPConnection.class );
		assertThat( variables.get( "myResult" ) ).isInstanceOf( FTPResult.class );

		FTPResult ftpResult = ( FTPResult ) variables.get( "myResult" );
		assertThat( ftpResult.isSuccessful() ).isTrue();
		assertThat( ftpResult.getStatusCode() ).isEqualTo( 230 );
		assertThat( ftpResult.getStatusText() ).isEqualTo( "230 Login successful." );
	}

	@DisplayName( "It can connect to an ftp server in passive mode" )
	@Test
	public void testConnectWithUsernameAndPasswordPassive() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="true" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( "conn" ) ).isInstanceOf( FTPConnection.class );
		assertThat( variables.get( "myResult" ) ).isInstanceOf( FTPResult.class );

		FTPResult ftpResult = ( FTPResult ) variables.get( "myResult" );
		assertThat( ftpResult.isSuccessful() ).isTrue();
		assertThat( ftpResult.getStatusCode() ).isEqualTo( 230 );
		assertThat( ftpResult.getStatusText() ).isEqualTo( "230 Login successful." );
	}

	@DisplayName( "It can list files" )
	@Test
	public void testListFiles() {

		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="listdir" connection="conn" directory="/" name="result"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );

		Query			arr				= variables.getAsQuery( result );
		List<String>	expectations	= List.of( "a_sub_folder", "file_a.txt", "something.txt" );

		for ( IStruct file : arr ) {
			assertThat( file.get( Key._name ) ).isIn( expectations );
		}

	}

	@DisplayName( "It can create a folder" )
	@Test
	public void testCreateFolder() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="removeDir" item="new_folder" connection="conn" stopOnError=false />
				<bx:ftp action="createDir" new="new_folder" connection="conn" stopOnError=true />
				<bx:ftp action="listdir" connection="conn" name="result"/>
				<bx:ftp action="removeDir" item="new_folder" connection="conn" stopOnError=false />
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Query arr = variables.getAsQuery( result );

		assertThat( Arrays.asList( arr.getColumnData( Key._name ) ) ).contains( "new_folder" );

	}

	@DisplayName( "It can delete a folder" )
	@Test
	public void testDeleteDirectory() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#"/>
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
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
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

	@DisplayName( "It can get the current working directory" )
	@Test
	public void testGetWorkingDirectory() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="getCurrentDir" connection="conn" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		FTPResult ftpResult = ( FTPResult ) variables.get( myResult );
		assertThat( ftpResult.isSuccessful() ).isTrue();
		assertThat( ftpResult.getStatusCode() ).isEqualTo( 257 );
	}

	@DisplayName( "It can check if a file exists" )
	@Test
	public void testFileExists() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="existsFile" connection="conn" remoteFile="something.txt" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		FTPResult ftpResult = ( FTPResult ) variables.get( myResult );
		assertThat( ftpResult.isSuccessful() ).isTrue();
		assertThat( ( Boolean ) ftpResult.getReturnValue() ).isTrue();
		assertThat( ftpResult.getStatusCode() ).isEqualTo( 226 );
	}

	@DisplayName( "It can check if a file does not exist" )
	@Test
	public void testFileDoesNotExist() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="existsFile" connection="conn" remoteFile="does_not_exist.txt" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		FTPResult ftpResult = ( FTPResult ) variables.get( myResult );
		assertThat( ftpResult.isSuccessful() ).isTrue();
		assertThat( ( Boolean ) ftpResult.getReturnValue() ).isFalse();
		assertThat( ftpResult.getStatusCode() ).isEqualTo( 226 );
	}

	@DisplayName( "It can check if a directory exists" )
	@Test
	public void testDirectoryExists() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="existsDir" connection="conn" directory="a_sub_folder" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		FTPResult ftpResult = ( FTPResult ) variables.get( myResult );
		assertThat( ftpResult.isSuccessful() ).isTrue();
		assertThat( ( Boolean ) ftpResult.getReturnValue() ).isTrue();
		assertThat( ftpResult.getStatusCode() ).isEqualTo( 250 );
	}

	@DisplayName( "It can check if a directory does not exist" )
	@Test
	public void testDirectoryDoesNotExist() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="existsDir" connection="conn" directory="does_not_exist" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		FTPResult ftpResult = ( FTPResult ) variables.get( myResult );
		assertThat( ftpResult.isSuccessful() ).isTrue();
		assertThat( ( Boolean ) ftpResult.getReturnValue() ).isFalse();
		assertThat( ftpResult.getStatusCode() ).isEqualTo( 250 );
	}

	@DisplayName( "It can close the connection" )
	@Test
	public void testCloseConnection() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="close" connection="conn" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( "myResult" ) ).isInstanceOf( FTPResult.class );

		FTPResult ftpResult = ( FTPResult ) variables.get( "myResult" );
		assertThat( ftpResult.isSuccessful() ).isTrue();
		assertThat( ftpResult.getStatusCode() ).isEqualTo( 221 );
	}

	@DisplayName( "It can change the working directory" )
	@Test
	public void testChangeWorkingDirectoryWithRelativePath() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="changedir" connection="conn" directory="a_sub_folder"/>
				<bx:ftp action="changedir" connection="conn" directory=".." />
				<bx:ftp action="listdir" connection="conn" name="result"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		FTPConnection conn = ( FTPConnection ) variables.get( "conn" );

		assertThat( conn ).isInstanceOf( FTPConnection.class );

		try {
			assertThat( conn.getWorkingDirectory() ).doesNotContain( "a_sub_folder" );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Query arr = ( Query ) variables.get( result );

		assertThat( Arrays.asList( arr.getColumnData( Key._name ) ) ).contains( "a_sub_folder" );

	}

	@DisplayName( "It can get a file" )
	@Test
	public void testGetFile() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="getfile" connection="conn" remoteFile="something.txt" localFile="something.txt" result="myResult"/>
				<bx:set result = fileExists( "something.txt" ) />
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( myResult ) ).isInstanceOf( FTPResult.class );

		FTPResult ftpResult = ( FTPResult ) variables.get( myResult );
		assertThat( ftpResult.isSuccessful() ).isTrue();
		assertThat( ftpResult.getStatusCode() ).isEqualTo( 226 );
		assertThat( variables.getAsBoolean( Key.result ) ).isTrue();

		BoxRuntimeException exception = assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			       <bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#" />
			    <bx:ftp action="getfile" connection="conn" remoteFile="something.txt" localFile="something.txt" result="myResult"/>
			       """,
			    context,
			    BoxSourceType.BOXTEMPLATE
			);
		} );

		assertThat( exception.getMessage() ).contains( "Local file already exists" );

		File file = new File( "something.txt" );

		if ( file.exists() ) {
			file.delete();
		}
	}

	@DisplayName( "It can put a file" )
	@Test
	public void testPutFile() {
		// @formatter:off
		instance.executeSource(
			"""
				<bx:set fileWrite( "test_put.txt", "somedata" ) />
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="putfile" connection="conn" remoteFile="test_put.txt" localFile="test_put.txt" result="myResult"/>
				<bx:set fileDelete( "test_put.txt" ) />
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		FTPResult ftpResult = ( FTPResult ) variables.get( myResult );
		assertThat( ftpResult.isSuccessful() ).isTrue();
		assertThat( ftpResult.getStatusCode() ).isEqualTo( 226 );

		File file = new File( "resources/ftp_files/test_put.txt" );

		if ( file.exists() ) {
			file.delete();
		}

		BoxRuntimeException exception = assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    <bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#" />
			    <bx:ftp action="putfile" connection="conn" remoteFile="something.txt" localFile="does_not_exists.txt" result="myResult"/>
			    """,
			    context,
			    BoxSourceType.BOXTEMPLATE
			);
		} );
	}
}
