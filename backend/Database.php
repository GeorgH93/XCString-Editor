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
        
        // Run migrations after schema initialization
        $this->runMigrations();
    }
    
    public function runMigrations() {
        // Ensure migrations table exists
        $this->ensureMigrationsTable();
        
        // Get list of applied migrations
        $appliedMigrations = $this->getAppliedMigrations();
        
        // Get all migration files
        $migrationFiles = $this->getMigrationFiles();
        
        foreach ($migrationFiles as $migrationFile) {
            $migrationName = basename($migrationFile, '.sql');
            
            // Skip if migration already applied
            if (in_array($migrationName, $appliedMigrations)) {
                error_log("Skipping already applied migration: $migrationName");
                continue;
            }
            
            // Apply migration (each migration is its own transaction)
            $this->applyMigration($migrationFile, $migrationName);
            
            // Refresh applied migrations list after each migration
            $appliedMigrations = $this->getAppliedMigrations();
        }
    }
    
    private function ensureMigrationsTable() {
        // Check if migrations table exists
        $tableExists = false;
        
        switch ($this->config['database']['driver']) {
            case 'sqlite':
                $result = $this->fetchOne("SELECT name FROM sqlite_master WHERE type='table' AND name='migrations'");
                $tableExists = !empty($result);
                break;
            case 'mysql':
                $result = $this->fetchOne("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'migrations'", 
                    [$this->config['database']['database']]);
                $tableExists = !empty($result);
                break;
            case 'postgres':
                $result = $this->fetchOne("SELECT tablename FROM pg_tables WHERE tablename = 'migrations'");
                $tableExists = !empty($result);
                break;
        }
        
        if (!$tableExists) {
            // Create migrations table
            $migrationTableFile = __DIR__ . '/migrations/000_create_migrations_table.sql';
            if (file_exists($migrationTableFile)) {
                $sql = file_get_contents($migrationTableFile);
                $sql = $this->adaptSqlForDriver($sql);
                $this->pdo->exec($sql);
            }
        }
    }
    
    private function getAppliedMigrations() {
        try {
            $result = $this->fetchAll("SELECT migration FROM migrations ORDER BY applied_at");
            return array_column($result, 'migration');
        } catch (Exception $e) {
            // If migrations table doesn't exist yet, return empty array
            return [];
        }
    }
    
    private function getMigrationFiles() {
        $migrationDir = __DIR__ . '/migrations';
        $files = glob($migrationDir . '/*.sql');
        
        // Sort files to ensure proper order
        sort($files);
        
        // Filter out the migrations table creation file as it's handled separately
        return array_filter($files, function($file) {
            return basename($file) !== '000_create_migrations_table.sql';
        });
    }
    
    private function applyMigration($migrationFile, $migrationName) {
        error_log("Starting migration: $migrationName");
        
        try {
            $this->beginTransaction();
            
            // Handle special migrations that require PHP logic
            if ($migrationName === '002_populate_existing_file_versions') {
                error_log("Executing PHP logic for migration: $migrationName");
                $this->populateExistingFileVersions();
            } else {
                error_log("Executing SQL for migration: $migrationName");
                
                // Read migration SQL
                $sql = file_get_contents($migrationFile);
                
                // Adapt SQL for current database driver
                $sql = $this->adaptSqlForDriver($sql);
                
                error_log("Migration SQL: " . substr($sql, 0, 200) . "...");
                
                // Split into statements and execute
                $statements = array_filter(array_map('trim', explode(';', $sql)));
                
                foreach ($statements as $statement) {
                    if (!empty($statement) && !$this->isComment($statement)) {
                        error_log("Executing statement: " . substr($statement, 0, 100) . "...");
                        $this->pdo->exec($statement);
                    }
                }
            }
            
            // Record migration as applied
            $this->execute("INSERT INTO migrations (migration) VALUES (?)", [$migrationName]);
            
            $this->commit();
            
            error_log("Successfully applied migration: $migrationName");
            
        } catch (Exception $e) {
            $this->rollback();
            error_log("Migration failed: $migrationName - " . $e->getMessage());
            throw new Exception("Failed to apply migration $migrationName: " . $e->getMessage());
        }
    }
    
    private function populateExistingFileVersions() {
        // First check if file_versions table exists
        if (!$this->tableExists('file_versions')) {
            error_log("file_versions table does not exist yet, skipping population");
            return;
        }
        
        // Get all existing files that don't have versions yet
        $files = $this->fetchAll("
            SELECT f.id, f.content, f.user_id, f.created_at 
            FROM xcstring_files f 
            LEFT JOIN file_versions fv ON f.id = fv.file_id 
            WHERE fv.id IS NULL
        ");
        
        foreach ($files as $file) {
            $contentHash = hash('sha256', $file['content']);
            $sizeBytes = strlen($file['content']);
            
            // Create initial version for existing file
            $this->execute("
                INSERT INTO file_versions 
                (file_id, version_number, content, comment, created_by_user_id, created_at, content_hash, size_bytes) 
                VALUES (?, 1, ?, 'Initial version (migrated)', ?, ?, ?, ?)
            ", [
                $file['id'], 
                $file['content'], 
                $file['user_id'], 
                $file['created_at'],
                $contentHash, 
                $sizeBytes
            ]);
        }
        
        error_log("Populated version history for " . count($files) . " existing files");
    }
    
    private function tableExists($tableName) {
        switch ($this->config['database']['driver']) {
            case 'sqlite':
                $result = $this->fetchOne("SELECT name FROM sqlite_master WHERE type='table' AND name=?", [$tableName]);
                break;
            case 'mysql':
                $result = $this->fetchOne("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?", 
                    [$this->config['database']['database'], $tableName]);
                break;
            case 'postgres':
                $result = $this->fetchOne("SELECT tablename FROM pg_tables WHERE tablename = ?", [$tableName]);
                break;
            default:
                return false;
        }
        
        return !empty($result);
    }
    
    private function adaptSqlForDriver($sql) {
        switch ($this->config['database']['driver']) {
            case 'mysql':
                return $this->adaptSchemaForMySQL($sql);
            case 'postgres':
                return $this->adaptSchemaForPostgreSQL($sql);
            default:
                return $sql;
        }
    }
    
    private function isComment($statement) {
        return strpos(trim($statement), '--') === 0;
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