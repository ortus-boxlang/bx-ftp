# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

### Added

- SFTP Support
- Upgraded gradle
- DotEnv support for tests
- BoxLang 1.9 support

### Fixed

- Updated dependencies
- Fixed Dockerfile due to issues when updating the package scripts for installing the ssh server

## [1.4.0] - 2025-11-21

### Added

- New GitHub Actions for building and testing
- Updated Gradle to latest
- Updated Java Dependency versions to latests

### Fixed

- Updated box.json type to boxlang-modules so it installs correctly in BoxLang

## [1.3.0] - 2025-10-14

### Added

- Added new `onFTPError` interception point for handling FTP errors globally
- Added supplier lambdas for events and performance
- Updated to use nonConcurrent Structs for better performance for announcements
- Added AI instructions for better context
- Updated all GitHub actions to latest versions
- Updated Gradle to latest
- Updated Java Dependency versions to latests
- More tests for local passive mode connections

### Fixed

- before and after FTP Calls where not being registered correctly

## [1.2.0] - 2025-08-05

### Updated

- Bump commons-net:commons-net from 3.11.1 to 3.12.0 #12
- New github actions for building and testing #13
- Changed dependabot to monthly
- Updated `tests.yml` to latest versions

* * *

## [1.1.0] - 2025-02-17

### Fixed

- Location of the zip missing `bx-ftp` module
- Junit regresions for testing

### Added

- Gradle plugin upgrades

## [1.0.0] - 2025-01-17

- First iteration of this module

[unreleased]: https://github.com/ortus-boxlang/bx-ftp/compare/v1.4.0...HEAD
[1.4.0]: https://github.com/ortus-boxlang/bx-ftp/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/ortus-boxlang/bx-ftp/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/ortus-boxlang/bx-ftp/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ortus-boxlang/bx-ftp/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/ortus-boxlang/bx-ftp/compare/136f3680c7f92b785733421a2a92c3db2d91d404...v1.0.0
