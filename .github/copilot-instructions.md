# BoxLang FTP Module - AI Coding Agent Instructions

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

# Run tests with Testcontainers (automatic FTP server)
./gradlew test

# Create module distribution
./gradlew createModuleStructure zipModuleStructure
```

### Testing Architecture
- **Base Test**: `BaseIntegrationTest` loads module into BoxRuntime with `boxlang.json` config
- **Integration Tests**: Use Testcontainers with Docker Compose for real FTP server
- **Test FTP Server**: Ubuntu + vsftpd, credentials: `test_user/testpass`
- **Connection Setup**: Test base automatically configures FTP properties from container

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
- **Development**: `Docker-Compose.yml` for manual testing (ports 2221, 2222)
- **Testing**: Testcontainers uses separate Docker Compose with dynamic ports
- **FTP Server**: vsftpd in Ubuntu container with test user and files

## Critical Integration Points

- **BoxRuntime**: Module loaded via `ModuleRecord` with descriptor and activation
- **Global Service**: FTPService accessible via `runtime.getGlobalService(FTPKeys.FTPService)`
- **Connection Tracking**: Named connections stored in service, accessible across component calls
- **Apache Commons Net**: Core FTP operations via `org.apache.commons.net.ftp.FTPClient`

## Development Workflow

1. **Module Changes**: Edit Java/BoxLang → run `./gradlew build`
2. **Test Locally**: Use `./gradlew test` (starts containers automatically)
3. **Manual Testing**: Use root `Docker-Compose.yml` for persistent FTP server
4. **Distribution**: `./gradlew zipModuleStructure` creates installable ZIP

## Common Issues

- **Port Conflicts**: Manual Docker vs Testcontainers - stop manual containers before tests
- **Module Loading**: Tests require `boxlang.json` config in `src/test/resources/`
- **Connection State**: FTP connections are stateful - ensure proper cleanup in error scenarios