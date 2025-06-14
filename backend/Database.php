<?php

class Database {
    private $pdo;
    private $config;
    
    public function __construct($config) {
        $this->config = $config;
        $this->connect();
    }
    
    private function connect() {
        $db = $this->config['database'];
        
        try {
            switch ($db['driver']) {
                case 'sqlite':
                    $this->ensureDataDirectory();
                    $dsn = "sqlite:" . $db['sqlite_path'];
                    $this->pdo = new PDO($dsn);
                    $this->pdo->exec('PRAGMA foreign_keys = ON;');
                    break;
                    
                case 'mysql':
                    $dsn = "mysql:host={$db['host']};port={$db['port']};dbname={$db['database']};charset=utf8mb4";
                    $this->pdo = new PDO($dsn, $db['username'], $db['password']);
                    break;
                    
                case 'postgres':
                    $dsn = "pgsql:host={$db['host']};port={$db['port']};dbname={$db['database']}";
                    $this->pdo = new PDO($dsn, $db['username'], $db['password']);
                    break;
                    
                default:
                    throw new Exception("Unsupported database driver: {$db['driver']}");
            }
            
            $this->pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $this->pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
            
        } catch (PDOException $e) {
            throw new Exception("Database connection failed: " . $e->getMessage());
        }
    }
    
    private function ensureDataDirectory() {
        $dataDir = dirname($this->config['database']['sqlite_path']);
        if (!file_exists($dataDir)) {
            mkdir($dataDir, 0755, true);
        }
    }
    
    public function initializeSchema() {
        $schemaFile = __DIR__ . '/schema.sql';
        if (!file_exists($schemaFile)) {
            throw new Exception("Schema file not found");
        }
        
        $schema = file_get_contents($schemaFile);
        
        // Handle different SQL dialects
        if ($this->config['database']['driver'] === 'mysql') {
            $schema = $this->adaptSchemaForMySQL($schema);
        } elseif ($this->config['database']['driver'] === 'postgres') {
            $schema = $this->adaptSchemaForPostgreSQL($schema);
        }
        
        $statements = array_filter(array_map('trim', explode(';', $schema)));
        
        foreach ($statements as $statement) {
            if (!empty($statement)) {
                $this->pdo->exec($statement);
            }
        }
    }
    
    private function adaptSchemaForMySQL($schema) {
        // Replace SQLite-specific syntax with MySQL equivalents
        $schema = str_replace('INTEGER PRIMARY KEY AUTOINCREMENT', 'INT AUTO_INCREMENT PRIMARY KEY', $schema);
        $schema = str_replace('BOOLEAN', 'TINYINT(1)', $schema);
        $schema = str_replace('TIMESTAMP DEFAULT CURRENT_TIMESTAMP', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP', $schema);
        return $schema;
    }
    
    private function adaptSchemaForPostgreSQL($schema) {
        // Replace SQLite-specific syntax with PostgreSQL equivalents
        $schema = str_replace('INTEGER PRIMARY KEY AUTOINCREMENT', 'SERIAL PRIMARY KEY', $schema);
        $schema = str_replace('BOOLEAN DEFAULT FALSE', 'BOOLEAN DEFAULT FALSE', $schema);
        $schema = str_replace('TIMESTAMP DEFAULT CURRENT_TIMESTAMP', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP', $schema);
        return $schema;
    }
    
    public function getPDO() {
        return $this->pdo;
    }
    
    public function query($sql, $params = []) {
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);
        return $stmt;
    }
    
    public function fetchOne($sql, $params = []) {
        $stmt = $this->query($sql, $params);
        return $stmt->fetch();
    }
    
    public function fetchAll($sql, $params = []) {
        $stmt = $this->query($sql, $params);
        return $stmt->fetchAll();
    }
    
    public function execute($sql, $params = []) {
        $stmt = $this->query($sql, $params);
        return $stmt->rowCount();
    }
    
    public function lastInsertId() {
        return $this->pdo->lastInsertId();
    }
    
    public function beginTransaction() {
        return $this->pdo->beginTransaction();
    }
    
    public function commit() {
        return $this->pdo->commit();
    }
    
    public function rollback() {
        return $this->pdo->rollback();
    }
}