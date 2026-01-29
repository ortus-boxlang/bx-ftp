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
package ortus.boxlang.ftp;

import java.io.File;
import java.time.Duration;

import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Base class for FTP and SFTP connections.
 * <p>
 * This abstract class provides common functionality for both FTP and SFTP connections,
 * including connection state management, configuration defaults, and file validation.
 * </p>
 */
public abstract class BaseFTPConnection implements IFTPConnection {

	/**
	 * --------------------------------------------------------------------------
	 * Defaults
	 * --------------------------------------------------------------------------
	 */

	public static final int			DEFAULT_PORT			= 21;
	public static final int			DEFAULT_SFTP_PORT		= 22;
	public static final boolean		DEFAULT_PASSIVE			= false;
	public static final String		DEFAULT_USERNAME		= "anonymous";
	public static final String		DEFAULT_PASSWORD		= "anonymous";
	public static final boolean		DEFAULT_STOP_ON_ERROR	= true;
	public static final int			DEFAULT_PROXY_SERVER_PORT	= 1080;
	// In Seconds
	public static final Duration	DEFAULT_TIMEOUT			= Duration.ofSeconds( 30 );

	/**
	 * --------------------------------------------------------------------------
	 * Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * If true, an exception will be thrown if an error occurs. If false, the
	 * error will be ignored.
	 */
	protected boolean			stopOnError	= DEFAULT_STOP_ON_ERROR;

	/**
	 * The name of the connection.
	 */
	protected Key				name;

	/**
	 * The username for the connection.
	 */
	protected String			username;

	/**
	 * The BoxLang logger to use
	 */
	protected BoxLangLogger		logger;

	/**
	 * The last status code from the server
	 */
	protected int				statusCode	= 0;

	/**
	 * The last status text from the server
	 */
	protected String			statusText	= "";

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Build a Connection
	 *
	 * @param name   The name of the connection
	 * @param logger The BoxLang logger to use
	 */
	public BaseFTPConnection( Key name, BoxLangLogger logger ) {
		this.name	= name;
		this.logger	= logger;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Common Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IFTPConnection setStopOnError( boolean stopOnError ) {
		this.stopOnError = stopOnError;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getStatusCode() {
		return this.statusCode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getStatusText() {
		return this.statusText;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Key getName() {
		return this.name;
	}

	/**
	 * A string representation of the connection.
	 */
	@Override
	public String toString() {
		return getMetadata().toString();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Protected Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Ensure that the local file exists, is a file, and can be read
	 *
	 * @param localFile The local file to check
	 *
	 * @return The validated file
	 *
	 * @throws BoxRuntimeException If the local file does not exist, not a file, or cannot be read.
	 */
	protected File ensureLocalFile( File localFile ) {
		// validations
		if ( !localFile.exists() ) {
			throw new BoxRuntimeException( "Error: Local file does not exist: " + localFile );
		}
		if ( !localFile.isFile() ) {
			throw new BoxRuntimeException( "Error: Path is not a file: " + localFile );
		}
		if ( !localFile.canRead() ) {
			throw new BoxRuntimeException( "Error: Cannot read the file: " + localFile );
		}
		return localFile;
	}

	/**
	 * Update the status code and text from the connection
	 *
	 * @param code The status code
	 * @param text The status text
	 */
	protected void updateStatus( int code, String text ) {
		this.statusCode	= code;
		this.statusText	= text;
	}
}
