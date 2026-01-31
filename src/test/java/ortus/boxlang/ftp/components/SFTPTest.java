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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.ftp.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

/**
 * Integration tests for SFTP functionality
 */
public class SFTPTest extends BaseIntegrationTest {

	static Key myResultKey = new Key( "myResult" );

	@BeforeEach
	@Override
	public void setupEach() {
		super.setupEach();

		// Setup SFTP connection variables
		variables.put( "username", dotenv.get( "BOXLANG_FTP_USERNAME" ) );
		variables.put( "password", dotenv.get( "BOXLANG_FTP_PASSWORD" ) );
		variables.put( "server", dotenv.get( "BOXLANG_FTP_SERVER", "localhost" ) );
		variables.put( "sftpPort", dotenv.get( "BOXLANG_SFTP_PORT", "2222" ) );
	}

	@DisplayName( "It can connect to an SFTP server with password" )
	@Test
	public void testConnectWithPassword() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open"
					connection="sftpConn"
					username="#variables.username#"
					password="#variables.password#"
					server="#variables.server#"
					port="#variables.sftpPort#"
					secure="true"
					result="myResult"/>
				<bx:set sftpMeta = sftpConn.getMetadata()>
				<bx:script>
					println( sftpConn );
					println( myResult )
				</bx:script>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct myResult = ( IStruct ) variables.get( "myResult" );
		assertThat( myResult.getAsBoolean( Key.of( "Succeeded" ) ) ).isTrue();

