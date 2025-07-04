<?php
/**
 * Simple Test Runner for XCString Editor
 * A lightweight testing framework without external dependencies
 */

class TestCase {
    protected $name;
    protected $passed = 0;
    protected $failed = 0;
    protected $errors = [];
    
    public function __construct($name) {
        $this->name = $name;
    }
    
    public function setUp() {
        // Override in subclasses
    }
    
    public function tearDown() {
        // Override in subclasses
    }
    
    public function assertEquals($expected, $actual, $message = '') {
        if ($expected === $actual) {
            $this->passed++;
            return true;
        } else {
            $this->failed++;
            $this->errors[] = "assertEquals failed: " . ($message ?: "Expected '$expected', got '$actual'");
            return false;
        }
    }
    
    public function assertNotEquals($expected, $actual, $message = '') {
        if ($expected !== $actual) {
            $this->passed++;
            return true;
        } else {
            $this->failed++;
            $this->errors[] = "assertNotEquals failed: " . ($message ?: "Expected not '$expected', but got '$actual'");
            return false;
        }
    }
    
    public function assertTrue($condition, $message = '') {
        if ($condition === true) {
            $this->passed++;
            return true;
        } else {
            $this->failed++;
            $this->errors[] = "assertTrue failed: " . ($message ?: "Expected true, got false");
            return false;
        }
    }
    
    public function assertFalse($condition, $message = '') {
        if ($condition === false) {
            $this->passed++;
            return true;
        } else {
            $this->failed++;
            $this->errors[] = "assertFalse failed: " . ($message ?: "Expected false, got true");
            return false;
        }
    }
    
    public function assertArrayHasKey($key, $array, $message = '') {
        if (is_array($array) && array_key_exists($key, $array)) {
            $this->passed++;
            return true;
        } else {
            $this->failed++;
            $this->errors[] = "assertArrayHasKey failed: " . ($message ?: "Key '$key' not found in array");
            return false;
        }
    }
    
    public function assertInstanceOf($expected, $actual, $message = '') {
        if ($actual instanceof $expected) {
            $this->passed++;
            return true;
        } else {
            $this->failed++;
            $actualType = is_object($actual) ? get_class($actual) : gettype($actual);
            $this->errors[] = "assertInstanceOf failed: " . ($message ?: "Expected instance of '$expected', got '$actualType'");
            return false;
        }
    }
    
    public function expectException($exceptionClass, $callable) {
        try {
            $callable();
            $this->failed++;
            $this->errors[] = "expectException failed: Expected '$exceptionClass' but no exception was thrown";
            return false;
        } catch (Exception $e) {
            if ($e instanceof $exceptionClass) {
                $this->passed++;
                return true;
            } else {
                $this->failed++;
                $actualClass = get_class($e);
                $this->errors[] = "expectException failed: Expected '$exceptionClass', got '$actualClass'";
                return false;
            }
        }
    }
    
    public function run() {
        echo "Running {$this->name}...\n";
        
        $methods = get_class_methods($this);
        $testMethods = array_filter($methods, function($method) {
            return strpos($method, 'test') === 0;
        });
        
        foreach ($testMethods as $method) {
            $this->setUp();
            try {
                $this->$method();
                echo "  ✓ $method\n";
            } catch (Exception $e) {
                $this->failed++;
                $this->errors[] = "$method failed with exception: " . $e->getMessage();
                echo "  ✗ $method - {$e->getMessage()}\n";
            }
            $this->tearDown();
        }
        
        return [
            'passed' => $this->passed,
            'failed' => $this->failed,
            'errors' => $this->errors
        ];
    }
}

class TestRunner {
    private $tests = [];
    
    public function addTest(TestCase $test) {
        $this->tests[] = $test;
    }
    
    public function run() {
        $totalPassed = 0;
        $totalFailed = 0;
        $allErrors = [];
        
        echo "XCString Editor Test Suite\n";
        echo "=========================\n\n";
        
        foreach ($this->tests as $test) {
            $result = $test->run();
            $totalPassed += $result['passed'];
            $totalFailed += $result['failed'];
            $allErrors = array_merge($allErrors, $result['errors']);
            echo "\n";
        }
        
        echo "Test Results:\n";
        echo "=============\n";
        echo "Passed: $totalPassed\n";
        echo "Failed: $totalFailed\n";
        echo "Total: " . ($totalPassed + $totalFailed) . "\n\n";
        
        if (!empty($allErrors)) {
            echo "Errors:\n";
            echo "-------\n";
            foreach ($allErrors as $error) {
                echo "- $error\n";
            }
            echo "\n";
        }
        
        if ($totalFailed > 0) {
            echo "❌ Some tests failed!\n";
            exit(1);
        } else {
            echo "✅ All tests passed!\n";
            exit(0);
        }
    }
}