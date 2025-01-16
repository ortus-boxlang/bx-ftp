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

### Available Actions

The available actions for the `bx:ftp` component are:

#### Connection Actions

- `open` - Open a connection to an FTP server
- `close` - Close a connection to an FTP server

#### Directory Actions

- `changeDir` - Change the current directory
- `createDir` - Create a directory
- `existsDir` - Check if a directory exists
- `getCurrentDir` - Get the current directory
- `listDir` - List the contents of a directory
- `removeDir` - Remove a directory
- `renameDir` - Rename a directory

#### File Actions

- `removeFile` - Remove a file
- `renameFile` - Rename a file
- `putfile` - Upload a file
- `getfile` - Download a file
- `existsFile` - Check if a file exists

## CFML Compatibility

This module will require the `bx-cfml-compat` module if you want it to work like Adobe/Lucee in your CFML applications.  That's it!

## Ortus Sponsors

BoxLang is a professional open-source project and it is completely funded by the [community](https://patreon.com/ortussolutions) and [Ortus Solutions, Corp](https://www.ortussolutions.com).  Ortus Patreons get many benefits like a cfcasts account, a FORGEBOX Pro account and so much more.  If you are interested in becoming a sponsor, please visit our patronage page: [https://patreon.com/ortussolutions](https://patreon.com/ortussolutions)

### THE DAILY BREAD

 > "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12
