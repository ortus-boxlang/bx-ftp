# ⚡︎ BoxLang Module: FTP ⚡︎

```
|:------------------------------------------------------:|
| ⚡︎ B o x L a n g ⚡︎
| Dynamic : Modular : Productive
|:------------------------------------------------------:|
```

<blockquote>
	Copyright Since 2023 by Ortus Solutions, Corp
	<br>
	<a href="https://www.boxlang.io">www.boxlang.io</a> |
	<a href="https://www.ortussolutions.com">www.ortussolutions.com</a>
</blockquote>

<p>&nbsp;</p>

## Welcome to the BoxLang FTP Module

This module will allow your BoxLang applications to interact with FTP servers.

If there are any issues, please report them to the [BoxLang JIRA](https://ortussolutions.atlassian.net/browse/BL/issues) or the [Module Issues](https://github.com/ortus-boxlang/bx-compat-cfml/issues) repository.

## Settings

As of this version, there are no configurable settings for this module.

## Usage

You will use the `ftp` component in your BoxLang application by leveraging the `bx:ftp` component call, whether you are in script or in the templating language.

```java
// Connect to an FTP server
bx:ftp action="open" connection="myConnection" server="ftp.example.com" username="myuser" password="mypass";

// List the contents of a directory
bx:ftp action="listdir" connection="myConnection" directory="/" name="results";

// Download a file
bx:ftp action="getfile" connection="myConnection" remoteFile="/path/to/remote/file.txt" localFile="/path/to/local/file.txt";

// Upload a file
bx:ftp action="putfile" connection="myConnection" localFile="/path/to/local/file.txt" remoteFile="/path/to/remote/file.txt";

// Close the connection
bx:ftp action="close" connection="myConnection";
```

Please note that the `connection` attribute is required for all actions, as it is the reference to the connection that you have opened and BoxLang will track for you in the `FTPService` service that's also registered in the runtime as a global service.

## Available Actions

All actions can use a `result` attribute to store the result of the action in a variable, if not a variable called `bxftp` will be used (In CFML compat mode it will be `cftp`).

The result will be a struct with the following keys:

- `statusCode` - The status code of the action (integer)
- `statusText` - The status text of the action (string)
- `returnValue` - The return value of the action (if any)
- `succeeded` - Whether the action was successful (boolean)

### Connection Actions

### `open`

Open a connection to an FTP server.  The available attributes are:

- `connection` - The name of the connection to use and track in the `FTPService` service
- `server` - The server ip or hostname to connect to
- `port` - The port to connect to (default is 21)
- `username` - The username to use for authentication
- `password` - The password to use for authentication
- `timeout` - The timeout for the connection (default is 30 seconds)
- `secure` - Whether to use a secure connection (default is `false`)
- `passive` - Whether to use passive mode (default is `true`)

### `close`

Close a connection to an FTP server. The available attributes are:

- `connection` - The name of the connection to close

### Directory Actions

## `changeDir`

Change the current directory.  The available attributes are:

- `directory` - The directory to change to

## `createDir`

Create a directory. The available attributes are:

- `directory` - The directory to create

## `existsDir`

Check if a directory exists. The available attributes are:

- `directory` - The directory to check

## `getCurrentDir`

Get the current working directory of the connection.  No attributes are required.

## `listDir`

List the contents of a directory. The available attributes are:

- `directory` - The directory to list
- `name` - The name of the variable to store the results in as a query

## `removeDir`

Remove a directory. The available attributes are:

- `directory` - The directory to remove

## `renameDir`

Rename a directory.  The available attributes are:

- `existing` - The directory to rename
- `new` - The new name of the directory

### File Actions

## `removeFile`

Remove a file. The available attributes are:

- `item` - The file to remove

## `renameFile`

Rename a file. The available attributes are:

- `existing` - The directory to rename
- `new` - The new name of the directory

## `putfile`

Upload a file. The available attributes are:

- `localFile` - The local file to upload
- `remoteFile` - The remote file to upload to

## `getfile`

Download a file. The available attributes are:

- `localFile` - The local file to download to
- `remoteFile` - The remote file to download

## `existsFile`

Check if a file exists. The available attributes are:

- `remoteFile` - The file to check for existence

## CFML Compatibility

This module will require the `bx-cfml-compat` module if you want it to work like Adobe/Lucee in your CFML applications.  Enjoy!

## Ortus Sponsors

BoxLang is a professional open-source project and it is completely funded by the [community](https://patreon.com/ortussolutions) and [Ortus Solutions, Corp](https://www.ortussolutions.com).  Ortus Patreons get many benefits like a cfcasts account, a FORGEBOX Pro account and so much more.  If you are interested in becoming a sponsor, please visit our patronage page: [https://patreon.com/ortussolutions](https://patreon.com/ortussolutions)

### THE DAILY BREAD

 > "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12