		// Verify it's an SFTP connection by checking the secure flag in metadata
		IStruct metadata = variables.getAsStruct( Key.of( "sftpMeta" ) );
		assertThat( metadata.getAsBoolean( Key.of( "secure" ) ) ).isTrue();
	}

	@DisplayName( "It can list files on SFTP server" )
	@Test
	public void testListFiles() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open"
					connection="sftpConn"
					username="#variables.username#"
					password="#variables.password#"
					server="#variables.server#"
					port="#variables.sftpPort#"
					secure="true" />
				<bx:ftp action="listdir"
					connection="sftpConn"
				directory="."
					name="result"/>
				<bx:set println( result )>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );

		Query			arr				= variables.getAsQuery( result );
		List<String>	expectations	= List.of( "a_sub_folder", "file_a.txt", "something.txt" );
		List<String>	actualNames		= new ArrayList<>();

		for ( IStruct file : arr ) {
			actualNames.add( file.getAsString( Key._name ) );
		}

		// Check that all expected files are present (allow extras from previous test runs)
		for ( String expected : expectations ) {
			assertThat( actualNames ).contains( expected );
		}
	}

	@DisplayName( "It can list files as array of structs on SFTP server" )
	@Test
	public void testListFilesAsArrayOfStructs() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
			<bx:ftp action="listdir" connection="sftpConn" directory="." name="result" returnType="array"/>
				<bx:set println( result )>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( Array.class );

		Array			arr				= variables.getAsArray( result );
		List<String>	expectations	= List.of( "a_sub_folder", "file_a.txt", "something.txt" );
		List<String>	actualNames		= new ArrayList<>();

		for ( Object file : arr ) {
			IStruct targetFile = ( IStruct ) file;
			actualNames.add( targetFile.getAsString( Key._name ) );
		}

		// Check that all expected files are present (allow extras from previous test runs)
		for ( String expected : expectations ) {
			assertThat( actualNames ).contains( expected );
		}
	}

	@DisplayName( "It can create a folder on SFTP server" )
	@Test
	public void testCreateFolder() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
			<bx:ftp action="removeDir" directory="new_folder" connection="sftpConn" stopOnError=false />
			<bx:ftp action="createDir" new="new_folder" connection="sftpConn" stopOnError=true />
			<bx:ftp action="listdir" connection="sftpConn" name="result"/>
			<bx:ftp action="removeDir" directory="new_folder" connection="sftpConn" stopOnError=false />
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Query arr = variables.getAsQuery( result );

		assertThat( Arrays.asList( arr.getColumnData( Key._name ) ) ).contains( "new_folder" );
	}

	@DisplayName( "It can delete a folder on SFTP server" )
	@Test
	public void testDeleteDirectory() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true"/>
			<bx:ftp action="removeDir" directory="new_folder" connection="sftpConn" stopOnError=false />
			<bx:ftp action="createDir" new="new_folder" connection="sftpConn" stopOnError=true />
			<bx:ftp action="removeDir" directory="new_folder" connection="sftpConn" stopOnError=true />
				<bx:ftp action="listdir" connection="sftpConn" name="result"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Query arr = ( Query ) variables.get( result );

		assertThat( Arrays.asList( arr.getColumnData( Key._name ) ) ).doesNotContain( "new_folder" );
	}

	@DisplayName( "It can change the working directory on SFTP server" )
	@Test
	public void testChangeWorkingDirectory() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open"
					connection="sftpConn"
					username="#variables.username#"
					password="#variables.password#"
					server="#variables.server#"
					port="#variables.sftpPort#"
					secure="true" />
				<bx:ftp action="changedir" connection="sftpConn" directory="a_sub_folder"/>
				<bx:ftp action="listdir" connection="sftpConn" name="result"/>
				<bx:set sftpConn = sftpConn.getMetadata()>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct sftpConn = variables.getAsStruct( Key.of( "sftpConn" ) );
		assertThat( sftpConn.getAsString( Key.of( "workingDirectory" ) ) ).contains( "a_sub_folder" );

		Query arr = ( Query ) variables.get( result );

		assertThat( Arrays.asList( arr.getColumnData( Key._name ) ) ).contains( "a-sub-file.md" );
	}

	@DisplayName( "It can get the current working directory on SFTP server" )
	@Test
	public void testGetWorkingDirectory() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="getCurrentDir" connection="sftpConn" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "Succeeded" ) ) ).isTrue();
	}

	@DisplayName( "It can check if a file exists on SFTP server" )
	@Test
	public void testFileExists() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="existsFile" connection="sftpConn" remoteFile="something.txt" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "Succeeded" ) ) ).isTrue();
		assertThat( ftpResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();
	}

	@DisplayName( "It can check if a file does not exist on SFTP server" )
	@Test
	public void testFileDoesNotExist() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="existsFile" connection="sftpConn" remoteFile="does_not_exist.txt" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "Succeeded" ) ) ).isTrue();
		assertThat( ftpResult.getAsBoolean( Key.of( "returnValue" ) ) ).isFalse();
	}

	@DisplayName( "It can check if a directory exists on SFTP server" )
	@Test
	public void testDirectoryExists() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="existsDir" connection="sftpConn" directory="a_sub_folder" result="myResult"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "Succeeded" ) ) ).isTrue();
		assertThat( ftpResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();
	}

	@DisplayName( "It can close an SFTP connection" )
	@Test
	public void testCloseConnection() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:ftp
					action="open"
					connection="sftpConn"
					username="#variables.username#"
					password="#variables.password#"
					server="#variables.server#"
					port="#variables.sftpPort#"
					secure="true" />
				<bx:ftp
					action="close"
					connection="sftpConn"
					result="myResult"/>
				<bx:script>
					println( sftpConn );
					println( myResult )
					isOpen = sftpConn.isConnected();
				</bx:script>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "Succeeded" ) ) ).isTrue();
		assertThat(
		    variables.getAsBoolean( Key.of( "isOpen" ) )
		).isFalse();
	}

	@DisplayName( "It can get a file from SFTP server" )
	@Test
	public void testGetFile() {
		try {
			// @formatter:off
			runtime.executeSource(
				"""
					<bx:ftp
						action="open"
						connection="sftpConn"
						username="#variables.username#"
						password="#variables.password#"
						server="#variables.server#"
						port="#variables.sftpPort#"
						secure="true" />
					<bx:ftp
						action="getfile"
						connection="sftpConn"
						remoteFile="something.txt"
						localFile="something_sftp.txt"
						result="myResult"/>
					<bx:set result = fileExists( "something_sftp.txt" ) />
					<bx:set println( myResult )>
				""",
				context,
				BoxSourceType.BOXTEMPLATE
			);
			// @formatter:on

			IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
			assertThat( ftpResult.getAsBoolean( Key.of( "Succeeded" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.result ) ).isTrue();
		} finally {
			File file = new File( "something_sftp.txt" );
			if ( file.exists() ) {
				file.delete();
			}
		}
	}

	@DisplayName( "It can put a file to SFTP server" )
	@Test
	public void testPutFile() {
		// @formatter:off
		runtime.executeSource(
			"""
				<bx:set fileWrite( "test_put_sftp.txt", "somedata" ) />
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="putfile" connection="sftpConn" remoteFile="test_put_sftp.txt" localFile="test_put_sftp.txt" result="myResult"/>
				<bx:set fileDelete( "test_put_sftp.txt" ) />
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		IStruct ftpResult = variables.getAsStruct( Key.of( "myResult" ) );
		assertThat( ftpResult.getAsBoolean( Key.of( "Succeeded" ) ) ).isTrue();

		// Clean up remote file
		runtime.executeSource(
		    """
		    <bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
		    <bx:ftp action="remove" connection="sftpConn" item="test_put_sftp.txt" />
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);
	}

	@DisplayName( "It can rename a file on SFTP server" )
	@Test
	public void testRenameFile() {
		// @formatter:off
		// Clean up any leftover renamed file first
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="remove" connection="sftpConn" item="file_renamed_sftp.txt" stopOnError="false"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);

		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="renamefile" connection="sftpConn" existing="file_a.txt" new="file_renamed_sftp.txt" result="renameResult"/>
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
		    <bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
		    <bx:ftp action="existsfile" connection="sftpConn" remoteFile="file_renamed_sftp.txt" result="existsResult"/>
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);

		IStruct existsResult = variables.getAsStruct( Key.of( "existsResult" ) );
		assertThat( existsResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();

		// Rename back for other tests
		runtime.executeSource(
		    """
		    <bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
		    <bx:ftp action="renamefile" connection="sftpConn" existing="file_renamed_sftp.txt" new="file_a.txt"/>
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);
	}

	@DisplayName( "It can remove a file from SFTP server" )
	@Test
	public void testRemoveFile() {
		// @formatter:off
		// First create a test file
		runtime.executeSource(
			"""
				<bx:set fileWrite( "test_remove_sftp.txt", "to be deleted" ) />
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="putfile" connection="sftpConn" remoteFile="test_remove_sftp.txt" localFile="test_remove_sftp.txt"/>
				<bx:set fileDelete( "test_remove_sftp.txt" ) />
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);

		// Now remove it
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="removefile" connection="sftpConn" remoteFile="test_remove_sftp.txt" result="removeResult"/>
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
		    <bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
		    <bx:ftp action="existsfile" connection="sftpConn" remoteFile="test_remove_sftp.txt" result="existsResult"/>
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);

		IStruct existsResult = variables.getAsStruct( Key.of( "existsResult" ) );
		assertThat( existsResult.getAsBoolean( Key.of( "returnValue" ) ) ).isFalse();
	}

	@DisplayName( "It can rename a directory on SFTP server" )
	@Test
	public void testRenameDirectory() {
		// @formatter:off
		// Clean up any leftover directories first
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="removedir" connection="sftpConn" directory="test_rename_dir_sftp" stopOnError="false"/>
				<bx:ftp action="removedir" connection="sftpConn" directory="test_renamed_dir_sftp" stopOnError="false"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);

		// First create a test directory
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="createdir" connection="sftpConn" new="test_rename_dir_sftp"/>
		    """,
			context,
			BoxSourceType.BOXTEMPLATE
		);

		// Now rename it
		runtime.executeSource(
			"""
				<bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
				<bx:ftp action="renamedir" connection="sftpConn" existing="test_rename_dir_sftp" new="test_renamed_dir_sftp" result="renameResult"/>
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
		    <bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
		    <bx:ftp action="existsdir" connection="sftpConn" directory="test_renamed_dir_sftp" result="existsResult"/>
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);

		IStruct existsResult = variables.getAsStruct( Key.of( "existsResult" ) );
		assertThat( existsResult.getAsBoolean( Key.of( "returnValue" ) ) ).isTrue();

		// Clean up
		runtime.executeSource(
		    """
		    <bx:ftp action="open" connection="sftpConn" username="#variables.username#" password="#variables.password#" server="#variables.server#" port="#variables.sftpPort#" secure="true" />
		    <bx:ftp action="removedir" connection="sftpConn" directory="test_renamed_dir_sftp"/>
		      """,
		    context,
		    BoxSourceType.BOXTEMPLATE
		);
	}
}
