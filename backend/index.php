<?php
// Prevent warnings from breaking JSON output
error_reporting(E_ERROR | E_PARSE);
ini_set('display_errors', 0);

session_start();

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// Load configuration and classes
$configPath = file_exists('../config.php') ? '../config.php' : __DIR__ . '/../config.php';
if (!file_exists($configPath)) {
    http_response_code(500);
    echo json_encode(['success' => false, 'error' => 'Configuration file not found']);
    exit;
}
$config = require $configPath;
require_once 'Database.php';
require_once 'Auth.php';
require_once 'FileManager.php';
require_once 'AIService.php';

// Initialize services
try {
    $db = new Database($config);
    $auth = new Auth($db, $config);
    $fileManager = new FileManager($db, $config);
    
    // Initialize AI service (defensive initialization)
    try {
        $aiService = new AIService($config);
    } catch (Exception $e) {
        // Create a disabled AI service if initialization fails
        $aiService = new class {
            public function isEnabled() { return false; }
            public function getAvailableProviders() { return []; }
            public function translate($text, $sourceLanguage, $targetLanguage, $context = [], $provider = null, $model = null) {
                throw new Exception('AI features are not enabled');
            }
            public function proofread($text, $language, $context = [], $provider = null, $model = null) {
                throw new Exception('AI features are not enabled');
            }
            public function buildContext($currentKey, $allStrings, $language, $maxItems = 5) {
                return [];
            }
            public function buildTranslationContext($currentKey, $allStrings, $sourceLanguage, $targetLanguage, $maxItems = 5) {
                return [];
            }
        };
    }
    
    // Initialize database schema if needed (for SQLite)
    if ($config['database']['driver'] === 'sqlite') {
        $sqlitePath = $config['database']['sqlite_path'];
        if (!file_exists($sqlitePath)) {
            $db->initializeSchema();
        }
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'error' => 'Database initialization failed']);
    exit;
}

$requestMethod = $_SERVER['REQUEST_METHOD'];
$requestUri = $_SERVER['REQUEST_URI'];
$currentUser = $auth->getCurrentUser();

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

function fixDataForJavaScript($data) {
    // Fix specific issues with XCString data structure for JavaScript consumption
    if (isset($data['strings'])) {
        foreach ($data['strings'] as $stringKey => &$stringData) {
            // If a string object became an empty array, convert it back to an object with localizations
            if (is_array($stringData) && empty($stringData)) {
                $stringData = [
                    'localizations' => new stdClass()
                ];
            }
            
            // Ensure localizations property exists as an object
            if (is_array($stringData)) {
                if (!isset($stringData['localizations'])) {
                    $stringData['localizations'] = new stdClass();
                } elseif (is_array($stringData['localizations']) && empty($stringData['localizations'])) {
                    $stringData['localizations'] = new stdClass();
                }
            }
        }
    }
    return $data;
}

function generateXcString($data) {
    // Ensure objects stay as objects (not arrays) in the structure
    $data = ensureObjectsStayObjects($data);
    
    // Generate JSON with proper formatting
    $json = json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    
    // Convert 4-space indentation to 2-space indentation
    $json = preg_replace_callback('/^(    )+/m', function($matches) {
        $indentLevel = strlen($matches[0]) / 4;
        return str_repeat('  ', $indentLevel);
    }, $json);
    
    // Ensure proper spacing around colons: "key" : "value" (space before and after colon)
    $json = preg_replace('/"\s*:\s*/', '" : ', $json);
    
    return $json;
}

