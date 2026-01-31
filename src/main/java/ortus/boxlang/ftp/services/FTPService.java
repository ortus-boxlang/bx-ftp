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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ortus.boxlang.ftp.FTPKeys;
import ortus.boxlang.ftp.IFTPConnection;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.BaseService;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;

/**
 * This service is in charge of managing all FTP connections and their lifecycles.
 */
public class FTPService extends BaseService {

	/**
	 * Concurrent map that stores all FTP connections
	 */
	private final ConcurrentMap<Key, IFTPConnection>	ftpConnections		= new ConcurrentHashMap<>();

	/**
	 * The main FTP logger
	 */
	BoxLangLogger										logger;

	/**
	 * Interception points for the service.
	 */
	private static final Key[]							INTERCEPTION_POINTS	= List.of(
	    FTPKeys.onFTPConnectionOpen,
	    FTPKeys.onFTPConnectionClose,
	    FTPKeys.afterFTPCall,
	    FTPKeys.beforeFTPCall,
	    FTPKeys.onFTPError
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
	 * @return The FTPConnection that was found or created (defaults to FTP)
	 */
	public IFTPConnection getOrBuildConnection( Key name ) {
		return getOrBuildConnection( name, false );
	}

	/**
	 * Get a connection or create a new one if it does not exist
	 *
	 * @param name   The name of the connection
	 * @param secure Whether to create an SFTP connection (true) or FTP connection (false)
	 *
	 * @return The IFTPConnection that was found or created
	 */
	public IFTPConnection getOrBuildConnection( Key name, boolean secure ) {
		return this.ftpConnections.computeIfAbsent( name, key -> {
			if ( secure ) {
				return new ortus.boxlang.ftp.SFTPConnection( name, getLogger() );
			} else {
				return new ortus.boxlang.ftp.FTPConnection( name, getLogger() );
			}
		} );
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
		IFTPConnection connection = this.ftpConnections.remove( name );
		if ( connection != null ) {
			connection.close();
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
			} catch ( BoxIOException e ) {
				// continue remove all connections, this is taking care of in the `close()` method
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
