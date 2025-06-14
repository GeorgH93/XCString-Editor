<?php

class Auth {
    private $db;
    private $config;
    
    public function __construct($db, $config) {
        $this->db = $db;
        $this->config = $config;
    }
    
    public function register($email, $name, $password) {
        // Validate email domain if restrictions are set
        if (!$this->isEmailAllowed($email)) {
            throw new Exception('Email domain not allowed');
        }
        
        // Check if registration is enabled
        if (!$this->config['registration']['enabled']) {
            throw new Exception('Registration is disabled');
        }
        
        // Validate input
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            throw new Exception('Invalid email address');
        }
        
        if (strlen($password) < 6) {
            throw new Exception('Password must be at least 6 characters');
        }
        
        if (strlen($name) < 1) {
            throw new Exception('Name is required');
        }
        
        // Check if user already exists
        $existing = $this->db->fetchOne('SELECT id FROM users WHERE email = ?', [$email]);
        if ($existing) {
            throw new Exception('User with this email already exists');
        }
        
        // Hash password
        $passwordHash = password_hash($password, PASSWORD_DEFAULT);
        
        // Insert user
        $this->db->execute(
            'INSERT INTO users (email, name, password_hash) VALUES (?, ?, ?)',
            [$email, $name, $passwordHash]
        );
        
        return $this->db->lastInsertId();
    }
    
    public function login($email, $password) {
        $user = $this->db->fetchOne(
            'SELECT id, email, name, password_hash FROM users WHERE email = ?',
            [$email]
        );
        
        if (!$user || !password_verify($password, $user['password_hash'])) {
            throw new Exception('Invalid email or password');
        }
        
        // Create session
        $sessionId = $this->generateSessionId();
        $expiresAt = date('Y-m-d H:i:s', time() + $this->config['session']['lifetime']);
        
        $this->db->execute(
            'INSERT INTO sessions (id, user_id, expires_at) VALUES (?, ?, ?)',
            [$sessionId, $user['id'], $expiresAt]
        );
        
        // Set cookie
        setcookie(
            $this->config['session']['cookie_name'],
            $sessionId,
            time() + $this->config['session']['lifetime'],
            '/',
            '',
            $this->config['session']['cookie_secure'],
            $this->config['session']['cookie_httponly']
        );
        
        return [
            'id' => $user['id'],
            'email' => $user['email'],
            'name' => $user['name']
        ];
    }
    
    public function logout() {
        $sessionId = $this->getSessionId();
        if ($sessionId) {
            $this->db->execute('DELETE FROM sessions WHERE id = ?', [$sessionId]);
            setcookie(
                $this->config['session']['cookie_name'],
                '',
                time() - 3600,
                '/'
            );
        }
    }
    
    public function getCurrentUser() {
        $sessionId = $this->getSessionId();
        if (!$sessionId) {
            return null;
        }
        
        $session = $this->db->fetchOne(
            'SELECT s.user_id, u.email, u.name 
             FROM sessions s 
             JOIN users u ON s.user_id = u.id 
             WHERE s.id = ? AND s.expires_at > CURRENT_TIMESTAMP',
            [$sessionId]
        );
        
        if (!$session) {
            // Clean up expired session
            $this->logout();
            return null;
        }
        
        return [
            'id' => $session['user_id'],
            'email' => $session['email'],
            'name' => $session['name']
        ];
    }
    
    public function isLoggedIn() {
        return $this->getCurrentUser() !== null;
    }
    
    private function generateSessionId() {
        return bin2hex(random_bytes(32));
    }
    
    private function getSessionId() {
        return $_COOKIE[$this->config['session']['cookie_name']] ?? null;
    }
    
    private function isEmailAllowed($email) {
        $allowedDomains = $this->config['registration']['allowed_domains'];
        
        // If no restrictions, allow all
        if (empty($allowedDomains)) {
            return true;
        }
        
        $domain = substr(strrchr($email, '@'), 1);
        return in_array($domain, $allowedDomains);
    }
    
    public function cleanExpiredSessions() {
        $this->db->execute('DELETE FROM sessions WHERE expires_at < CURRENT_TIMESTAMP');
    }
}