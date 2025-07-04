<?php

require_once __DIR__ . '/TestRunner.php';

// Include the backend functions we need for export testing
if (!function_exists('ensureObjectsStayObjects')) {
    function ensureObjectsStayObjects($data) {
        if (is_array($data)) {
            if (empty($data)) {
                return new stdClass();
            }
            if (array_keys($data) !== range(0, count($data) - 1)) {
                $obj = new stdClass();
                foreach ($data as $key => $value) {
                    $obj->$key = ensureObjectsStayObjects($value);
                }
                return $obj;
            }
            return array_map('ensureObjectsStayObjects', $data);
        }
        return $data;
    }
}

if (!function_exists('fixDoubleEscapedNewlines')) {
    function fixDoubleEscapedNewlines($data) {
        if (is_array($data)) {
            foreach ($data as $key => $value) {
                $data[$key] = fixDoubleEscapedNewlines($value);
            }
        } elseif (is_object($data)) {
            foreach ($data as $key => $value) {
                $data->$key = fixDoubleEscapedNewlines($value);
            }
        } elseif (is_string($data)) {
            // Convert double-escaped newlines (\\n) back to single-escaped newlines (\n)
            // This fixes the issue where textareas convert actual newlines to \\n
            $data = str_replace('\\n', "\n", $data);
        }
        return $data;
    }
}

if (!function_exists('generateXcString')) {
    function generateXcString($data) {
        // Ensure objects stay as objects (not arrays) in the structure
        $data = ensureObjectsStayObjects($data);
        
        // Fix double-escaped newlines from frontend textarea inputs
        $data = fixDoubleEscapedNewlines($data);
        
        // Generate JSON with proper formatting
        $json = json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        
        // Convert 4-space indentation to 2-space indentation
        $json = preg_replace_callback('/^(    )+/m', function($matches) {
            $indentLevel = strlen($matches[0]) / 4;
            return str_repeat('  ', $indentLevel);
        }, $json);
        
        // Ensure proper spacing around colons: "key" : "value" (space before and after colon)
        $json = preg_replace('/"\s*:\s*/', '" : ', $json);
        
        return $json;
    }
}

class XCStringExportTest extends TestCase {
    
    public function __construct() {
        parent::__construct('XCString Export Tests');
    }
    
    public function testBasicExport() {
        $data = [
            'sourceLanguage' => 'en',
            'version' => '1.0',
            'strings' => [
                'simple_key' => [
                    'comment' => 'A simple string',
                    'localizations' => [
                        'en' => [
                            'stringUnit' => [
                                'state' => 'translated',
                                'value' => 'Hello World'
                            ]
                        ]
                    ]
                ]
            ]
        ];
        
        $result = generateXcString($data);
        
        $this->assertTrue(is_string($result), 'Should return a string');
        $this->assertTrue(strpos($result, '"Hello World"') !== false, 'Should contain the string value');
        $this->assertTrue(strpos($result, '"sourceLanguage" : "en"') !== false, 'Should have proper colon spacing');
        
        // Verify it's valid JSON
        $parsed = json_decode($result, true);
        $this->assertTrue($parsed !== false, 'Should generate valid JSON');
        $this->assertEquals('en', $parsed['sourceLanguage'], 'Source language should be preserved');
    }
    
    public function testEmptyLocalizationsStayObjects() {
        $data = [
            'sourceLanguage' => 'en',
            'strings' => [
                'empty_key' => [
                    'localizations' => []
                ]
            ]
        ];
        
        $result = generateXcString($data);
        
        // Should contain {} not []
        $this->assertTrue(strpos($result, '"localizations" : {}') !== false, 'Empty localizations should be objects');
        $this->assertFalse(strpos($result, '"localizations" : []') !== false, 'Should not contain empty arrays');
    }
    
    public function testNewlinePreservation() {
        // Data with double-escaped newlines (as would come from frontend)
        $data = [
            'sourceLanguage' => 'en',
            'strings' => [
                'multiline_key' => [
                    'localizations' => [
                        'en' => [
                            'stringUnit' => [
                                'state' => 'translated',
                                'value' => 'Line 1\\nLine 2\\nLine 3'
                            ]
                        ]
                    ]
                ]
            ]
        ];
        
        $result = generateXcString($data);
        
        // Should contain proper \n escapes, not \\n
        $this->assertTrue(strpos($result, 'Line 1\\nLine 2\\nLine 3') !== false, 'Should contain properly escaped newlines');
        $this->assertFalse(strpos($result, 'Line 1\\\\nLine 2\\\\nLine 3') !== false, 'Should not contain double-escaped newlines');
        
        // Verify it's valid JSON and parses correctly
        $parsed = json_decode($result, true);
        $this->assertTrue($parsed !== false, 'Should generate valid JSON');
        
        $parsedValue = $parsed['strings']['multiline_key']['localizations']['en']['stringUnit']['value'];
        $this->assertEquals("Line 1\nLine 2\nLine 3", $parsedValue, 'Parsed value should have actual newlines');
    }
    
