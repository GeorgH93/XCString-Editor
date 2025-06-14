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
    
    // OAuth2 Methods
    
    public function generateOAuth2State($provider) {
        $state = bin2hex(random_bytes(32));
        
        // Store state for verification
        $this->db->execute(
            'INSERT INTO oauth2_states (id, provider) VALUES (?, ?)',
            [$state, $provider]
        );
        
        // Clean up old states (older than 1 hour)
        $this->db->execute(
            'DELETE FROM oauth2_states WHERE created_at < datetime("now", "-1 hour")'
        );
        
        return $state;
    }
    
    public function verifyOAuth2State($state, $provider) {
        $result = $this->db->fetchOne(
            'SELECT provider FROM oauth2_states WHERE id = ? AND provider = ?',
            [$state, $provider]
        );
        
        if ($result) {
            // Remove used state
            $this->db->execute('DELETE FROM oauth2_states WHERE id = ?', [$state]);
            return true;
        }
        
        return false;
    }
    
    public function handleOAuth2Login($userInfo) {
        // Check if OAuth2 account already exists
        $oauthAccount = $this->db->fetchOne(
            'SELECT user_id FROM oauth2_accounts WHERE provider = ? AND provider_user_id = ?',
            [$userInfo['provider'], $userInfo['provider_id']]
        );
        
        if ($oauthAccount) {
            // Existing OAuth2 account - get user
            $user = $this->db->fetchOne(
                'SELECT id, email, name, avatar_url FROM users WHERE id = ?',
                [$oauthAccount['user_id']]
            );
            
            if (!$user) {
                throw new Exception('User account not found');
            }
            
            // Update user info if changed
            $this->updateUserFromOAuth2($user['id'], $userInfo);
            
            return $this->createSession($user);
        }
        
        // Check if user exists with same email
        $existingUser = $this->db->fetchOne(
            'SELECT id, email, name, avatar_url FROM users WHERE email = ?',
            [$userInfo['email']]
        );
        
        if ($existingUser) {
            // Link existing user account with OAuth2 provider
            $this->linkOAuth2Account($existingUser['id'], $userInfo);
            $this->updateUserFromOAuth2($existingUser['id'], $userInfo);
            
            return $this->createSession($existingUser);
        }
        
        // Create new user account
        return $this->createUserFromOAuth2($userInfo);
    }
    
    private function createUserFromOAuth2($userInfo) {
        // Validate email domain if restrictions are set
        if (!$this->isEmailAllowed($userInfo['email'])) {
            throw new Exception('Email domain not allowed');
        }
        
        // Check if registration is enabled
        if (!$this->config['registration']['enabled']) {
            throw new Exception('Registration is disabled');
        }
        
        try {
            $this->db->beginTransaction();
            
            // Create user (password_hash is nullable for OAuth2 users)
            $this->db->execute(
                'INSERT INTO users (email, name, avatar_url) VALUES (?, ?, ?)',
                [$userInfo['email'], $userInfo['name'], $userInfo['avatar']]
            );
            
            $userId = $this->db->lastInsertId();
            
            // Link OAuth2 account
            $this->linkOAuth2Account($userId, $userInfo);
            
            $this->db->commit();
            
            $user = [
                'id' => $userId,
                'email' => $userInfo['email'],
                'name' => $userInfo['name'],
                'avatar_url' => $userInfo['avatar']
            ];
            
            return $this->createSession($user);
            
        } catch (Exception $e) {
            $this->db->rollback();
            throw $e;
        }
    }
    
    private function linkOAuth2Account($userId, $userInfo) {
        // Use appropriate upsert syntax based on database driver
        $driver = $this->config['database']['driver'];
        
        if ($driver === 'sqlite') {
            $this->db->execute(
                'INSERT OR REPLACE INTO oauth2_accounts (user_id, provider, provider_user_id) VALUES (?, ?, ?)',
                [$userId, $userInfo['provider'], $userInfo['provider_id']]
            );
        } else {
            // For MySQL and PostgreSQL, use INSERT ... ON DUPLICATE KEY UPDATE / ON CONFLICT
            $existing = $this->db->fetchOne(
                'SELECT id FROM oauth2_accounts WHERE provider = ? AND provider_user_id = ?',
                [$userInfo['provider'], $userInfo['provider_id']]
            );
            
            if ($existing) {
                $this->db->execute(
                    'UPDATE oauth2_accounts SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE provider = ? AND provider_user_id = ?',
                    [$userId, $userInfo['provider'], $userInfo['provider_id']]
                );
            } else {
                $this->db->execute(
                    'INSERT INTO oauth2_accounts (user_id, provider, provider_user_id) VALUES (?, ?, ?)',
                    [$userId, $userInfo['provider'], $userInfo['provider_id']]
                );
            }
        }
    }
    
    private function updateUserFromOAuth2($userId, $userInfo) {
        // Update name and avatar if they've changed
        $this->db->execute(
            'UPDATE users SET name = ?, avatar_url = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
            [$userInfo['name'], $userInfo['avatar'], $userId]
        );
    }
    
    private function createSession($user) {
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
            'name' => $user['name'],
            'avatar_url' => $user['avatar_url'] ?? null
        ];
    }
    
    public function getOAuth2Providers() {
        require_once 'OAuth2Provider.php';
        return OAuth2ProviderFactory::getAvailableProviders($this->config);
    }
    
    public function unlinkOAuth2Provider($userId, $provider) {
        // Check if user has a password or other OAuth2 providers
        $user = $this->db->fetchOne(
            'SELECT password_hash FROM users WHERE id = ?',
            [$userId]
        );
        
        $otherProviders = $this->db->fetchAll(
            'SELECT provider FROM oauth2_accounts WHERE user_id = ? AND provider != ?',
            [$userId, $provider]
        );
        
        if (!$user['password_hash'] && count($otherProviders) === 0) {
            throw new Exception('Cannot unlink the only authentication method');
        }
        
        $this->db->execute(
            'DELETE FROM oauth2_accounts WHERE user_id = ? AND provider = ?',
            [$userId, $provider]
        );
        
        return true;
    }
    
    public function getUserOAuth2Providers($userId) {
        return $this->db->fetchAll(
            'SELECT provider, created_at FROM oauth2_accounts WHERE user_id = ?',
            [$userId]
        );
    }
}