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

import org.apache.commons.net.ftp.FTPReply;

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Represents the result of an FTP operation.
 */
public class FTPResult {

	/**
	 * The FTP connection that was used to perform the operation.
	 */
	private FTPConnection	conn;

	/**
	 * The return value of the operation.
	 */
	private Object			returnValue;

	/**
	 * Default constructor.
	 */
	public FTPResult() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param conn The FTP connection.
	 */
	public FTPResult( FTPConnection conn ) {
		super();
		this.conn = conn;
	}

	/**
	 * The return value of the operation.
	 */
	public Object getReturnValue() {
		return this.returnValue;
	}

	/**
	 * Sets the return value of the operation.
	 *
	 * @param returnValue The return value.
	 *
	 * @return This object.
	 */
	public FTPResult setReturnValue( Object returnValue ) {
		this.returnValue = returnValue;
		return this;
	}

	/**
	 * Reeturns the FTP Status code.
	 *
	 * @return The status code
	 */
	public int getStatusCode() {
		return this.conn.getStatusCode();
	}

	/**
	 * Returns the status code of the operation.
	 *
	 * @return The status code of the operation.
	 */
	public String getStatusText() {
		return this.conn.getStatusText();
	}

	/**
	 * Returns true if the operation was successful.
	 *
	 * @return True if the operation was successful.
	 */
	public Boolean isSuccessful() {
		int statusCode = getStatusCode();
		return FTPReply.isPositiveCompletion( statusCode );
	}

	/**
	 * Converts the result to a struct.
	 */
	public IStruct toStruct() {
		return Struct.of(
		    // Left for dumb ACF compat
		    "errorCode", getStatusCode(),
		    "errorText", getStatusText(),
		    // Newer way
		    "statusCode", getStatusCode(),
		    "statusText", getStatusText(),
		    "returnValue", getReturnValue(),
		    "Succeeded", isSuccessful()
		);
	}

	/**
	 * Convert to String
	 */
	@Override
	public String toString() {
		return toStruct().toString();
	}

}
