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
package ortus.boxlang.ftp.services;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ortus.boxlang.ftp.FTPConnection;
import ortus.boxlang.ftp.FTPKeys;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.BaseService;

/**
 * This service is in charge of managing all FTP connections and their lifecycles.
 */
public class FTPService extends BaseService {

	/**
	 * Concurrent map that stores all FTP connections
	 */
	private final ConcurrentMap<Key, FTPConnection>	ftpConnections		= new ConcurrentHashMap<>();

	/**
	 * The main FTP logger
	 */
	BoxLangLogger									logger;

	/**
	 * Interception points for the service.
	 */
	private static final Key[]						INTERCEPTION_POINTS	= List.of(
	    // I NEED YOU TO CREATE events for onFTPConnection, onFTPDisconnection, onFTPCommand, etc.
	    FTPKeys.onFTPConnectionOpen,
	    FTPKeys.onFTPConnectionClose,
	    FTPKeys.postFTPCall
	).toArray( new Key[ 0 ] );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * public no-arg constructor for the ServiceProvider
	 */
	public FTPService() {
		this( BoxRuntime.getInstance() );
	}

	/**
	 * Constructor
	 *
	 * @param runtime The BoxRuntime
	 */
	public FTPService( BoxRuntime runtime ) {
		super( runtime, FTPKeys.FTPService );
		getLogger().trace( "+ FTP Service built" );
		runtime.getInterceptorService().registerInterceptionPoint( INTERCEPTION_POINTS );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public void onConfigurationLoad() {
		// Not used by the service, since those are only for core services
	}

	@Override
	public void onShutdown( Boolean force ) {
		getLogger().info( "+ FTP Service shutdown requested" );
		shutdownAllConnections();
	}

	@Override
	public void onStartup() {
		getLogger().info( "+ FTP Service started" );
	}

	/**
	 * ------------------------------------------------------------------------------
	 * Connection Methods
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * How many connections do we store
	 *
	 * @return The number of connections
	 */
	public int getConnectionCount() {
		return this.ftpConnections.size();
	}

	/**
	 * Get a connection or create a new one if it does not exist
	 *
	 * @param name The name of the connection
	 *
	 * @return The FTPConnection that was found or created
	 */
	public FTPConnection getOrBuildConnection( Key name ) {
		return this.ftpConnections.computeIfAbsent( name, key -> new FTPConnection( name, getLogger() ) );
	}

	/**
	 * Verifies if the named connection exists
	 *
	 * @param name The key of the connection
	 *
	 * @return True if the connection exists, false if it does not
	 */
	public boolean hasConnection( Key name ) {
		return this.ftpConnections.containsKey( name );
	}

	/**
	 * Remove a connection by key, this will shutdown the connection first
	 *
	 * @param name The key of the connection
	 *
	 * @return True if the connection was removed, false if it was not found
	 */
	public boolean removeConnection( Key name ) {
		FTPConnection connection = this.ftpConnections.remove( name );
		if ( connection != null ) {
			try {
				connection.close();
			} catch ( IOException e ) {
				getLogger().error( "Error closing connection: " + name.getName(), e );
			}
			return true;
		}
		return false;
	}

	/**
	 * Shutdown and remove all connections
	 */
	public void shutdownAllConnections() {
		this.ftpConnections.forEach( ( key, connection ) -> {
			try {
				connection.close();
			} catch ( IOException e ) {
				getLogger().error( "Error closing connection: " + key.getName(), e );
			}
		} );
		this.ftpConnections.clear();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helper methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the ORM logger that logs to the "orm" category.
	 */
	public BoxLangLogger getLogger() {
		if ( this.logger == null ) {
			synchronized ( FTPService.class ) {
				if ( this.logger == null ) {
					this.logger = runtime.getLoggingService().getLogger( "ftp" );
				}
			}
		}
		return this.logger;
	}

}
