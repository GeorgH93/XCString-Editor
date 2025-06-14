<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

$requestMethod = $_SERVER['REQUEST_METHOD'];
$requestUri = $_SERVER['REQUEST_URI'];

function parseXcString($content) {
    $json = json_decode($content, true);
    if (!$json) {
        throw new Exception('Invalid xcstring format');
    }
    
    return $json;
}

function generateXcString($data) {
    return json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
}

try {
    switch ($requestMethod) {
        case 'POST':
            if (strpos($requestUri, '/parse') !== false) {
                $input = json_decode(file_get_contents('php://input'), true);
                if (!isset($input['content'])) {
                    throw new Exception('Content is required');
                }
                
                $parsed = parseXcString($input['content']);
                echo json_encode(['success' => true, 'data' => $parsed]);
            } else if (strpos($requestUri, '/generate') !== false) {
                $input = json_decode(file_get_contents('php://input'), true);
                if (!isset($input['data'])) {
                    throw new Exception('Data is required');
                }
                
                $xcstring = generateXcString($input['data']);
                echo json_encode(['success' => true, 'xcstring' => $xcstring]);
            } else {
                throw new Exception('Invalid endpoint');
            }
            break;
            
        case 'GET':
            if (strpos($requestUri, '/test') !== false) {
                echo json_encode(['success' => true, 'message' => 'XCString Tool API is working']);
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