<?php

require_once __DIR__ . '/TestRunner.php';

// Include only the parsing functions we need
function parseXcString($content) {
    $json = json_decode($content, true);
    if (!$json) {
        throw new Exception('Invalid xcstring format');
    }
    return $json;
}

function ensureObjectsStayObjects($data) {
    if (is_array($data)) {
        // If it's an empty array, convert to stdClass to preserve {} format
        if (empty($data)) {
            return new stdClass();
        }
        
        // If it's an associative array, convert to stdClass
        if (array_keys($data) !== range(0, count($data) - 1)) {
            $obj = new stdClass();
            foreach ($data as $key => $value) {
                $obj->$key = ensureObjectsStayObjects($value);
            }
            return $obj;
        }
        
        // It's a numeric array, recurse into elements
        return array_map('ensureObjectsStayObjects', $data);
    }
    
    return $data;
}

class XCStringParsingTest extends TestCase {
    
    public function __construct() {
        parent::__construct('XCString Parsing Tests');
    }
    
    public function testParseSampleXCString() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample.xcstrings');
        $parsed = parseXcString($content);
        
        $this->assertTrue(is_array($parsed), 'Parsed content should be an array');
        $this->assertArrayHasKey('sourceLanguage', $parsed, 'Should have sourceLanguage');
        $this->assertArrayHasKey('strings', $parsed, 'Should have strings');
        $this->assertArrayHasKey('version', $parsed, 'Should have version');
        