try {
    // Clean expired sessions periodically
    if (rand(1, 100) === 1) {
        $auth->cleanExpiredSessions();
    }
    
    switch ($requestMethod) {
        case 'POST':
            $input = json_decode(file_get_contents('php://input'), true);
            
            if (strpos($requestUri, '/auth/register') !== false) {
                if (!isset($input['email'], $input['name'], $input['password'])) {
                    throw new Exception('Email, name, and password are required');
                }
                $userId = $auth->register($input['email'], $input['name'], $input['password']);
                echo json_encode(['success' => true, 'user_id' => $userId]);
                
            } elseif (strpos($requestUri, '/auth/login') !== false) {
                if (!isset($input['email'], $input['password'])) {
                    throw new Exception('Email and password are required');
                }
                $user = $auth->login($input['email'], $input['password']);
                echo json_encode(['success' => true, 'user' => $user]);
                
            } elseif (strpos($requestUri, '/auth/logout') !== false) {
                $auth->logout();
                echo json_encode(['success' => true]);
                
            } elseif (strpos($requestUri, '/files/save') !== false) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                if (!isset($input['name'], $input['content'])) {
                    throw new Exception('Name and content are required');
                }
                $fileId = $fileManager->saveFile(
                    $currentUser['id'], 
                    $input['name'], 
                    $input['content'],
                    $input['is_public'] ?? false
                );
                echo json_encode(['success' => true, 'file_id' => $fileId]);
                
            } elseif (strpos($requestUri, '/files/update') !== false) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                if (!isset($input['file_id'], $input['content'])) {
                    throw new Exception('File ID and content are required');
                }
                $fileManager->updateFile($input['file_id'], $currentUser['id'], $input['content']);
                echo json_encode(['success' => true]);
                
            } elseif (strpos($requestUri, '/files/share') !== false) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                if (!isset($input['file_id'], $input['email'])) {
                    throw new Exception('File ID and email are required');
                }
                $fileManager->shareFile(
                    $input['file_id'], 
                    $currentUser['id'], 
                    $input['email'],
                    $input['can_edit'] ?? false
                );
                echo json_encode(['success' => true]);
                
            } elseif (strpos($requestUri, '/parse') !== false) {
                if (!isset($input['content'])) {
                    throw new Exception('Content is required');
                }
                $parsed = parseXcString($input['content']);
                $parsed = fixDataForJavaScript($parsed);
                echo json_encode(['success' => true, 'data' => $parsed]);
                
            } elseif (strpos($requestUri, '/generate') !== false) {
                if (!isset($input['data'])) {
                    throw new Exception('Data is required');
                }
                $xcstring = generateXcString($input['data']);
                echo json_encode(['success' => true, 'xcstring' => $xcstring]);
                
            } elseif (strpos($requestUri, '/ai/translate') !== false) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                if (!$aiService->isEnabled()) {
                    throw new Exception('AI features are not enabled');
                }
                if (!isset($input['text'], $input['source_language'], $input['target_language'])) {
                    throw new Exception('Text, source_language, and target_language are required');
                }
                
                $context = [];
                if (isset($input['context_strings'])) {
                    $context = $aiService->buildTranslationContext(
                        $input['string_key'] ?? '',
                        $input['context_strings'],
                        $input['source_language'],
                        $input['target_language']
                    );
                }
                
                $translation = $aiService->translate(
                    $input['text'],
                    $input['source_language'],
                    $input['target_language'],
                    $context,
                    $input['provider'] ?? null,
                    $input['model'] ?? null
                );
                
                echo json_encode(['success' => true, 'translation' => $translation]);
                
            } elseif (strpos($requestUri, '/ai/proofread') !== false) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                if (!$aiService->isEnabled()) {
                    throw new Exception('AI features are not enabled');
                }
                if (!isset($input['text'], $input['language'])) {
                    throw new Exception('Text and language are required');
                }
                
                $context = [];
                if (isset($input['context_strings'])) {
                    $context = $aiService->buildContext(
                        $input['string_key'] ?? '',
                        $input['context_strings'],
                        $input['language']
                    );
                }
                
                $review = $aiService->proofread(
                    $input['text'],
                    $input['language'],
                    $context,
                    $input['provider'] ?? null,
                    $input['model'] ?? null
                );
                
                echo json_encode(['success' => true, 'review' => $review]);
                
            } elseif (strpos($requestUri, '/ai/batch-translate') !== false) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                if (!$aiService->isEnabled()) {
                    throw new Exception('AI features are not enabled');
                }
                if (!isset($input['items'], $input['source_language'], $input['target_language'])) {
                    throw new Exception('Items, source_language, and target_language are required');
                }
                if (!is_array($input['items'])) {
                    throw new Exception('Items must be an array');
                }
                
                $translations = $aiService->batchTranslate(
                    $input['items'],
                    $input['source_language'],
                    $input['target_language'],
                    $input['provider'] ?? null,
                    $input['model'] ?? null
                );
                
                echo json_encode(['success' => true, 'translations' => $translations]);
                
            } elseif (strpos($requestUri, '/ai/batch-proofread') !== false) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                if (!$aiService->isEnabled()) {
                    throw new Exception('AI features are not enabled');
                }
                if (!isset($input['items'], $input['language'])) {
                    throw new Exception('Items and language are required');
                }
                if (!is_array($input['items'])) {
                    throw new Exception('Items must be an array');
                }
                
                $reviews = $aiService->batchProofread(
                    $input['items'],
                    $input['language'],
                    $input['provider'] ?? null,
                    $input['model'] ?? null
                );
                
                echo json_encode(['success' => true, 'reviews' => $reviews]);
                
            } else {
                throw new Exception('Invalid endpoint');
            }
            break;
            
        case 'GET':
            if (strpos($requestUri, '/auth/user') !== false) {
                $oauthProviders = $auth->getOAuth2Providers();
                echo json_encode([
                    'success' => true, 
                    'user' => $currentUser,
                    'config' => [
                        'registration_enabled' => $config['registration']['enabled'],
                        'oauth2_enabled' => $config['oauth2']['enabled'],
                        'oauth2_providers' => array_keys($oauthProviders),
                        'ai_enabled' => $aiService->isEnabled(),
                        'ai_providers' => $aiService->getAvailableProviders()
                    ]
                ]);
                
            } elseif (preg_match('/\/auth\/oauth\/([^\/]+)\/redirect/', $requestUri, $matches)) {
                // OAuth2 login redirect
                $provider = $matches[1];
                try {
                    require_once 'OAuth2Provider.php';
                    $oauthProviders = $auth->getOAuth2Providers();
                    
                    if (!isset($oauthProviders[$provider])) {
                        throw new Exception('Provider not available');
                    }
                    
                    $oauthProvider = OAuth2ProviderFactory::create($provider, $oauthProviders[$provider]['config'], $config);
                    $state = $auth->generateOAuth2State($provider);
                    $authUrl = $oauthProvider->getAuthorizationUrl($state);
                    
                    header('Location: ' . $authUrl);
                    exit;
                } catch (Exception $e) {
                    header('Location: ' . $config['oauth2']['base_url'] . '?oauth_error=' . urlencode($e->getMessage()));
                    exit;
                }
                
            } elseif (preg_match('/\/auth\/oauth\/([^\/]+)\/callback/', $requestUri, $matches)) {
                // OAuth2 callback
                $provider = $matches[1];
                try {
                    if (!isset($_GET['code'], $_GET['state'])) {
                        throw new Exception('Missing authorization code or state');
                    }
                    
                    if (isset($_GET['error'])) {
                        throw new Exception('OAuth2 error: ' . $_GET['error']);
                    }
                    
                    if (!$auth->verifyOAuth2State($_GET['state'], $provider)) {
                        throw new Exception('Invalid OAuth2 state');
                    }
                    
                    require_once 'OAuth2Provider.php';
                    $oauthProviders = $auth->getOAuth2Providers();
                    
                    if (!isset($oauthProviders[$provider])) {
                        throw new Exception('Provider not available');
                    }
                    
                    $oauthProvider = OAuth2ProviderFactory::create($provider, $oauthProviders[$provider]['config'], $config);
                    $accessToken = $oauthProvider->getAccessToken($_GET['code']);
                    $userInfo = $oauthProvider->getUserInfo($accessToken);
                    
                    $user = $auth->handleOAuth2Login($userInfo);
                    
                    header('Location: ' . $config['oauth2']['base_url'] . '?oauth_success=1');
                    exit;
                } catch (Exception $e) {
                    header('Location: ' . $config['oauth2']['base_url'] . '?oauth_error=' . urlencode($e->getMessage()));
                    exit;
                }
                
            } elseif (strpos($requestUri, '/files/my') !== false) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                $files = $fileManager->getUserFiles($currentUser['id']);
                echo json_encode(['success' => true, 'files' => $files]);
                
            } elseif (strpos($requestUri, '/files/shared') !== false) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                $files = $fileManager->getSharedFiles($currentUser['id']);
                echo json_encode(['success' => true, 'files' => $files]);
                
            } elseif (strpos($requestUri, '/files/public') !== false) {
                $files = $fileManager->getPublicFiles();
                echo json_encode(['success' => true, 'files' => $files]);
                
            } elseif (preg_match('/\/files\/(\d+)/', $requestUri, $matches)) {
                $fileId = $matches[1];
                $file = $fileManager->getFile($fileId, $currentUser['id'] ?? null);
                echo json_encode(['success' => true, 'file' => $file]);
                
            } elseif (preg_match('/\/files\/(\d+)\/shares/', $requestUri, $matches)) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                $fileId = $matches[1];
                $shares = $fileManager->getFileShares($fileId, $currentUser['id']);
                echo json_encode(['success' => true, 'shares' => $shares]);
                
            } elseif (strpos($requestUri, '/test') !== false) {
                echo json_encode(['success' => true, 'message' => 'XCString Tool API is working']);
                
            } else {
                throw new Exception('Invalid endpoint');
            }
            break;
            
        case 'DELETE':
            if (preg_match('/\/files\/(\d+)/', $requestUri, $matches)) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                $fileId = $matches[1];
                $fileManager->deleteFile($fileId, $currentUser['id']);
                echo json_encode(['success' => true]);
                
            } elseif (preg_match('/\/files\/(\d+)\/shares\/(\d+)/', $requestUri, $matches)) {
                if (!$currentUser) {
                    throw new Exception('Authentication required');
                }
                $fileId = $matches[1];
                $userId = $matches[2];
                $fileManager->unshareFile($fileId, $currentUser['id'], $userId);
                echo json_encode(['success' => true]);
                
            } else {
                throw new Exception('Invalid endpoint');
            }
            break;
            
        default:
            throw new Exception('Method not allowed');
    }
} catch (Exception $e) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => $e->getMessage()]);
}
?>