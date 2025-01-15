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

import ortus.boxlang.runtime.scopes.Key;

public class FTPKeys {

	public static final Key	bxftp					= Key.of( "bxftp" );
	public static final Key	isDirectory				= Key.of( "isDirectory" );
	public static final Key	lastModified			= Key.of( "lastModified" );
	public static final Key	url						= Key.of( "url" );
	public static final Key	raw						= Key.of( "raw" );
	public static final Key	connection				= Key.of( "connection" );
	public static final Key	stopOnError				= Key.of( "stopOnError" );
	public static final Key	passive					= Key.of( "passive" );
	public static final Key	_new					= Key.of( "new" );
	public static final Key	remoteFile				= Key.of( "remoteFile" );
	public static final Key	localFile				= Key.of( "localFile" );
	public static final Key	directory				= Key.of( "directory" );
	public static final Key	FTPService				= Key.of( "ftpService" );

	// Events
	public static final Key	onFTPConnectionOpen		= Key.of( "onFTPConnectionOpen" );
	public static final Key	onFTPConnectionClose	= Key.of( "onFTPConnectionClose" );
}