        $this->assertEquals('en', $parsed['sourceLanguage'], 'Source language should be en');
        $this->assertEquals('1.0', $parsed['version'], 'Version should be 1.0');
    }
    
    public function testParseXCStringWithVariations() {
        $content = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');
        $parsed = parseXcString($content);
        
        $this->assertArrayHasKey('%d minutes', $parsed['strings'], 'Should have plural minutes key');
        $this->assertArrayHasKey('simple_key', $parsed['strings'], 'Should have simple key');
        $this->assertArrayHasKey('%@ items', $parsed['strings'], 'Should have device variation key');
        
        // Test plural variations
        $minutesString = $parsed['strings']['%d minutes'];
        $this->assertArrayHasKey('localizations', $minutesString, 'Minutes string should have localizations');
        $this->assertArrayHasKey('en', $minutesString['localizations'], 'Should have English localization');
        $this->assertArrayHasKey('de', $minutesString['localizations'], 'Should have German localization');
        
        // Test German plural variations
        $germanMinutes = $minutesString['localizations']['de'];
        $this->assertArrayHasKey('variations', $germanMinutes, 'German should have variations');
        $this->assertArrayHasKey('plural', $germanMinutes['variations'], 'Should have plural variations');
        $this->assertArrayHasKey('one', $germanMinutes['variations']['plural'], 'Should have singular form');
        $this->assertArrayHasKey('other', $germanMinutes['variations']['plural'], 'Should have plural form');
        
        // Test device variations
        $itemsString = $parsed['strings']['%@ items'];
        $englishItems = $itemsString['localizations']['en'];
        $this->assertArrayHasKey('variations', $englishItems, 'Items should have variations');
        $this->assertArrayHasKey('device', $englishItems['variations'], 'Should have device variations');
        $this->assertArrayHasKey('iphone', $englishItems['variations']['device'], 'Should have iPhone variant');
        $this->assertArrayHasKey('ipad', $englishItems['variations']['device'], 'Should have iPad variant');
    }
    
    public function testParseEmptyLocalizations() {
        $content = file_get_contents(__DIR__ . '/fixtures/test-empty-localizations.xcstrings');
        $parsed = parseXcString($content);
        
        $this->assertArrayHasKey('test_key_with_empty_localizations', $parsed['strings']);
        $this->assertArrayHasKey('test_key_with_some_localizations', $parsed['strings']);
        
        $emptyString = $parsed['strings']['test_key_with_empty_localizations'];
        $this->assertArrayHasKey('localizations', $emptyString, 'Should have localizations key');
        $this->assertTrue(empty($emptyString['localizations']), 'Localizations should be empty');
        $this->assertEquals('This string has no localizations', $emptyString['comment']);
        
        $someString = $parsed['strings']['test_key_with_some_localizations'];
        $this->assertArrayHasKey('en', $someString['localizations'], 'Should have English localization');
        $this->assertEquals('Hello', $someString['localizations']['en']['stringUnit']['value']);
    }
    
    public function testParseMissingLocalizations() {
        $content = file_get_contents(__DIR__ . '/fixtures/test-missing-localizations.xcstrings');
        $parsed = parseXcString($content);
        
        $this->assertArrayHasKey('test_key_with_missing_localizations', $parsed['strings']);
        $missingString = $parsed['strings']['test_key_with_missing_localizations'];
        
        // Should not have localizations key at all
        $this->assertFalse(isset($missingString['localizations']), 'Should not have localizations key');
    }
    
    public function testParseInvalidJSON() {
        $this->expectException('Exception', function() {
            parseXcString('invalid json {');
        });
    }
    
    public function testParseAIDemoFile() {
        $content = file_get_contents(__DIR__ . '/fixtures/sample-ai-demo.xcstrings');
        $parsed = parseXcString($content);
        
        $this->assertTrue(is_array($parsed), 'AI demo should parse correctly');
        $this->assertArrayHasKey('strings', $parsed, 'Should have strings');
        
        // Test that we have some expected keys from the AI demo
        $stringKeys = array_keys($parsed['strings']);
        $this->assertTrue(count($stringKeys) > 0, 'Should have at least one string');
    }
    
    public function testEnsureObjectsStayObjects() {
        // Test the helper function that preserves object structure
        $emptyArray = [];
        $result = ensureObjectsStayObjects($emptyArray);
        $this->assertInstanceOf('stdClass', $result, 'Empty array should become stdClass');
        
        $assocArray = ['key' => 'value'];
        $result = ensureObjectsStayObjects($assocArray);
        $this->assertInstanceOf('stdClass', $result, 'Associative array should become stdClass');
        $this->assertEquals('value', $result->key, 'Object should preserve values');
        
        $numericArray = [1, 2, 3];
        $result = ensureObjectsStayObjects($numericArray);
        $this->assertTrue(is_array($result), 'Numeric array should stay array');
        $this->assertEquals(3, count($result), 'Array length should be preserved');
    }
    
    public function testStringStatesAndComments() {
        $content = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');
        $parsed = parseXcString($content);
        
        // Test that string states are preserved
        $minutesString = $parsed['strings']['%d minutes'];
        $germanOne = $minutesString['localizations']['de']['variations']['plural']['one']['stringUnit'];
        $this->assertEquals('needs_review', $germanOne['state'], 'State should be preserved');
        
        $englishOther = $minutesString['localizations']['en']['variations']['plural']['other']['stringUnit'];
        $this->assertEquals('new', $englishOther['state'], 'New state should be preserved');
        
        // Test comments
        $this->assertEquals('Duration in minutes', $minutesString['comment'], 'Comment should be preserved');
        
        $simpleString = $parsed['strings']['simple_key'];
        $this->assertEquals('A simple string without variations', $simpleString['comment'], 'Simple comment should be preserved');
    }
    
    public function testStringValues() {
        $content = file_get_contents(__DIR__ . '/fixtures/test-variations.xcstrings');
        $parsed = parseXcString($content);
        
        // Test simple string value
        $simpleString = $parsed['strings']['simple_key'];
        $this->assertEquals('Hello World', $simpleString['localizations']['en']['stringUnit']['value']);
        $this->assertEquals('Hallo Welt', $simpleString['localizations']['de']['stringUnit']['value']);
        
        // Test variation values
        $minutesString = $parsed['strings']['%d minutes'];
        $englishOne = $minutesString['localizations']['en']['variations']['plural']['one']['stringUnit']['value'];
        $this->assertEquals('%d minute', $englishOne, 'Singular form should be correct');
        
        $germanOther = $minutesString['localizations']['de']['variations']['plural']['other']['stringUnit']['value'];
        $this->assertEquals('%d Minuten', $germanOther, 'German plural should be correct');
        
        // Test device variations
        $itemsString = $parsed['strings']['%@ items'];
        $ipadValue = $itemsString['localizations']['en']['variations']['device']['ipad']['stringUnit']['value'];
        $this->assertEquals('%@ items on iPad', $ipadValue, 'iPad variant should be different');
    }
    
    public function testParseMultilineStrings() {
        $content = file_get_contents(__DIR__ . '/fixtures/test-multiline.xcstrings');
        $parsed = parseXcString($content);
        
        $this->assertArrayHasKey('multiline_test', $parsed['strings'], 'Should have multiline test key');
        
        $multilineString = $parsed['strings']['multiline_test'];
        $this->assertArrayHasKey('localizations', $multilineString, 'Should have localizations');
        $this->assertArrayHasKey('en', $multilineString['localizations'], 'Should have English localization');
        
        $englishValue = $multilineString['localizations']['en']['stringUnit']['value'];
        $this->assertEquals("Line 1\nLine 2\nLine 3", $englishValue, 'Should preserve newlines in parsed content');
        $this->assertTrue(strpos($englishValue, "\n") !== false, 'Should contain actual newlines');
        $this->assertFalse(strpos($englishValue, "\\n") !== false, 'Should not contain escaped newlines');
    }
}