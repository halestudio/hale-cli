# Change Log
All notable changes to this project will be documented in this file.
See the [change log guidelines](http://keepachangelog.com/) for information on how to structure the file.

## [Unreleased]

## [3.2.0]

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

[Unreleased]: https://github.com/halestudio/hale/compare/v3.2.0...HEAD
[3.2.0]: https://github.com/halestudio/hale/compare/v3.1.0...v3.2.0
[3.1.0]: https://github.com/halestudio/hale/compare/v3.0.0...v3.1.0
