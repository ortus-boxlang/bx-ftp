/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.ftp;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.ModuleService;

/**
 * Use this as a base integration test for your non web-support package
 * modules. If you want web based testing, use the BaseWebIntegrationTest
 */
@Testcontainers
public abstract class BaseIntegrationTest {

	protected static BoxRuntime				runtime;
	protected static ModuleService			moduleService;
	protected static ModuleRecord			moduleRecord;
	protected static Key					result			= new Key( "result" );
	protected static Key					moduleName		= FTPKeys.bxftp;
	protected ScriptingRequestBoxContext	context;
	protected IScope						variables;

	// FTP server configuration
	protected static String					ftpHost;
	protected static int					ftpPort;
	protected static final String			FTP_USERNAME	= "test_user";
	protected static final String			FTP_PASSWORD	= "testpass";

	@Container
	static DockerComposeContainer<?>		environment		= new DockerComposeContainer<>( new File( "src/test/resources/docker-compose.yml" ) )
	    .withBuild( true ) // run docker compose build
	    .withExposedService( "ftp", 21, Wait.forListeningPort() )
	    .withExposedService( "ftp", 22, Wait.forListeningPort() )
	    // expose a few passive ports you'll actually use (range is fine to repeat)
	    .withExposedService( "ftp", 10000, Wait.forListeningPort() )
	    .withLocalCompose( true ); // use your local docker compose binary

	@BeforeAll
	public static void setup() {
		// Start the FTP container and get connection details
		ftpHost	= environment.getServiceHost( "ftp", 21 );
		ftpPort	= environment.getServicePort( "ftp", 21 );

		System.out.println( "FTP Server started at " + ftpHost + ":" + ftpPort );

		runtime			= BoxRuntime.getInstance( true, Path.of( "src/test/resources/boxlang.json" ).toString() );
		moduleService	= runtime.getModuleService();
		// Load the module
		loadModule( runtime.getRuntimeContext() );
	}

	@BeforeEach
	public void setupEach() {
		// Create the mock contexts
		context		= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );

		// Set FTP connection properties from Testcontainers
		System.setProperty( "BOXLANG_FTP_USERNAME", FTP_USERNAME );
		System.setProperty( "BOXLANG_FTP_PASSWORD", FTP_PASSWORD );
		System.setProperty( "BOXLANG_FTP_SERVER", ftpHost );
		System.setProperty( "BOXLANG_FTP_PORT", String.valueOf( ftpPort ) );
	}

	protected static void loadModule( IBoxContext context ) {
		if ( !runtime.getModuleService().hasModule( moduleName ) ) {
			System.out.println( "Loading module: " + moduleName );
			String physicalPath = Paths.get( "./build/module" ).toAbsolutePath().toString();
			moduleRecord = new ModuleRecord( physicalPath );

			moduleService.getRegistry().put( moduleName, moduleRecord );

			moduleRecord
			    .loadDescriptor( context )
			    .register( context )
			    .activate( context );
		} else {
			System.out.println( "Module already loaded: " + moduleName );
		}
	}

}
