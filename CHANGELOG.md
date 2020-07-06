# Change Log
All notable changes to this project will be documented in this file.
See the [change log guidelines](http://keepachangelog.com/) for information on how to structure the file.

## [4.0.0]

## Added

- GeoPackage support

## Changed

- Use OpenJDK Docker base image
- Upgrade hale dependencies to 4.0

## [3.5.0]

### Added

- Added command for exporting projects
- Extent project export command with hale connect project export
- Added support for specifying settings for project writers

### Changed

- Upgrade hale dependencies to 3.5
- Replaced Saxon dependency with official Saxon HE artifact

### Fixed

- Provide project information to advisors when saving a project
- Suppress irrelevant error messages caused by Eclipse client platform update

## [3.4.0]

### Added

- Alignment merge command
- PostNAS matching project command
- MSSQL database support
- Rewrite command for schemas

### Changed

- Upgrade hale dependencies to 3.4.0
- When filtering cells by type, drop partial matches if Join focus is no match

### Fixed

- Fixed Docker image build problems

## [3.3.2]

### Changed

- Upgrade hale dependencies to 3.3.2

## [3.3.1]

### Fixed

- When filtering an alignment, create complete copy if base alignment is not used

### Added

- Added `RewriteCommand` for rewriting a source

### Changed

- Upgrade hale dependencies to 3.3.1

## [3.3.0]

### Fixed

- Work around command line length restriction issue on Windows

### Added

- Added dependency for Schematron validation
- Added dependency for collector functions

### Changed

- Upgrade hale dependencies to 3.3

## [3.2.0]

### Changed

- Upgrade hale dependencies to 3.2

## [3.1.0]

### Added

- Added command to split a XML/GML data set, respecting local XLink references and leaving them intact (`hale data split`)
- Added experimental commands for migrating hale projects to a different schema (`hale project migrate`)

## 3.0.0

Initial release based on hale studio 3.0.0.

### Added

- Transformation command from hale studio (`hale transform`)
- Command for launching a Groovy shell (`hale groovysh`)
- Command to list hale projects (`hale project list`)
- Command to generate HTML mapping documentation from hale projects (`hale project alignment export-doc`)
- Command to generate mapping tables (Excel) from hale projects (`hale project alignment export-table`)
- Command to generate JSON representations of hale alignments (`hale project alignment export-json`)
- Experimental command to filter to create a copy of a hale project with a filtered alignment (`hale project alignment filter`)

[4.0.0]: https://github.com/halestudio/hale-cli/compare/v3.5.0...v4.0.0
[3.5.0]: https://github.com/halestudio/hale-cli/compare/v3.4.0...v3.5.0
[3.4.0]: https://github.com/halestudio/hale-cli/compare/v3.3.2...v3.4.0
[3.3.2]: https://github.com/halestudio/hale-cli/compare/v3.3.1...v3.3.2
[3.3.1]: https://github.com/halestudio/hale-cli/compare/v3.3.0...v3.3.1
[3.3.0]: https://github.com/halestudio/hale-cli/compare/v3.2.0...v3.3.0
[3.2.0]: https://github.com/halestudio/hale-cli/compare/v3.1.0...v3.2.0
[3.1.0]: https://github.com/halestudio/hale-cli/compare/v3.0.0...v3.1.0