    public function testComplexDataStructure() {
        $data = [
            'sourceLanguage' => 'en',
            'version' => '1.0',
            'strings' => [
                'plural_key' => [
                    'comment' => 'Plural variations',
                    'localizations' => [
                        'en' => [
                            'variations' => [
                                'plural' => [
                                    'one' => [
                                        'stringUnit' => [
                                            'state' => 'translated',
                                            'value' => '%d item'
                                        ]
                                    ],
                                    'other' => [
                                        'stringUnit' => [
                                            'state' => 'translated',
                                            'value' => '%d items'
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ]
            ]
        ];
        
        $result = generateXcString($data);
        
        // Verify structure is preserved
        $this->assertTrue(strpos($result, '"variations"') !== false, 'Should contain variations');
        $this->assertTrue(strpos($result, '"plural"') !== false, 'Should contain plural key');
        $this->assertTrue(strpos($result, '"one"') !== false, 'Should contain one variant');
        $this->assertTrue(strpos($result, '"other"') !== false, 'Should contain other variant');
        
        // Verify valid JSON
        $parsed = json_decode($result, true);
        $this->assertTrue($parsed !== false, 'Should generate valid JSON');
        
        // Check deep structure
        $this->assertArrayHasKey('plural', $parsed['strings']['plural_key']['localizations']['en']['variations']);
        $this->assertEquals('%d item', $parsed['strings']['plural_key']['localizations']['en']['variations']['plural']['one']['stringUnit']['value']);
        $this->assertEquals('%d items', $parsed['strings']['plural_key']['localizations']['en']['variations']['plural']['other']['stringUnit']['value']);
    }
    
    public function testUnicodePreservation() {
        $data = [
            'sourceLanguage' => 'en',
            'strings' => [
                'unicode_key' => [
                    'localizations' => [
                        'es' => [
                            'stringUnit' => [
                                'value' => 'Línea con acentos\\nSegunda línea'
                            ]
                        ],
                        'zh' => [
                            'stringUnit' => [
                                'value' => '中文文本\\n第二行'
                            ]
                        ]
                    ]
                ]
            ]
        ];
        
        $result = generateXcString($data);
        
        // Should preserve Unicode characters
        $this->assertTrue(strpos($result, 'Línea con acentos') !== false, 'Should preserve Spanish accents');
        $this->assertTrue(strpos($result, '中文文本') !== false, 'Should preserve Chinese characters');
        
        // Verify valid JSON
        $parsed = json_decode($result, true);
        $this->assertTrue($parsed !== false, 'Should generate valid JSON with Unicode');
        
        $spanishValue = $parsed['strings']['unicode_key']['localizations']['es']['stringUnit']['value'];
        $this->assertTrue(strpos($spanishValue, "\n") !== false, 'Should preserve newlines in Unicode text');
    }
    
    public function testIndentationFormatting() {
        $data = [
            'sourceLanguage' => 'en',
            'strings' => [
                'test' => [
                    'localizations' => [
                        'en' => [
                            'stringUnit' => [
                                'value' => 'test'
                            ]
                        ]
                    ]
                ]
            ]
        ];
        
        $result = generateXcString($data);
        
        // Should use 2-space indentation pattern
        $this->assertTrue(strpos($result, '  "sourceLanguage"') !== false, 'Should have 2-space indented top-level keys');
        $this->assertTrue(strpos($result, '    "test"') !== false, 'Should have 4-space indented nested keys');
        
        // Test that the conversion from 4-space to 2-space works correctly
        // The nested objects should use multiples of 2 spaces
        $lines = explode("\n", $result);
        
        // Find lines with significant indentation and verify they use 2-space increments
        $validIndentation = true;
        foreach ($lines as $line) {
            if (preg_match('/^( +)"/', $line, $matches)) {
                $spacesCount = strlen($matches[1]);
                if ($spacesCount % 2 !== 0) {
                    $validIndentation = false;
                    break;
                }
            }
        }
        
        $this->assertTrue($validIndentation, 'All indentation should be multiples of 2 spaces');
    }
    
    public function testColonSpacing() {
        $data = [
            'sourceLanguage' => 'en',
            'version' => '1.0'
        ];
        
        $result = generateXcString($data);
        
        // Should have proper colon spacing: "key" : "value"
        $this->assertTrue(strpos($result, '"sourceLanguage" : "en"') !== false, 'Should have space before and after colon');
        $this->assertTrue(strpos($result, '"version" : "1.0"') !== false, 'Should have consistent colon spacing');
        
        // Should not have inconsistent spacing
        $this->assertFalse(strpos($result, '"sourceLanguage":"en"') !== false, 'Should not have no spaces around colon');
        $this->assertFalse(strpos($result, '"sourceLanguage" :"en"') !== false, 'Should not have space only before colon');
    }
}