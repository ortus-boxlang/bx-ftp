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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.ftp.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class FTPTest extends BaseIntegrationTest {

	static Key		myResultKey	= new Key( "myResult" );
	static String	FTPMode		= "active";

	@BeforeAll
	public static void setUp() {
		String SystemFTPMode = System.getenv( "ftpMode" );
		if ( SystemFTPMode != null ) {
			FTPMode = SystemFTPMode;
		}
	}

	@BeforeEach
	@Override
	public void setupEach() {
		super.setupEach();
		try {
			String	dir		= System.getProperty( "user.dir" );
			String	envFile	= "./resources/.env";

			if ( dir.endsWith( "bx-ftp" ) ) {
				envFile = dir + "/src/test/resources/.env";
			}
			System.getProperties().load( new FileInputStream( envFile ) );
		} catch ( IOException e ) {
			e.printStackTrace();
		}

		variables.put( "username", System.getProperty( "BOXLANG_FTP_USERNAME" ) );
		variables.put( "password", System.getProperty( "BOXLANG_FTP_PASSWORD" ) );
		variables.put( "server", System.getProperty( "BOXLANG_FTP_SERVER", "localhost" ) );
		variables.put( "port", System.getProperty( "BOXLANG_FTP_PORT", "21" ) );
		// variables.put( "mode", FTPMode );
		variables.put( "ftpMode", "passive" );
	}

	@DisplayName( "It can connect to an ftp server in choosen mode" )
	@Test
	public void testConnectWithUsernameAndPassword() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open"
					connection="conn"
					username="#variables.username#"
					password="#variables.password#"
					server="#variables.server#"
					port="#variables.port#"
					result="myResult"/>

				<bx:script>
					println( conn );
					println( myResult )
				</bx:script>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct myResult = ( IStruct ) variables.get( "myResult" );
		assertThat( myResult.getAsBoolean( Key.of( "succeeded" ) ) ).isTrue();
	}

	@DisplayName( "It can connect to an ftp server in passive mode" )
	@Test
	public void testConnectWithUsernameAndPasswordPassive() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open"
					connection="conn"
					username="#variables.username#"
					password="#variables.password#"
					server="#variables.server#"
					port="#variables.port#"
					passive="true"
					result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( "conn" ).getClass().getSimpleName() ).isEqualTo( "FTPConnection" );

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "succeeded" ) ) ).isTrue();
	}

	@DisplayName( "It can list files" )
	@Test
	public void testListFiles() {

		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="listdir" connection="conn" directory="/" name="result"/>
				<bx:set println( result )>
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
		runtime.executeSource(
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
		runtime.executeSource(
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
		runtime.executeSource(
			"""
				<bx:ftp action="open"
					connection="conn"
					username="#variables.username#"
					password="#variables.password#"
					server="#variables.server#"
					port="#variables.port#"
					passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="changedir" connection="conn" directory="a_sub_folder"/>
				<bx:ftp action="listdir" connection="conn" name="result"/>
				<bx:set conn = conn.getMetadata()>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct conn = variables.getAsStruct( Key.of( "conn" ) );
		assertThat( conn.getAsString( Key.of( "workingDirectory" ) ) ).contains( "a_sub_folder" );

		Query arr = ( Query ) variables.get( result );

		assertThat( Arrays.asList( arr.getColumnData( Key._name ) ) ).contains( "a-sub-file.md" );
	}

	@DisplayName( "It can get the current working directory" )
	@Test
	public void testGetWorkingDirectory() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="getCurrentDir" connection="conn" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "succeeded" ) ) ).isTrue();
	}

	@DisplayName( "It can check if a file exists" )
	@Test
	public void testFileExists() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="existsFile" connection="conn" remoteFile="something.txt" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "succeeded" ) ) ).isTrue();
		assertThat( ftpResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();
	}

	@DisplayName( "It can check if a file does not exist" )
	@Test
	public void testFileDoesNotExist() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="existsFile" connection="conn" remoteFile="does_not_exist.txt" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "succeeded" ) ) ).isTrue();
		assertThat( ftpResult.getAsBoolean( Key.of( "returnValue" ) ) ).isFalse();
	}

	@DisplayName( "It can check if a directory exists" )
	@Test
	public void testDirectoryExists() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="existsDir" connection="conn" directory="a_sub_folder" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "succeeded" ) ) ).isTrue();
		assertThat( ftpResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();
	}

	@DisplayName( "It can check if a directory does not exist" )
	@Test
	public void testDirectoryDoesNotExist() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp
					action="open"
					connection="conn"
					username="#variables.username#"
					password="#variables.password#"
					server="#variables.server#"
					port="#variables.port#"
					passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="existsDir" connection="conn" directory="does_not_exist" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "succeeded" ) ) ).isTrue();
		assertThat( ftpResult.getAsBoolean( Key.of( "returnValue" ) ) ).isFalse();
	}

	@DisplayName( "It can close an opened connection" )
	@Test
	public void testCloseConnection() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp
					action="open"
					connection="conn"
					username="#variables.username#"
					password="#variables.password#"
					server="#variables.server#"
					port="#variables.port#"
					passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp
					action="close"
					connection="conn"
					result="myResult"/>
				<bx:script>
					println( conn );
					println( myResult )
					isOpen = conn.isConnected();
				</bx:script>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "succeeded" ) ) ).isTrue();
		assertThat(
		    variables.getAsBoolean( Key.of( "isOpen" ) )
		).isFalse();
	}

	@DisplayName( "It can ignore a closed or non-existed connection when closing" )
	@Test
	public void testCloseConnectionIgnore() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp
					action="close"
					connection="bogus"
					result="myResult"/>
				<bx:script>
					println( bogus );
					isOpen = bogus.isConnected();
				</bx:script>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( variables.getAsBoolean( Key.of( "isOpen" ) ) ).isFalse();
	}

	@DisplayName( "It can change the working directory" )
	@Test
	public void testChangeWorkingDirectoryWithRelativePath() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="changedir" connection="conn" directory="a_sub_folder"/>
				<bx:ftp action="changedir" connection="conn" directory=".." />
				<bx:ftp action="listdir" connection="conn" name="result"/>
				<bx:set conn = conn.getMetadata()>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct conn = variables.getAsStruct( Key.of( "conn" ) );
		assertThat( conn.getAsString( Key.of( "workingDirectory" ) ) ).doesNotContain( "a_sub_folder" );

		Query arr = ( Query ) variables.get( result );
		assertThat( Arrays.asList( arr.getColumnData( Key._name ) ) ).contains( "a_sub_folder" );

	}

	@DisplayName( "It can get a file" )
	@Test
	public void testGetFile() {
		try {
			// @formatter:off
			runtime.executeSource(
				"""
					<bx:ftp
						action="open"
						connection="conn"
						username="#variables.username#"
						password="#variables.password#"
						server="#variables.server#"
						port="#variables.port#"
						passive="#(variables.ftpMode == 'passive')#" />
					<bx:ftp
						action="getfile"
						connection="conn"
						remoteFile="something.txt"
						localFile="something.txt"
						result="myResult"/>
					<bx:set result = fileExists( "something.txt" ) />
					<bx:set println( myResult )>
				""",
				context,
				BoxSourceType.BOXTEMPLATE
			);
			// @formatter:on

			IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
			assertThat( ftpResult.getAsBoolean( Key.of( "succeeded" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.result ) ).isTrue();

			BoxRuntimeException exception = assertThrows( BoxRuntimeException.class, () -> {
				runtime.executeSource(
				    """
				      <bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#" />
				    <bx:ftp action="getfile" connection="conn" remoteFile="something.txt" localFile="something.txt" result="myResult"/>
				      """,
				    context,
				    BoxSourceType.BOXTEMPLATE
				);
			} );
			assertThat( exception.getMessage() ).contains( "Local file already exists" );
		} finally {
			File file = new File( "something.txt" );
			if ( file.exists() ) {
				file.delete();
			}
		}

	}

	@DisplayName( "It can put a file" )
	@Test
	public void testPutFile() {
		// @formatter:off
		runtime.executeSource(
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

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "succeeded" ) ) ).isTrue();

		runtime.executeSource(
		    """
		    <bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#" />
		    <bx:ftp action="remove" connection="conn" item="test_put.txt" />
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);

		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
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
