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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.ftp.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
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

		variables.put( "username", dotenv.get( "BOXLANG_FTP_USERNAME" ) );
		variables.put( "password", dotenv.get( "BOXLANG_FTP_PASSWORD" ) );
		variables.put( "server", dotenv.get( "BOXLANG_FTP_SERVER", "localhost" ) );
		variables.put( "port", dotenv.get( "BOXLANG_FTP_PORT", "21" ) );
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
				<bx:ftp action="open"
					connection="conn"
					username="#variables.username#"
					password="#variables.password#"
					server="#variables.server#"
					port="#variables.port#"
					passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="listdir"
					connection="conn"
					directory="/"
					name="result"/>
				<bx:set println( result )>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );

		Query			arr				= variables.getAsQuery( result );
		List<String>	expectedFiles	= List.of( "a_sub_folder", "file_a.txt", "something.txt" );
		List<String>	actualFileNames	= arr.stream()
		    .map( file -> ( String ) file.get( Key._name ) )
		    .toList();

		// Assert all expected files exist (ignore test artifacts from concurrent runs)
		for ( String expectedFile : expectedFiles ) {
			assertThat( actualFileNames ).contains( expectedFile );
		}
	}

	@DisplayName( "It can list files as array of structs" )
	@Test
	public void testListFilesAsArrayOfStructs() {

		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="listdir" connection="conn" directory="/" name="result" returnType="array"/>
				<bx:set println( result )>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( Array.class );

		Array			arr				= variables.getAsArray( result );
		List<String>	expectedFiles	= List.of( "a_sub_folder", "file_a.txt", "something.txt" );
		List<String>	actualFileNames	= arr.stream()
		    .map( file -> ( String ) ( ( IStruct ) file ).get( Key._name ) )
		    .toList();

		// Assert all expected files exist (ignore test artifacts from concurrent runs)
		for ( String expectedFile : expectedFiles ) {
			assertThat( actualFileNames ).contains( expectedFile );
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
				// @formatter:off
				runtime.executeSource(
				    """
				      	<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#"  passive="#(variables.ftpMode == 'passive')#" />
				   		<bx:ftp action="getfile" connection="conn" remoteFile="something.txt" localFile="something.txt" result="myResult"/>
				      """,
				    context,
				    BoxSourceType.BOXTEMPLATE
				);
				// @formatter:on
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

	@DisplayName( "It can rename a file" )
	@Test
	public void testRenameFile() {
		// @formatter:off
		// Clean up any leftover renamed file first
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="remove" connection="conn" item="file_renamed.txt" stopOnError="false"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);

		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="renamefile" connection="conn" existing="file_a.txt" new="file_renamed.txt" result="renameResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct renameResult = variables.getAsStruct( Key.of( "renameResult" ) );
		assertThat( renameResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();

		// Verify file was renamed
		runtime.executeSource(
		    """
		    <bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
		    <bx:ftp action="existsfile" connection="conn" remoteFile="file_renamed.txt" result="existsResult"/>
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);

		IStruct existsResult = variables.getAsStruct( Key.of( "existsResult" ) );
		assertThat( existsResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();

		// Rename back for other tests
		runtime.executeSource(
		    """
		    <bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
		    <bx:ftp action="renamefile" connection="conn" existing="file_renamed.txt" new="file_a.txt"/>
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);
	}

	@DisplayName( "It can remove a file" )
	@Test
	public void testRemoveFile() {
		// @formatter:off
		// First create a test file
		runtime.executeSource(
			"""
				<bx:set fileWrite( "test_remove.txt", "to be deleted" ) />
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="putfile" connection="conn" remoteFile="test_remove.txt" localFile="test_remove.txt"/>
				<bx:set fileDelete( "test_remove.txt" ) />
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);

		// Now remove it
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="removefile" connection="conn" remoteFile="test_remove.txt" result="removeResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct removeResult = variables.getAsStruct( Key.of( "removeResult" ) );
		assertThat( removeResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();

		// Verify file was deleted
		runtime.executeSource(
		    """
		    <bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
		    <bx:ftp action="existsfile" connection="conn" remoteFile="test_remove.txt" result="existsResult"/>
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);

		IStruct existsResult = variables.getAsStruct( Key.of( "existsResult" ) );
		assertThat( existsResult.getAsBoolean( Key.of( "returnValue" ) ) ).isFalse();
	}

	@DisplayName( "It can rename a directory" )
	@Test
	public void testRenameDirectory() {
		// @formatter:off
		// Clean up any leftover directories first
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="removedir" connection="conn" directory="test_rename_dir" stopOnError="false"/>
				<bx:ftp action="removedir" connection="conn" directory="test_renamed_dir" stopOnError="false"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);

		// First create a test directory
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="createdir" connection="conn" new="test_rename_dir"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);

		// Now rename it
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
				<bx:ftp action="renamedir" connection="conn" existing="test_rename_dir" new="test_renamed_dir" result="renameResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct renameResult = variables.getAsStruct( Key.of( "renameResult" ) );
		assertThat( renameResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();

		// Verify directory was renamed
		runtime.executeSource(
		    """
		    <bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
		    <bx:ftp action="existsdir" connection="conn" directory="test_renamed_dir" result="existsResult"/>
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);

		IStruct existsResult = variables.getAsStruct( Key.of( "existsResult" ) );
		assertThat( existsResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();

		// Clean up
		runtime.executeSource(
		    """
		    <bx:ftp action="open" connection="conn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.port#" passive="#(variables.ftpMode == 'passive')#" />
		    <bx:ftp action="removedir" connection="conn" directory="test_renamed_dir"/>
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);
	}
}
