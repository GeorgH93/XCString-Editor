# Test Fixtures

This directory contains sample .xcstrings files used for testing various parsing scenarios.

## Files

### `sample.xcstrings`
Basic .xcstrings file with simple string entries and localizations.

### `sample-ai-demo.xcstrings` 
Demo file showcasing AI translation features with multiple languages and translation states.

### `test-variations.xcstrings`
Complex file demonstrating:
- Plural variations (one/other)
- Device variations (iPhone/iPad/other)
- Multiple localization states (new, translated, needs_review)
- Comments and metadata

### `test-empty-localizations.xcstrings`
Test file with strings that have empty localization objects, used to test edge cases in parsing.

### `test-missing-localizations.xcstrings`
Test file with strings that completely lack localization keys, used to test malformed input handling.

## Usage in Tests

These fixtures are used by:
- `XCStringParsingTest.php` - Tests parsing logic and edge cases
- File management tests - Sample content for CRUD operations

Each fixture is designed to test specific parsing scenarios and ensure robust handling of various .xcstrings file formats.