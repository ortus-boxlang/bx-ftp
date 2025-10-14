# BoxLang FTP Module - AI Coding Agent Instructions

## Documentation Resources

For comprehensive BoxLang documentation and API references, use the **BoxLang MCP Server**:
- **MCP Server URL**: https://boxlang.ortusbooks.com/~gitbook/mcp
- **Usage**: Access BoxLang language features, component APIs, module development patterns, and integration guides
- **Search Documentation**: Use the MCP tools to find specific BoxLang functionality, syntax, and best practices

## Architecture Overview

This is a **BoxLang module** that provides FTP functionality through a service-oriented architecture:

- **`FTPService`**: Global runtime service managing connection lifecycle and state (`src/main/java/ortus/boxlang/ftp/services/`)
- **`FTP` Component**: BoxLang component exposing FTP operations (`src/main/java/ortus/boxlang/ftp/components/FTP.java`)
- **Connection Management**: Named connections tracked in concurrent map, automatic cleanup on module shutdown
- **Module Registration**: Uses ServiceLoader pattern with `META-INF/services` auto-generation

## Key Development Patterns

### Module Structure
- **Java Source**: `src/main/java/ortus/boxlang/ftp/` - Core FTP logic
- **BoxLang Config**: `src/main/bx/ModuleConfig.bx` - Module descriptor and lifecycle
- **Module Build**: `build/module/` - Generated module structure for distribution
- **Service Registration**: Uses `@BoxComponent` and implements service interfaces

### Essential Commands
```bash
# Download BoxLang runtime dependency
./gradlew downloadBoxLang

# Build and package module
./gradlew build

# Start FTP server for testing
docker-compose up -d --build

# Run tests (requires FTP server running)
./gradlew test

# Run specific test
./gradlew test --tests "*FTPTest.testListFiles"

# Stop FTP server
docker-compose down

# Create module distribution
./gradlew createModuleStructure zipModuleStructure
```

### Testing Architecture
- **Base Test**: `BaseIntegrationTest` loads module into BoxRuntime with `boxlang.json` config
- **Manual Docker Setup**: Uses `Docker-Compose.yml` for consistent FTP server environment
- **Test FTP Server**: Ubuntu + vsftpd, credentials: `test_user/testpass`
- **FTP Server Ports**: Control port 2221, data port 2220, passive ports 10000-10010
- **Connection Setup**: Test configuration loaded from `.env` file in test resources
- **Passive Mode**: Critical vsftpd configuration for data connections (see FTP Server Configuration section)

## BoxLang Integration Patterns

### Component Development
```java
@BoxComponent(allowsBody = false)
public class FTP extends Component {
    // Action-based component with validate() and process() methods
    // Uses FTPService for connection management
    // Returns FTPResult wrapped in BoxLang struct
}
```

### Service Pattern
```java
public class FTPService extends BaseService {
    // Singleton service registered as "ftpService" globally
    // Manages ConcurrentHashMap<Key, FTPConnection>
    // Implements interception points for lifecycle events
}
```

### Key Constants
- All BoxLang keys defined in `FTPKeys.java` using `Key.of("string")`
- Service name: `FTPKeys.FTPService`
- Module name: `FTPKeys.bxftp`

## Build System Specifics

### Gradle Configuration
- **ServiceLoader**: Auto-generates `META-INF/services` files for BoxLang runtime discovery
- **Shadow JAR**: Creates fat JAR with dependencies for module distribution
- **Token Replacement**: `@build.version@` tokens replaced in module files during build
- **Spotless**: Eclipse formatter with `.ortus-java-style.xml`

### Module Packaging
1. Compile Java → `build/libs/{module}-{version}.jar`
2. Copy BoxLang files from `src/main/bx/` with token replacement
3. Create `build/module/` structure with JAR in `libs/` subdirectory
4. Package as ZIP for distribution

### Docker Integration
- **Development**: `Docker-Compose.yml` for manual testing (ports 2221, 2220, 10000-10010)
- **FTP Server**: vsftpd in Ubuntu container with test user and files
- **Test Configuration**: `.env` file in `src/test/resources/` with FTP connection properties
- **Manual Testing**: Use root `Docker-Compose.yml` for persistent FTP server

### FTP Server Configuration
Critical `vsftpd.conf` settings for proper operation:
```conf
# Essential passive mode configuration
pasv_enable=YES
pasv_min_port=10000
pasv_max_port=10010
pasv_address=127.0.0.1

# User authentication
local_enable=YES
write_enable=YES
chroot_local_user=YES
allow_writeable_chroot=YES
```

**Important**: Passive mode configuration is essential for data connections. Missing these settings will cause "connection refused" errors during file operations even if control connection succeeds.

## Critical Integration Points

- **BoxRuntime**: Module loaded via `ModuleRecord` with descriptor and activation
- **Global Service**: FTPService accessible via `runtime.getGlobalService(FTPKeys.FTPService)`
- **Connection Tracking**: Named connections stored in service, accessible across component calls
- **Apache Commons Net**: Core FTP operations via `org.apache.commons.net.ftp.FTPClient`

## Development Workflow

1. **Module Changes**: Edit Java/BoxLang → run `./gradlew build`
2. **Test Locally**: Start FTP server with `docker-compose up -d --build` → run `./gradlew test`
3. **Manual Testing**: Use root `Docker-Compose.yml` for persistent FTP server
4. **Distribution**: `./gradlew zipModuleStructure` creates installable ZIP

## Common Issues & Solutions

### Local Test Failures ("Connection Refused")
- **Symptom**: Tests pass in CI but fail locally with FTP data connection errors
- **Root Cause**: Missing passive mode configuration in `vsftpd.conf`
- **Solution**: Ensure `resources/vsftpd.conf` contains proper passive mode settings (see FTP Server Configuration)
- **Debugging**: Use `nc -v localhost 2221` to test control connection, check Docker port mappings

### Container Management
- **Port Conflicts**: Stop existing containers before tests: `docker-compose down`
- **Clean Rebuild**: Use `docker-compose up -d --build --force-recreate` for configuration changes
- **Test Isolation**: Manual Docker containers should be stopped before running `./gradlew test`

### Module Development
- **Module Loading**: Tests require `boxlang.json` config in `src/test/resources/`
- **Connection State**: FTP connections are stateful - ensure proper cleanup in error scenarios
- **Service Registration**: Uses `@BoxComponent` annotation for automatic discovery