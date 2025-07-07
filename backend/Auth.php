<?php

class Auth {
    private $db;
    private $config;
    private $fileManager;
    
    public function __construct($db, $config, $fileManager = null) {
        $this->db = $db;
        $this->config = $config;
        $this->fileManager = $fileManager;
    }
    
    public function register($email, $name, $password, $inviteToken = null) {
        // Validate email domain if restrictions are set
        if (!$this->isEmailAllowed($email)) {
            throw new Exception('Email domain not allowed');
        }
        
        // Check if registration is enabled or if valid invite token provided
        if (!$this->config['registration']['enabled']) {
            if (!$inviteToken) {
                throw new Exception('Registration is disabled');
            }
            if (!$this->validateInviteToken($inviteToken, $email)) {
                throw new Exception('Invalid or expired invite token');
            }
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
        
        $userId = $this->db->lastInsertId();
        
        // Mark invite as used if provided
        if ($inviteToken) {
            $this->markInviteAsUsed($inviteToken, $userId);
        }
        
        // Convert any pending shares for this email address
        if ($this->fileManager) {
            try {
                $this->fileManager->convertPendingSharesForNewUser($email, $userId);
            } catch (Exception $e) {
                // Log error but don't fail registration
                error_log("Failed to convert pending shares for new user $email: " . $e->getMessage());
            }
        }
        
        return $userId;
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
    
    public function handleOAuth2Login($userInfo, $providerConfig = null) {
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
        return $this->createUserFromOAuth2($userInfo, $providerConfig);
    }
    
    private function createUserFromOAuth2($userInfo, $providerConfig = null) {
        // Validate email domain if restrictions are set
        if (!$this->isEmailAllowed($userInfo['email'])) {
            throw new Exception('Email domain not allowed');
        }
        
        // Check if registration is enabled
        // For custom providers, check if they allow registration even if global registration is disabled
        $allowRegistration = $this->config['registration']['enabled'];
        if (!$allowRegistration && $providerConfig && isset($providerConfig['allow_registration'])) {
            $allowRegistration = $providerConfig['allow_registration'];
        }
        
        if (!$allowRegistration) {
            throw new Exception('Registration is disabled');
        }

        $userId = null;
        
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
        
        // Convert any pending shares for this email address
        if (!is_null($userId) && $this->fileManager) {
            try {
                $this->fileManager->convertPendingSharesForNewUser($userInfo['email'], $userId);
            } catch (Exception $e) {
                // Log error but don't fail registration
                error_log("Failed to convert pending shares for OAuth2 user {$userInfo['email']}: " . $e->getMessage());
            }
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
    
    // Invite System Methods
    
    public function canCreateInvites($userEmail) {
        $inviteDomains = $this->config['registration']['invite_domains'];
        
        // If no invite domains configured, nobody can create invites
        if (empty($inviteDomains)) {
            return false;
        }
        
        $domain = substr(strrchr($userEmail, '@'), 1);
        return in_array($domain, $inviteDomains);
    }
    
    public function createInvite($creatorUserId, $email = null) {
        // Validate that creator can create invites
        $creator = $this->db->fetchOne('SELECT email FROM users WHERE id = ?', [$creatorUserId]);
        if (!$creator) {
            throw new Exception('Creator user not found');
        }
        
        if (!$this->canCreateInvites($creator['email'])) {
            throw new Exception('You are not authorized to create invites');
        }
        
        // Generate secure token
        $token = bin2hex(random_bytes(32));
        
        // Set expiration to 1 month from now
        $expiresAt = date('Y-m-d H:i:s', strtotime('+1 month'));
        
        // Insert invite
        $this->db->execute(
            'INSERT INTO invites (token, created_by_user_id, email, expires_at) VALUES (?, ?, ?, ?)',
            [$token, $creatorUserId, $email, $expiresAt]
        );
        
        return [
            'token' => $token,
            'expires_at' => $expiresAt,
            'email' => $email
        ];
    }
    
    public function validateInviteToken($token, $email = null) {
        $invite = $this->db->fetchOne(
            'SELECT id, email, expires_at, used_at FROM invites WHERE token = ?',
            [$token]
        );
        
        if (!$invite) {
            return false;
        }
        
        // Check if already used
        if ($invite['used_at']) {
            return false;
        }
        
        // Check if expired
        if (strtotime($invite['expires_at']) < time()) {
            return false;
        }
        
        // Check if invite is for specific email
        if ($invite['email'] && $invite['email'] !== $email) {
            return false;
        }
        
        return true;
    }
    
    public function markInviteAsUsed($token, $userId) {
        $this->db->execute(
            'UPDATE invites SET used_by_user_id = ?, used_at = CURRENT_TIMESTAMP WHERE token = ?',
            [$userId, $token]
        );
    }
    
    public function getUserInvites($userId) {
        return $this->db->fetchAll(
            'SELECT id, token, email, expires_at, used_at, created_at,
                    (SELECT name FROM users WHERE id = used_by_user_id) as used_by_name
             FROM invites 
             WHERE created_by_user_id = ? 
             ORDER BY created_at DESC',
            [$userId]
        );
    }
    
    public function revokeInvite($inviteId, $userId) {
        $invite = $this->db->fetchOne(
            'SELECT created_by_user_id, used_at FROM invites WHERE id = ?',
            [$inviteId]
        );
        
        if (!$invite) {
            throw new Exception('Invite not found');
        }
        
        if ($invite['created_by_user_id'] != $userId) {
            throw new Exception('You can only revoke your own invites');
        }
        
        if ($invite['used_at']) {
            throw new Exception('Cannot revoke an invite that has already been used');
        }
        
        $this->db->execute('DELETE FROM invites WHERE id = ?', [$inviteId]);
    }
    
    public function cleanExpiredInvites() {
        $this->db->execute('DELETE FROM invites WHERE expires_at < CURRENT_TIMESTAMP');
    }
}
