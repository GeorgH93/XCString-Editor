class XCStringEditor {
    constructor() {
        this.data = null;
        this.currentUser = null;
        this.currentFileId = null;
        this.isModified = false;
        this.config = null;
        this.aiConfig = null;
        this.selectedAIProvider = null;
        this.selectedAIModel = null;
        
        this.initializeElements();
        this.setupEventListeners();
        this.checkAuthStatus();
    }

    initializeElements() {
        // Original elements
        this.uploadArea = document.getElementById('uploadArea');
        this.fileInput = document.getElementById('fileInput');
        this.editorSection = document.getElementById('editorSection');
        this.stringsContainer = document.getElementById('stringsContainer');
        this.addStringBtn = document.getElementById('addStringBtn');
        this.exportBtn = document.getElementById('exportBtn');
        
        // New elements
        this.authSection = document.getElementById('authSection');
        this.authModal = document.getElementById('authModal');
        this.closeModal = document.getElementById('closeModal');
        this.loginForm = document.getElementById('loginForm');
        this.registerForm = document.getElementById('registerForm');
        this.fileManagementSection = document.getElementById('fileManagementSection');
        this.uploadSection = document.getElementById('uploadSection');
        this.saveBtn = document.getElementById('saveBtn');
        this.shareBtn = document.getElementById('shareBtn');
        this.newFileBtn = document.getElementById('newFileBtn');
        this.editorTitle = document.getElementById('editorTitle');
        this.fileInfo = document.getElementById('fileInfo');
        
        // Progress elements
        this.progressSection = document.getElementById('progressSection');
        this.progressIndicators = document.getElementById('progressIndicators');
        
        // Filter elements
        this.filterSection = document.getElementById('filterSection');
        this.filterLanguage = document.getElementById('filterLanguage');
        this.filterStatus = document.getElementById('filterStatus');
        this.clearFilterBtn = document.getElementById('clearFilterBtn');
        
        // AI elements
        this.aiSection = document.getElementById('aiSection');
        this.aiProvider = document.getElementById('aiProvider');
        this.aiModel = document.getElementById('aiModel');
        this.translateMissingBtn = document.getElementById('translateMissingBtn');
        this.proofreadAllBtn = document.getElementById('proofreadAllBtn');
        
        // Notification and modal elements
        this.notifications = document.getElementById('notifications');
        this.confirmModal = document.getElementById('confirmModal');
        this.confirmTitle = document.getElementById('confirmTitle');
        this.confirmMessage = document.getElementById('confirmMessage');
        this.confirmOk = document.getElementById('confirmOk');
        this.confirmCancel = document.getElementById('confirmCancel');
        this.inputModal = document.getElementById('inputModal');
        this.inputTitle = document.getElementById('inputTitle');
        this.inputLabel = document.getElementById('inputLabel');
        this.inputField = document.getElementById('inputField');
        this.inputForm = document.getElementById('inputForm');
        this.inputCancel = document.getElementById('inputCancel');
        this.closeInputModal = document.getElementById('closeInputModal');
        
        // Status bar elements
        this.statusBar = document.getElementById('statusBar');
        this.statusAIOperation = document.getElementById('statusAIOperation');
        this.statusAIText = document.getElementById('statusAIText');
        this.statusAIProgressFill = document.getElementById('statusAIProgressFill');
        this.statusTotalStrings = document.getElementById('statusTotalStrings');
        this.statusIncompleteStrings = document.getElementById('statusIncompleteStrings');
        this.statusTopBtn = document.getElementById('statusTopBtn');
        this.statusBottomBtn = document.getElementById('statusBottomBtn');
        this.statusNextBtn = document.getElementById('statusNextBtn');
        
        // Version history elements
        this.versionHistoryBtn = document.getElementById('versionHistoryBtn');
        this.versionHistoryModal = document.getElementById('versionHistoryModal');
        this.closeVersionHistoryModal = document.getElementById('closeVersionHistoryModal');
        this.closeVersionHistory = document.getElementById('closeVersionHistory');
        this.versionHistoryTitle = document.getElementById('versionHistoryTitle');
        this.versionStats = document.getElementById('versionStats');
        this.versionCount = document.getElementById('versionCount');
        this.versionContributors = document.getElementById('versionContributors');
        this.versionSize = document.getElementById('versionSize');
        this.versionList = document.getElementById('versionList');
        
        // Upload version elements
        this.uploadVersionBtn = document.getElementById('uploadVersionBtn');
        this.uploadVersionModal = document.getElementById('uploadVersionModal');
        this.closeUploadVersionModal = document.getElementById('closeUploadVersionModal');
        this.uploadVersionForm = document.getElementById('uploadVersionForm');
        this.uploadVersionCancel = document.getElementById('uploadVersionCancel');
        
        // Manage shares elements
        this.manageSharesBtn = document.getElementById('manageSharesBtn');
        this.manageSharesModal = document.getElementById('manageSharesModal');
        this.closeManageSharesModal = document.getElementById('closeManageSharesModal');
        this.closeManageShares = document.getElementById('closeManageShares');
        this.manageSharesTitle = document.getElementById('manageSharesTitle');
        this.currentSharesList = document.getElementById('currentSharesList');
        this.pendingSharesList = document.getElementById('pendingSharesList');
        this.addShareForm = document.getElementById('addShareForm');
        this.shareEmail = document.getElementById('shareEmail');
        this.shareCanEdit = document.getElementById('shareCanEdit');
        
        // Invite elements
        this.invitesTab = document.getElementById('invitesTab');
        this.createInviteBtn = document.getElementById('createInviteBtn');
        this.invitesList = document.getElementById('invitesList');
        this.inviteTokenField = document.getElementById('inviteTokenField');
        this.versionFile = document.getElementById('versionFile');
        this.versionComment = document.getElementById('versionComment');
        this.uploadFileInfo = document.getElementById('uploadFileInfo');
        this.uploadProgress = document.getElementById('uploadProgress');
        this.uploadProgressFill = document.getElementById('uploadProgressFill');
        this.uploadProgressText = document.getElementById('uploadProgressText');
        
        // Presigned URL elements
        this.generateUploadUrlBtn = document.getElementById('generateUploadUrlBtn');
        this.presignedUrlModal = document.getElementById('presignedUrlModal');
        this.closePresignedUrlModal = document.getElementById('closePresignedUrlModal');
        this.closePresignedUrl = document.getElementById('closePresignedUrl');
        this.urlCommentPrefix = document.getElementById('urlCommentPrefix');
        this.generateUrlBtn = document.getElementById('generateUrlBtn');
        this.generatedUrlSection = document.getElementById('generatedUrlSection');
        this.generatedUrl = document.getElementById('generatedUrl');
        this.copyUrlBtn = document.getElementById('copyUrlBtn');
        this.urlExpiration = document.getElementById('urlExpiration');
        this.curlExample = document.getElementById('curlExample');
        this.copyCurlBtn = document.getElementById('copyCurlBtn');
        this.existingUrlsSection = document.getElementById('existingUrlsSection');
        this.existingUrlsList = document.getElementById('existingUrlsList');
    }

    setupEventListeners() {
        // Original listeners
        this.uploadArea.addEventListener('click', () => this.fileInput.click());
        this.uploadArea.addEventListener('dragover', this.handleDragOver.bind(this));
        this.uploadArea.addEventListener('dragleave', this.handleDragLeave.bind(this));
        this.uploadArea.addEventListener('drop', this.handleDrop.bind(this));
        this.fileInput.addEventListener('change', this.handleFileSelect.bind(this));
        this.addStringBtn.addEventListener('click', async () => await this.addNewString());
        this.exportBtn.addEventListener('click', this.exportFile.bind(this));
        
        // Auth listeners
        this.closeModal.addEventListener('click', () => this.hideAuthModal());
        document.getElementById('showRegister').addEventListener('click', (e) => {
            e.preventDefault();
            this.showRegisterForm();
        });
        document.getElementById('showLogin').addEventListener('click', (e) => {
            e.preventDefault();
            this.showLoginForm();
        });
        document.getElementById('loginFormElement').addEventListener('submit', this.handleLogin.bind(this));
        document.getElementById('registerFormElement').addEventListener('submit', this.handleRegister.bind(this));
        
        // File management listeners
        this.saveBtn.addEventListener('click', this.saveCurrentFile.bind(this));
        this.shareBtn.addEventListener('click', this.shareCurrentFile.bind(this));
        this.newFileBtn.addEventListener('click', this.createNewFile.bind(this));
        
        // Tab listeners
        document.querySelectorAll('.tab-button').forEach(button => {
            button.addEventListener('click', () => this.switchTab(button.dataset.tab));
        });
        
        // Modal background click
        this.authModal.addEventListener('click', (e) => {
            if (e.target === this.authModal) {
                this.hideAuthModal();
            }
        });
        
        // Confirmation modal listeners
        this.confirmCancel.addEventListener('click', () => this.hideConfirmModal());
        this.confirmModal.addEventListener('click', (e) => {
            if (e.target === this.confirmModal) {
                this.hideConfirmModal();
            }
        });
        
        // Input modal listeners
        this.inputCancel.addEventListener('click', () => this.hideInputModal());
        this.closeInputModal.addEventListener('click', () => this.hideInputModal());
        this.inputModal.addEventListener('click', (e) => {
            if (e.target === this.inputModal) {
                this.hideInputModal();
            }
        });
        
        // Filter listeners
        this.filterLanguage.addEventListener('change', () => {
            this.applyFilter();
        });
        
        this.clearFilterBtn.addEventListener('click', () => {
            this.clearFilter();
        });
        
        // AI listeners
        this.aiProvider.addEventListener('change', () => {
            this.updateAIModels();
        });
        
        this.translateMissingBtn.addEventListener('click', () => {
            this.translateMissingLocalizations();
        });
        
        this.proofreadAllBtn.addEventListener('click', () => {
            this.proofreadAllLocalizations();
        });
        
        // Version history listeners
        this.versionHistoryBtn.addEventListener('click', () => {
            this.showVersionHistory();
        });
        
        this.closeVersionHistoryModal.addEventListener('click', () => {
            this.hideVersionHistory();
        });
        
        this.closeVersionHistory.addEventListener('click', () => {
            this.hideVersionHistory();
        });
        
        this.versionHistoryModal.addEventListener('click', (e) => {
            if (e.target === this.versionHistoryModal) {
                this.hideVersionHistory();
            }
        });
        
        // Upload version listeners
        this.uploadVersionBtn.addEventListener('click', () => {
            this.showUploadVersionModal();
        });
        
        this.closeUploadVersionModal.addEventListener('click', () => {
            this.hideUploadVersionModal();
        });
        
        this.uploadVersionCancel.addEventListener('click', () => {
            this.hideUploadVersionModal();
        });
        
        this.uploadVersionModal.addEventListener('click', (e) => {
            if (e.target === this.uploadVersionModal) {
                this.hideUploadVersionModal();
            }
        });
        
        this.uploadVersionForm.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleVersionUpload();
        });
        
        this.versionFile.addEventListener('change', (e) => {
            this.validateUploadFile(e.target.files[0]);
        });
        
        // Manage shares listeners
        this.manageSharesBtn.addEventListener('click', () => {
            this.showManageSharesModal();
        });
        
        this.closeManageSharesModal.addEventListener('click', () => {
            this.hideManageSharesModal();
        });
        
        this.closeManageShares.addEventListener('click', () => {
            this.hideManageSharesModal();
        });
        
        this.manageSharesModal.addEventListener('click', (e) => {
            if (e.target === this.manageSharesModal) {
                this.hideManageSharesModal();
            }
        });
        
        this.addShareForm.addEventListener('submit', (e) => {
            e.preventDefault();
            this.addNewShare();
        });
        
        // Presigned URL listeners
        this.generateUploadUrlBtn.addEventListener('click', () => {
            this.showPresignedUrlModal();
        });
        
        this.closePresignedUrlModal.addEventListener('click', () => {
            this.hidePresignedUrlModal();
        });
        
        this.closePresignedUrl.addEventListener('click', () => {
            this.hidePresignedUrlModal();
        });
        
        this.presignedUrlModal.addEventListener('click', (e) => {
            if (e.target === this.presignedUrlModal) {
                this.hidePresignedUrlModal();
            }
        });
        
        this.generateUrlBtn.addEventListener('click', () => {
            this.generatePresignedUrl();
        });
        
        this.copyUrlBtn.addEventListener('click', () => {
            this.copyToClipboard(this.generatedUrl.value);
        });
        
        this.copyCurlBtn.addEventListener('click', () => {
            this.copyToClipboard(this.curlExample.textContent);
        });
        
        // Invite listeners
        this.createInviteBtn.addEventListener('click', () => {
            this.createInvite();
        });
        
        // Status bar listeners
        this.statusTopBtn.addEventListener('click', () => {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
        
        this.statusBottomBtn.addEventListener('click', () => {
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        });
        
        this.statusNextBtn.addEventListener('click', () => {
            this.navigateToNextIncomplete();
        });
    }

    async checkAuthStatus() {
        try {
            const response = await fetch('/backend/index.php/auth/user');
            const result = await response.json();
            
            if (result.success) {
                this.currentUser = result.user;
                this.config = result.config;
                this.aiConfig = result.config;
                this.updateAuthUI();
                this.updateOAuth2UI();
                this.updateRegistrationUI();
                this.updateInviteUI();
                this.updateAIUI();
                if (this.currentUser) {
                    this.showFileManagement();
                    this.loadUserFiles();
                }
            }
            
            // Check for OAuth2 callback results
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.has('oauth_success')) {
                // OAuth2 login successful, refresh page to update UI
                window.location.href = window.location.pathname;
            } else if (urlParams.has('oauth_error')) {
                this.showNotification('OAuth2 login failed: ' + urlParams.get('oauth_error'), 'error');
                // Clear the error from URL
                window.history.replaceState({}, document.title, window.location.pathname);
            }
        } catch (error) {
            console.error('Auth check failed:', error);
        }
    }

    updateAuthUI() {
        if (this.currentUser) {
            const avatar = this.currentUser.avatar_url ? 
                `<img src="${this.currentUser.avatar_url}" alt="Avatar" class="user-avatar">` : '';
            
            this.authSection.innerHTML = `
                <div class="user-info">
                    ${avatar}
                    <span class="user-name">${this.currentUser.name}</span>
                    <button class="btn btn-secondary btn-sm" onclick="editor.logout()">Logout</button>
                </div>
            `;
        } else {
            const registerBtn = this.config?.registration_enabled ? 
                '<button class="btn btn-secondary" onclick="editor.showAuthModal(\'register\')">Register</button>' : '';
            
            this.authSection.innerHTML = `
                <button class="btn btn-primary" onclick="editor.showAuthModal('login')">Login</button>
                ${registerBtn}
            `;
        }
    }
    
    updateOAuth2UI() {
        if (!this.config?.oauth2_enabled || !this.config?.oauth2_providers?.length) {
            return;
        }
        
        const loginOAuth2Section = document.getElementById('loginOAuth2Section');
        const registerOAuth2Section = document.getElementById('registerOAuth2Section');
        const loginOAuth2Buttons = document.getElementById('loginOAuth2Buttons');
        const registerOAuth2Buttons = document.getElementById('registerOAuth2Buttons');
        
        if (!loginOAuth2Section || !registerOAuth2Section) {
            return;
        }
        
        const oauth2ButtonsHtml = this.config.oauth2_providers.map(provider => {
            const providerName = this.getProviderDisplayName(provider);
            const providerIcon = this.getProviderIcon(provider);
            const providerKey = provider.name || provider; // Handle both object and string formats
            
            return `
                <a href="/backend/index.php/auth/oauth/${providerKey}/redirect" class="oauth2-btn ${providerKey}">
                    ${providerIcon}
                    Continue with ${providerName}
                </a>
            `;
        }).join('');
        
        loginOAuth2Buttons.innerHTML = oauth2ButtonsHtml;
        registerOAuth2Buttons.innerHTML = oauth2ButtonsHtml;
        
        loginOAuth2Section.style.display = 'block';
        registerOAuth2Section.style.display = 'block';
    }
    
    updateRegistrationUI() {
        const showRegisterLink = document.getElementById('showRegister');
        const urlParams = new URLSearchParams(window.location.search);
        const inviteToken = urlParams.get('invite');
        
        if (showRegisterLink) {
            const registerParagraph = showRegisterLink.closest('p');
            if (this.config?.registration_enabled || inviteToken) {
                // Show register link if registration is enabled OR if there's an invite token
                if (registerParagraph) registerParagraph.style.display = 'block';
                // Hide invite token field when registration is enabled (but not when using invite)
                if (this.inviteTokenField) {
                    this.inviteTokenField.style.display = this.config?.registration_enabled && !inviteToken ? 'none' : 'block';
                }
            } else {
                // Hide register link when registration is disabled and no invite token
                if (registerParagraph) registerParagraph.style.display = 'none';
                // Show invite token field when registration is disabled
                if (this.inviteTokenField) this.inviteTokenField.style.display = 'block';
            }
        }
        
        // Check URL parameters for invite token and auto-open registration
        if (inviteToken) {
            const inviteTokenInput = document.getElementById('registerInviteToken');
            if (inviteTokenInput) {
                inviteTokenInput.value = inviteToken;
            }
            
            // If user is not logged in and has an invite token, automatically show registration dialog
            if (!this.currentUser) {
                this.showAuthModal('register');
                
                // Clear the invite parameter from the URL after processing
                const newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
                window.history.replaceState({}, document.title, newUrl);
            }
        }
    }
    
    getProviderDisplayName(provider) {
        // Handle both old format (string) and new format (object)
        if (typeof provider === 'string') {
            const names = {
                'google': 'Google',
                'github': 'GitHub',
                'microsoft': 'Microsoft',
                'gitlab': 'GitLab'
            };
            return names[provider] || provider;
        }
        
        // New format with display_name property
        return provider.display_name || provider.name || 'Unknown Provider';
    }
    
    getProviderIcon(provider) {
        // Handle both old format (string) and new format (object)
        if (typeof provider === 'object' && provider.icon_svg) {
            // Custom provider with custom icon
            return provider.icon_svg;
        }
        
        // Get provider name for built-in icons
        const providerName = typeof provider === 'string' ? provider : provider.name;
        
        const icons = {
            'google': `<svg viewBox="0 0 24 24" width="20" height="20">
                <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>`,
            'github': `<svg viewBox="0 0 24 24" width="20" height="20">
                <path fill="currentColor" d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
            </svg>`,
            'microsoft': `<svg viewBox="0 0 24 24" width="20" height="20">
                <path fill="currentColor" d="M0 0h11.377v11.372H0V0zm12.623 0H24v11.372H12.623V0zM0 12.623h11.377V24H0V12.623zm12.623 0H24V24H12.623V12.623z"/>
            </svg>`,
            'gitlab': `<svg viewBox="0 0 24 24" width="20" height="20">
                <path fill="currentColor" d="M23.955 13.587l-1.342-4.135-2.664-8.189c-.135-.423-.73-.423-.867 0L16.418 9.45H7.582L4.918 1.263c-.135-.423-.73-.423-.867 0L1.387 9.452.045 13.587c-.121.375.014.789.331 1.023L12 23.054l11.624-8.443c.318-.235.452-.648.331-1.024"/>
            </svg>`
        };
        
        return icons[providerName] || '<svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>';
    }

    showAuthModal(mode = 'login') {
        this.authModal.style.display = 'block';
        if (mode === 'register') {
            this.showRegisterForm();
        } else {
            this.showLoginForm();
        }
    }

    hideAuthModal() {
        this.authModal.style.display = 'none';
    }

    showLoginForm() {
        this.loginForm.style.display = 'block';
        this.registerForm.style.display = 'none';
    }

    showRegisterForm() {
        this.loginForm.style.display = 'none';
        this.registerForm.style.display = 'block';
    }

    async handleLogin(e) {
        e.preventDefault();
        const email = document.getElementById('loginEmail').value;
        const password = document.getElementById('loginPassword').value;
        
        try {
            const response = await fetch('/backend/index.php/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            
            const result = await response.json();
            if (result.success) {
                this.currentUser = result.user;
                this.hideAuthModal();
                this.updateAuthUI();
                this.showFileManagement();
                this.loadUserFiles();
            } else {
                this.showNotification('Login failed: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Login error: ' + error.message, 'error');
        }
    }

    async handleRegister(e) {
        e.preventDefault();
        const name = document.getElementById('registerName').value;
        const email = document.getElementById('registerEmail').value;
        const password = document.getElementById('registerPassword').value;
        const inviteToken = document.getElementById('registerInviteToken').value;
        
        const payload = { name, email, password };
        if (inviteToken) {
            payload.invite_token = inviteToken;
        }
        
        try {
            const response = await fetch('/backend/index.php/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            
            const result = await response.json();
            if (result.success) {
                this.showNotification('Registration successful! Please login.', 'success');
                this.showLoginForm();
            } else {
                this.showNotification('Registration failed: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Registration error: ' + error.message, 'error');
        }
    }

    async logout() {
        try {
            await fetch('/backend/index.php/auth/logout', { method: 'POST' });
            this.currentUser = null;
            this.currentFileId = null;
            this.updateAuthUI();
            this.hideFileManagement();
            this.hideEditor();
        } catch (error) {
            console.error('Logout error:', error);
        }
    }

    showFileManagement() {
        this.fileManagementSection.style.display = 'block';
        this.uploadSection.style.display = 'block';
    }

    hideFileManagement() {
        this.fileManagementSection.style.display = 'none';
        this.uploadSection.style.display = 'block';
    }

    hideEditor() {
        this.editorSection.style.display = 'none';
        this.progressSection.style.display = 'none';
        this.uploadSection.style.display = 'block';
        this.data = null;
        this.isModified = false;
    }

    switchTab(tabName) {
        // Update tab buttons
        document.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));
        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
        
        // Update tab panes
        document.querySelectorAll('.tab-pane').forEach(pane => pane.classList.remove('active'));
        document.getElementById(tabName).classList.add('active');
        
        // Load appropriate data
        switch (tabName) {
            case 'my-files':
                this.loadUserFiles();
                break;
            case 'shared-files':
                this.loadSharedFiles();
                break;
            case 'public-files':
                this.loadPublicFiles();
                break;
            case 'invites':
                this.loadUserInvites();
                break;
        }
    }

    async loadUserFiles() {
        try {
            const response = await fetch('/backend/index.php/files/my');
            const result = await response.json();
            
            if (result.success) {
                this.renderFilesList(result.files, 'myFilesList', true);
            }
        } catch (error) {
            console.error('Failed to load user files:', error);
        }
    }

    async loadSharedFiles() {
        try {
            const response = await fetch('/backend/index.php/files/shared');
            const result = await response.json();
            
            if (result.success) {
                this.renderFilesList(result.files, 'sharedFilesList', false);
            }
        } catch (error) {
            console.error('Failed to load shared files:', error);
        }
    }

    async loadPublicFiles() {
        try {
            const response = await fetch('/backend/index.php/files/public');
            const result = await response.json();
            
            if (result.success) {
                this.renderFilesList(result.files, 'publicFilesList', false);
            }
        } catch (error) {
            console.error('Failed to load public files:', error);
        }
    }

    renderFilesList(files, containerId, isOwner) {
        const container = document.getElementById(containerId);
        
        if (files.length === 0) {
            container.innerHTML = '<p class="empty-state">No files found</p>';
            return;
        }
        
        container.innerHTML = files.map(file => `
            <div class="file-item" onclick="editor.loadFile(${file.id})">
                <div class="file-item-info">
                    <div class="file-item-name">${file.name}</div>
                    <div class="file-item-meta">
                        ${file.owner_name ? `by ${file.owner_name} • ` : ''}
                        Updated: ${new Date(file.updated_at).toLocaleDateString()}
                        ${file.can_edit !== undefined ? (file.can_edit ? ' • Can Edit' : ' • Read Only') : ''}
                    </div>
                </div>
                <div class="file-item-actions">
                    ${isOwner ? `<button class="btn btn-danger btn-sm" onclick="event.stopPropagation(); editor.deleteFile(${file.id})">Delete</button>` : ''}
                </div>
            </div>
        `).join('');
    }

    async loadFile(fileId) {
        try {
            const response = await fetch(`/backend/index.php/files/${fileId}`);
            const result = await response.json();
            
            if (result.success) {
                this.data = this.fixParsedData(JSON.parse(result.file.content));
                this.currentFileId = fileId;
                this.isModified = false;
                this.editorTitle.textContent = result.file.name;
                this.fileInfo.textContent = `by ${result.file.owner_name} • Updated: ${new Date(result.file.updated_at).toLocaleDateString()}`;
                
                // Show appropriate buttons
                this.saveBtn.style.display = this.currentUser ? 'inline-block' : 'none';
                this.shareBtn.style.display = this.currentUser ? 'inline-block' : 'none';
                this.manageSharesBtn.style.display = this.currentUser ? 'inline-block' : 'none';
                this.versionHistoryBtn.style.display = this.currentUser && this.currentFileId ? 'inline-block' : 'none';
                
                this.renderEditor();
            } else {
                this.showNotification('Failed to load file: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error loading file: ' + error.message, 'error');
        }
    }

    async deleteFile(fileId) {
        const shouldDelete = await this.showConfirmDialog(
            'Delete File', 
            'Are you sure you want to delete this file? This action cannot be undone.'
        );
        if (!shouldDelete) return;
        
        try {
            const response = await fetch(`/backend/index.php/files/${fileId}`, {
                method: 'DELETE'
            });
            
            const result = await response.json();
            if (result.success) {
                this.loadUserFiles(); // Refresh the list
                if (this.currentFileId === fileId) {
                    this.hideEditor();
                }
            } else {
                this.showNotification('Failed to delete file: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error deleting file: ' + error.message, 'error');
        }
    }

    createNewFile() {
        this.data = {
            sourceLanguage: 'en',
            strings: {},
            version: '1.0'
        };
        this.currentFileId = null;
        this.isModified = true;
        this.editorTitle.textContent = 'New File';
        this.fileInfo.textContent = 'Unsaved';
        
        this.saveBtn.style.display = 'inline-block';
        this.shareBtn.style.display = 'none';
        this.manageSharesBtn.style.display = 'none';
        this.versionHistoryBtn.style.display = 'none';
        
        this.renderEditor();
    }

    async saveCurrentFile() {
        if (!this.currentUser || !this.data) return;
        
        let fileName;
        if (this.currentFileId) {
            fileName = this.editorTitle.textContent;
        } else {
            fileName = await this.showInputDialog(
                'Save File', 
                'Enter file name:', 
                'Untitled.xcstrings'
            );
        }
        
        if (!fileName) return;
        
        try {
            // Clean AI review data before saving
            const cleanData = this.cleanDataForExport(this.data);
            const content = JSON.stringify(cleanData);
            let response;
            
            if (this.currentFileId) {
                // Update existing file
                response = await fetch('/backend/index.php/files/update', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        file_id: this.currentFileId,
                        content: content
                    })
                });
            } else {
                // Save new file
                response = await fetch('/backend/index.php/files/save', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: fileName,
                        content: content,
                        is_public: false
                    })
                });
            }
            
            const result = await response.json();
            if (result.success) {
                if (!this.currentFileId) {
                    this.currentFileId = result.file_id;
                    this.editorTitle.textContent = fileName;
                }
                this.isModified = false;
                this.fileInfo.textContent = 'Saved';
                this.shareBtn.style.display = 'inline-block';
                this.manageSharesBtn.style.display = 'inline-block';
                this.loadUserFiles(); // Refresh file list
            } else {
                this.showNotification('Failed to save file: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error saving file: ' + error.message, 'error');
        }
    }

    async saveUploadedFile(fileName, content) {
        if (!this.currentUser || !this.data) return;
        
        try {
            // Clean AI review data before saving
            const cleanData = this.cleanDataForExport(this.data);
            
            const response = await fetch('/backend/index.php/files/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: fileName,
                    content: JSON.stringify(cleanData),
                    is_public: false
                })
            });
            
            const result = await response.json();
            if (result.success) {
                this.currentFileId = result.file_id;
                this.fileInfo.textContent = 'Saved';
                this.saveBtn.style.display = 'inline-block';
                this.shareBtn.style.display = 'inline-block';
                this.manageSharesBtn.style.display = 'inline-block';
                this.loadUserFiles(); // Refresh file list
                this.showNotification('File saved successfully!', 'success');
            } else {
                this.showNotification('Failed to save file: ' + result.error, 'error');
                this.fileInfo.textContent = 'Uploaded file (not saved)';
                this.saveBtn.style.display = 'inline-block';
                this.shareBtn.style.display = 'none';
                this.manageSharesBtn.style.display = 'none';
            }
        } catch (error) {
            this.showNotification('Error saving file: ' + error.message, 'error');
            this.fileInfo.textContent = 'Uploaded file (not saved)';
            this.saveBtn.style.display = 'inline-block';
            this.shareBtn.style.display = 'none';
            this.manageSharesBtn.style.display = 'none';
        }
    }

    async shareCurrentFile() {
        if (!this.currentFileId) return;
        
        const email = await this.showInputDialog(
            'Share File', 
            'Enter email address to share with:'
        );
        if (!email) return;
        
        const canEdit = await this.showConfirmDialog(
            'Share Permissions', 
            `Share "${this.editorTitle.textContent}" with ${email}?\n\nClick "OK" to allow editing, or "Cancel" for read-only access.`
        );
        
        try {
            const response = await fetch('/backend/index.php/files/share', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    file_id: this.currentFileId,
                    email: email,
                    can_edit: canEdit
                })
            });
            
            const result = await response.json();
            if (result.success) {
                this.showNotification('File shared successfully!', 'success');
            } else {
                this.showNotification('Failed to share file: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error sharing file: ' + error.message, 'error');
        }
    }

    // Original methods (updated for new UI)
    handleDragOver(e) {
        e.preventDefault();
        this.uploadArea.classList.add('dragover');
    }

    handleDragLeave(e) {
        e.preventDefault();
        this.uploadArea.classList.remove('dragover');
    }

    handleDrop(e) {
        e.preventDefault();
        this.uploadArea.classList.remove('dragover');
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            this.processFile(files[0]);
        }
    }

    handleFileSelect(e) {
        const files = e.target.files;
        if (files.length > 0) {
            this.processFile(files[0]);
        }
    }

    async processFile(file) {
        try {
            const content = await this.readFile(file);
            const response = await fetch('/backend/index.php/parse', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content })
            });

            const result = await response.json();
            if (result.success) {
                this.data = this.fixParsedData(result.data);
                this.currentFileId = null;
                this.isModified = false;
                this.editorTitle.textContent = file.name;
                
                // For authenticated users, offer to save immediately
                if (this.currentUser) {
                    const shouldSave = await this.showConfirmDialog(
                        'Save File', 
                        'Save this file to your account?'
                    );
                    if (shouldSave) {
                        await this.saveUploadedFile(file.name, content);
                    } else {
                        this.fileInfo.textContent = 'Uploaded file (not saved)';
                        this.saveBtn.style.display = 'inline-block';
                        this.shareBtn.style.display = 'none';
                        this.manageSharesBtn.style.display = 'none';
                    }
                } else {
                    this.fileInfo.textContent = 'Uploaded file (not saved)';
                    this.saveBtn.style.display = 'none';
                    this.shareBtn.style.display = 'none';
                    this.manageSharesBtn.style.display = 'none';
                }
                
                this.renderEditor();
            } else {
                this.showNotification('Error parsing file: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error processing file: ' + error.message, 'error');
        }
    }

    readFile(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = e => resolve(e.target.result);
            reader.onerror = reject;
            reader.readAsText(file);
        });
    }

    renderEditor() {
        // Show editor and progress sections, hide upload section
        this.editorSection.style.display = 'block';
        this.progressSection.style.display = 'block';
        this.uploadSection.style.display = 'none';
        
        this.stringsContainer.innerHTML = '';

        if (this.data && this.data.strings) {
            Object.keys(this.data.strings).forEach(key => {
                this.renderStringEntry(key, this.data.strings[key]);
            });
            
            // Update progress indicators
            this.updateProgressIndicators();
            
            // Update status bar counts
            this.updateStatusBarCounts();
            
            // Reapply filter if one was active
            if (this.filterLanguage && this.filterLanguage.value) {
                // Small delay to ensure DOM is ready
                setTimeout(() => {
                    this.applyFilter();
                }, 50);
            }
        }
    }

    updateStringEntry(stringKey) {
        // Find and update existing entry instead of re-rendering everything
        const existingEntry = Array.from(document.querySelectorAll('.string-entry')).find(entry => {
            return entry.dataset.key === stringKey;
        });
        if (existingEntry) {
            const stringData = this.data.strings[stringKey];
            if (stringData) {
                // Store filter state
                const wasFilteredOut = existingEntry.classList.contains('filtered-out');
                const hasFocusedInput = existingEntry.classList.contains('has-focused-input');
                
                // Replace the entry
                const newEntry = this.createStringEntryElement(stringKey, stringData);
                
                // Restore filter state
                if (wasFilteredOut) newEntry.classList.add('filtered-out');
                if (hasFocusedInput) newEntry.classList.add('has-focused-input');
                
                existingEntry.replaceWith(newEntry);
                
                // Reapply focus monitoring
                this.setupFocusMonitoring(newEntry);
            }
        }
    }

    createStringEntryElement(key, stringData) {
        // Extract the string entry creation logic into a separate method
        const entryDiv = document.createElement('div');
        entryDiv.className = 'string-entry';
        entryDiv.dataset.key = key;

        const comment = stringData.comment || '';
        const localizations = stringData.localizations || {};

        // Create the structure using DOM methods (safer for special characters)
        const headerDiv = document.createElement('div');
        headerDiv.className = 'string-entry-header';
        
        const keyInput = document.createElement('input');
        keyInput.type = 'text';
        keyInput.className = 'string-key-input';
        keyInput.value = key;
        keyInput.placeholder = 'String key';
        
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-danger btn-sm delete-string-btn';
        deleteBtn.textContent = 'Delete';
        
        headerDiv.appendChild(keyInput);
        headerDiv.appendChild(deleteBtn);
        
        const detailsDiv = document.createElement('div');
        detailsDiv.className = 'string-details';
        
        const commentGroup = document.createElement('div');
        commentGroup.className = 'comment-group';
        
        const commentLabel = document.createElement('label');
        commentLabel.textContent = 'Comment';
        
        const commentInput = document.createElement('input');
        commentInput.type = 'text';
        commentInput.className = 'string-comment-input';
        commentInput.value = comment;
        commentInput.placeholder = 'comment';
        
        commentGroup.appendChild(commentLabel);
        commentGroup.appendChild(commentInput);
        
        const localizationsGroup = document.createElement('div');
        localizationsGroup.className = 'form-group';
        
        const localizationsLabel = document.createElement('label');
        localizationsLabel.textContent = 'Localizations:';
        
        const localizationsDiv = document.createElement('div');
        localizationsDiv.className = 'localizations';
        localizationsDiv.dataset.key = key;
        this.renderLocalizations(localizationsDiv, key, localizations);
        
        const addLocalizationBtn = document.createElement('button');
        addLocalizationBtn.className = 'btn btn-secondary add-localization-btn';
        addLocalizationBtn.textContent = 'Add Localization';
        
        const addVariationLocalizationBtn = document.createElement('button');
        addVariationLocalizationBtn.className = 'btn btn-secondary add-localization-btn';
        addVariationLocalizationBtn.textContent = 'Add with Variations';
        
        const buttonContainer = document.createElement('div');
        buttonContainer.style.display = 'flex';
        buttonContainer.style.gap = '10px';
        buttonContainer.style.marginTop = '10px';
        buttonContainer.appendChild(addLocalizationBtn);
        buttonContainer.appendChild(addVariationLocalizationBtn);
        
        localizationsGroup.appendChild(localizationsLabel);
        localizationsGroup.appendChild(localizationsDiv);
        localizationsGroup.appendChild(buttonContainer);
        
        detailsDiv.appendChild(commentGroup);
        detailsDiv.appendChild(localizationsGroup);
        
        entryDiv.appendChild(headerDiv);
        entryDiv.appendChild(detailsDiv);

        // Add event listeners
        keyInput.addEventListener('change', (e) => {
            this.updateStringKey(key, e.target.value);
        });

        deleteBtn.addEventListener('click', () => {
            this.deleteString(key);
        });

        commentInput.addEventListener('change', (e) => {
            this.updateStringComment(key, e.target.value);
        });

        addLocalizationBtn.addEventListener('click', async () => {
            await this.addLocalization(key);
        });

        addVariationLocalizationBtn.addEventListener('click', async () => {
            await this.addLocalizationWithVariations(key);
        });

        return entryDiv;
    }

    updateProgressIndicators() {
        if (!this.data || !this.data.strings) return;
        
        // Get all languages from all strings
        const languages = new Set();
        const sourceLanguage = this.data.sourceLanguage || 'en';
        
        Object.values(this.data.strings).forEach(stringData => {
            if (stringData.localizations) {
                Object.keys(stringData.localizations).forEach(lang => {
                    languages.add(lang);
                });
            }
        });
        
        // Calculate progress for each language
        const progressData = Array.from(languages).map(lang => {
            const totalStrings = Object.keys(this.data.strings).length;
            let translatedStrings = 0;
            
            if (lang === sourceLanguage) {
                // Source language is always 100%
                translatedStrings = totalStrings;
            } else {
                // Count translated strings for other languages
                Object.values(this.data.strings).forEach(stringData => {
                    if (stringData.localizations && stringData.localizations[lang]) {
                        const localization = stringData.localizations[lang];
                        
                        // Check if it's a simple localization or has variations
                        if (localization.stringUnit) {
                            // Simple localization - check if it has a value and is translated
                            if (localization.stringUnit.value && 
                                localization.stringUnit.value.trim() !== '' &&
                                localization.stringUnit.state === 'translated') {
                                translatedStrings++;
                            }
                        } else if (localization.variations) {
                            // Variations - check if at least one variation is translated
                            let hasTranslatedVariation = false;
                            Object.values(localization.variations).forEach(variationType => {
                                Object.values(variationType).forEach(variation => {
                                    if (variation.stringUnit && 
                                        variation.stringUnit.value && 
                                        variation.stringUnit.value.trim() !== '' &&
                                        variation.stringUnit.state === 'translated') {
                                        hasTranslatedVariation = true;
                                    }
                                });
                            });
                            if (hasTranslatedVariation) {
                                translatedStrings++;
                            }
                        }
                    }
                });
            }
            
            const percentage = totalStrings > 0 ? Math.round((translatedStrings / totalStrings) * 100) : 0;
            
            return {
                language: lang,
                percentage: percentage,
                translatedCount: translatedStrings,
                totalCount: totalStrings,
                isSource: lang === sourceLanguage
            };
        });
        
        // Sort by source language first, then by percentage descending
        progressData.sort((a, b) => {
            if (a.isSource) return -1;
            if (b.isSource) return 1;
            return b.percentage - a.percentage;
        });
        
        this.renderProgressIndicators(progressData);
        this.updateFilterOptions(progressData);
    }

    renderProgressIndicators(progressData) {
        this.progressIndicators.innerHTML = '';
        
        progressData.forEach(data => {
            const progressItem = document.createElement('div');
            progressItem.className = 'progress-item';
            
            const header = document.createElement('div');
            header.className = 'progress-item-header';
            
            const languageSpan = document.createElement('span');
            languageSpan.className = 'progress-language';
            languageSpan.textContent = data.isSource ? `${data.language} (source)` : data.language;
            
            const percentageSpan = document.createElement('span');
            percentageSpan.className = 'progress-percentage';
            percentageSpan.textContent = `${data.percentage}% (${data.translatedCount}/${data.totalCount})`;
            
            header.appendChild(languageSpan);
            header.appendChild(percentageSpan);
            
            const progressBar = document.createElement('div');
            progressBar.className = 'progress-bar';
            
            const progressFill = document.createElement('div');
            progressFill.className = 'progress-bar-fill';
            progressFill.style.width = `${data.percentage}%`;
            
            // Set color class based on percentage
            if (data.isSource) {
                progressFill.classList.add('source');
            } else if (data.percentage === 100) {
                progressFill.classList.add('complete');
            } else if (data.percentage >= 80) {
                progressFill.classList.add('high');
            } else if (data.percentage >= 50) {
                progressFill.classList.add('medium');
            } else {
                progressFill.classList.add('low');
            }
            
            progressBar.appendChild(progressFill);
            progressItem.appendChild(header);
            progressItem.appendChild(progressBar);
            
            this.progressIndicators.appendChild(progressItem);
        });
    }

    updateFilterOptions(progressData) {
        const currentValue = this.filterLanguage.value;
        
        // Keep track of static options that should be preserved
        const staticOptions = [
            { value: "", text: "No Filter" },
            { value: "any", text: "Any Language (missing translations)" },
            { value: "needs_review", text: "Needs Review" },
            { value: "new", text: "New (not yet translated)" },
            { value: "translated", text: "Translated" }
        ];
        
        // Clear all options first
        this.filterLanguage.innerHTML = '';
        
        // Re-add static options
        staticOptions.forEach(option => {
            const optionEl = document.createElement('option');
            optionEl.value = option.value;
            optionEl.textContent = option.text;
            this.filterLanguage.appendChild(optionEl);
        });
        
        // Add language-specific options (exclude source language)
        const sourceLanguage = this.data.sourceLanguage || 'en';
        progressData.forEach(data => {
            if (!data.isSource) {
                const option = document.createElement('option');
                option.value = data.language;
                option.textContent = `${data.language} (${data.percentage}% complete)`;
                this.filterLanguage.appendChild(option);
            }
        });
        
        // Restore previous selection if still valid
        if (currentValue && Array.from(this.filterLanguage.options).some(opt => opt.value === currentValue)) {
            this.filterLanguage.value = currentValue;
        }
    }

    applyFilter() {
        const filterValue = this.filterLanguage.value;
        
        if (!filterValue) {
            this.clearFilter();
            return;
        }
        
        let hiddenCount = 0;
        let totalCount = 0;
        
        document.querySelectorAll('.string-entry').forEach(entryDiv => {
            totalCount++;
            const stringKey = entryDiv.dataset.key;
            const shouldHide = this.shouldHideEntry(stringKey, filterValue);
            
            if (shouldHide) {
                entryDiv.classList.add('filtered-out');
                hiddenCount++;
            } else {
                entryDiv.classList.remove('filtered-out');
            }
            
            // Set up focus monitoring for auto-show
            this.setupFocusMonitoring(entryDiv);
        });
        
        // Update UI state
        this.updateFilterStatus(filterValue, hiddenCount, totalCount);
        this.clearFilterBtn.style.display = 'inline-block';
        this.filterStatus.classList.add('active');
    }

    shouldHideEntry(stringKey, filterLanguage) {
        const stringData = this.data.strings[stringKey];
        if (!stringData) return false;
        
        // Special handling for state-based filters when there are no meaningful localizations
        // Check if localizations is missing, empty, or only contains empty objects
        const hasNoMeaningfulLocalizations = !stringData.localizations || 
            Object.keys(stringData.localizations).length === 0 ||
            Object.values(stringData.localizations).every(loc => 
                !loc || Object.keys(loc).length === 0 || 
                (!loc.stringUnit && !loc.variations)
            );
            
            
        if (hasNoMeaningfulLocalizations) {
            if (filterLanguage === 'new') {
                return false; // Show entries without meaningful localizations (they are "new")
            } else if (filterLanguage === 'needs_review' || filterLanguage === 'translated') {
                return true; // Hide entries without meaningful localizations (they don't have these states)
            }
            return false; // For other filters, don't hide entries without meaningful localizations
        }
        
        const sourceLanguage = this.data.sourceLanguage || 'en';
        
        if (filterLanguage === 'any') {
            // Show entries that are missing translations in ANY language
            const allLanguages = new Set();
            Object.values(this.data.strings).forEach(str => {
                if (str.localizations) {
                    Object.keys(str.localizations).forEach(lang => allLanguages.add(lang));
                }
            });
            
            // Check if this string is missing in any non-source language
            for (const lang of allLanguages) {
                if (lang !== sourceLanguage) {
                    if (!this.isTranslated(stringData, lang)) {
                        return false; // Show this entry (it's incomplete)
                    }
                }
            }
            return true; // Hide this entry (it's complete in all languages)
        } else if (filterLanguage === 'needs_review') {
            // Show entries that have any localization with 'needs_review' state
            return !this.hasNeedsReviewState(stringData);
        } else if (filterLanguage === 'new') {
            // Show entries that have any localization with 'new' state
            return !this.hasNewState(stringData);
        } else if (filterLanguage === 'translated') {
            // Show entries that have any localization with 'translated' state
            return !this.hasTranslatedState(stringData);
        } else {
            // Show entries that are incomplete for specific language
            return this.isTranslated(stringData, filterLanguage);
        }
    }
    
    hasNeedsReviewState(stringData) {
        if (!stringData.localizations) return false;
        
        for (const [lang, localization] of Object.entries(stringData.localizations)) {
            if (localization.stringUnit && localization.stringUnit.state === 'needs_review') {
                return true;
            }
            
            // Check variations for needs_review state
            if (localization.variations) {
                for (const [variationType, variations] of Object.entries(localization.variations)) {
                    for (const [variationKey, variation] of Object.entries(variations)) {
                        if (variation.stringUnit && variation.stringUnit.state === 'needs_review') {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    hasNewState(stringData) {
        // Check if localizations is missing, empty, or only contains empty objects
        const hasNoMeaningfulLocalizations = !stringData.localizations || 
            Object.keys(stringData.localizations).length === 0 ||
            Object.values(stringData.localizations).every(loc => 
                !loc || Object.keys(loc).length === 0 || 
                (!loc.stringUnit && !loc.variations)
            );
            
        if (hasNoMeaningfulLocalizations) {
            return true; // Consider strings with no meaningful localizations as "new"
        }
        
        for (const [lang, localization] of Object.entries(stringData.localizations)) {
            if (localization.stringUnit && localization.stringUnit.state === 'new') {
                return true;
            }
            
            // Check variations for new state
            if (localization.variations) {
                for (const [variationType, variations] of Object.entries(localization.variations)) {
                    for (const [variationKey, variation] of Object.entries(variations)) {
                        if (variation.stringUnit && variation.stringUnit.state === 'new') {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    hasTranslatedState(stringData) {
        if (!stringData.localizations) return false;
        
        for (const [lang, localization] of Object.entries(stringData.localizations)) {
            if (localization.stringUnit && localization.stringUnit.state === 'translated') {
                return true;
            }
            
            // Check variations for translated state
            if (localization.variations) {
                for (const [variationType, variations] of Object.entries(localization.variations)) {
                    for (const [variationKey, variation] of Object.entries(variations)) {
                        if (variation.stringUnit && variation.stringUnit.state === 'translated') {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    isTranslated(stringData, language) {
        if (!stringData.localizations || !stringData.localizations[language]) {
            return false; // No localization exists
        }
        
        const localization = stringData.localizations[language];
        
        if (localization.stringUnit) {
            // Simple localization
            return localization.stringUnit.state === 'translated' && 
                   localization.stringUnit.value && 
                   localization.stringUnit.value.trim() !== '';
        } else if (localization.variations) {
            // Variations - check if at least one variation is translated
            let hasTranslation = false;
            Object.values(localization.variations).forEach(variationType => {
                Object.values(variationType).forEach(variation => {
                    if (variation.stringUnit && 
                        variation.stringUnit.state === 'translated' &&
                        variation.stringUnit.value && 
                        variation.stringUnit.value.trim() !== '') {
                        hasTranslation = true;
                    }
                });
            });
            return hasTranslation;
        }
        
        return false;
    }

    setupFocusMonitoring(entryDiv) {
        const inputs = entryDiv.querySelectorAll('input[type="text"], textarea');
        
        inputs.forEach(input => {
            input.addEventListener('focus', () => {
                entryDiv.classList.add('has-focused-input');
            });
            
            input.addEventListener('blur', () => {
                // Small delay to allow for focus to move to another input in the same entry
                setTimeout(() => {
                    const hasFocusedInput = Array.from(inputs).some(inp => inp === document.activeElement);
                    if (!hasFocusedInput) {
                        entryDiv.classList.remove('has-focused-input');
                    }
                }, 100);
            });
        });
    }

    updateFilterStatus(filterLanguage, hiddenCount, totalCount) {
        const visibleCount = totalCount - hiddenCount;
        let statusText = '';
        
        if (filterLanguage === 'any') {
            statusText = `Showing ${visibleCount} of ${totalCount} entries with incomplete translations`;
        } else if (filterLanguage === 'needs_review') {
            statusText = `Showing ${visibleCount} of ${totalCount} entries needing review`;
        } else if (filterLanguage === 'new') {
            statusText = `Showing ${visibleCount} of ${totalCount} entries with new state`;
        } else if (filterLanguage === 'translated') {
            statusText = `Showing ${visibleCount} of ${totalCount} entries with translated state`;
        } else {
            statusText = `Showing ${visibleCount} of ${totalCount} entries incomplete for ${filterLanguage}`;
        }
        
        this.filterStatus.textContent = statusText;
    }

    clearFilter() {
        this.filterLanguage.value = '';
        this.clearFilterBtn.style.display = 'none';
        this.filterStatus.textContent = '';
        this.filterStatus.classList.remove('active');
        
        // Show all entries
        document.querySelectorAll('.string-entry').forEach(entryDiv => {
            entryDiv.classList.remove('filtered-out', 'has-focused-input');
        });
    }

    renderStringEntry(key, stringData) {
        const entryDiv = this.createStringEntryElement(key, stringData);
        this.stringsContainer.appendChild(entryDiv);
    }

    renderLocalizations(container, stringKey, localizations) {
        // Clear existing content
        container.innerHTML = '';
        
        // Handle undefined or null localizations
        if (!localizations || typeof localizations !== 'object') {
            return;
        }
        
        Object.keys(localizations).forEach(lang => {
            const localization = localizations[lang];
            
            // Check if this localization has variations
            if (localization.variations) {
                this.renderVariationLocalization(container, stringKey, lang, localization);
            } else if (localization.stringUnit) {
                this.renderSimpleLocalization(container, stringKey, lang, localization);
            }
        });
    }

    renderSimpleLocalization(container, stringKey, lang, localization) {
        const value = localization.stringUnit ? localization.stringUnit.value : '';
        const state = localization.stringUnit ? localization.stringUnit.state : 'new';
        
        // Create localization entry using DOM methods
        const entryDiv = document.createElement('div');
        entryDiv.className = 'localization-entry simple-localization';
        
        // Add highlighting for needs review state
        if (state === 'needs_review') {
            entryDiv.classList.add('needs-review');
        }
        
        const langInput = document.createElement('input');
        langInput.type = 'text';
        langInput.value = lang;
        langInput.placeholder = 'Language';
        langInput.className = 'localization-lang-input';
        langInput.maxLength = 3;
        
        const valueInput = document.createElement('input');
        valueInput.type = 'text';
        valueInput.value = value;
        valueInput.placeholder = 'Translation';
        valueInput.className = 'localization-value-input';
        
        const stateSelect = document.createElement('select');
        stateSelect.className = 'localization-state-select';
        ['new', 'translated', 'needs_review'].forEach(stateOption => {
            const option = document.createElement('option');
            option.value = stateOption;
            option.textContent = stateOption.replace('_', ' ');
            option.selected = stateOption === state;
            stateSelect.appendChild(option);
        });
        
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-danger btn-sm';
        deleteBtn.textContent = '×';
        
        // Add event listeners
        langInput.addEventListener('change', (e) => {
            this.updateLocalizationLang(stringKey, lang, e.target.value);
        });
        
        valueInput.addEventListener('change', (e) => {
            this.updateLocalizationValue(stringKey, lang, e.target.value);
            this.autoUpdateState(stringKey, lang, e.target.value, stateSelect);
        });
        
        valueInput.addEventListener('input', (e) => {
            this.autoUpdateState(stringKey, lang, e.target.value, stateSelect, true);
        });
        
        stateSelect.addEventListener('change', (e) => {
            this.updateLocalizationState(stringKey, lang, e.target.value);
        });
        
        deleteBtn.addEventListener('click', () => {
            this.deleteLocalization(stringKey, lang);
        });
        
        // AI features
        const aiControls = this.createAIControls(stringKey, lang, localization);
        
        entryDiv.appendChild(langInput);
        entryDiv.appendChild(valueInput);
        entryDiv.appendChild(stateSelect);
        if (aiControls.proofreadIndicator) {
            entryDiv.appendChild(aiControls.proofreadIndicator);
        }
        entryDiv.appendChild(deleteBtn);
        
        // Add AI action buttons
        if (aiControls.buttons.length > 0) {
            const aiButtonsDiv = document.createElement('div');
            aiButtonsDiv.className = 'ai-buttons';
            aiControls.buttons.forEach(button => {
                aiButtonsDiv.appendChild(button);
            });
            entryDiv.appendChild(aiButtonsDiv);
        }
        
        container.appendChild(entryDiv);
        
        // Add proofreading feedback below the entry
        if (aiControls.feedback) {
            container.appendChild(aiControls.feedback);
        }
    }

    renderVariationLocalization(container, stringKey, lang, localization) {
        // Create variation container
        const variationContainer = document.createElement('div');
        variationContainer.className = 'variation-container';
        
        // Language header with delete button
        const headerDiv = document.createElement('div');
        headerDiv.className = 'variation-header';
        
        const langLabel = document.createElement('strong');
        langLabel.textContent = `${lang} (with variations)`;
        
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-danger btn-sm';
        deleteBtn.textContent = '× Delete Language';
        deleteBtn.addEventListener('click', () => {
            this.deleteLocalization(stringKey, lang);
        });
        
        headerDiv.appendChild(langLabel);
        headerDiv.appendChild(deleteBtn);
        variationContainer.appendChild(headerDiv);
        
        // Render each variation type (plural, device, etc.)
        Object.keys(localization.variations).forEach(variationType => {
            const variationTypeDiv = document.createElement('div');
            variationTypeDiv.className = 'variation-type';
            
            const typeHeader = document.createElement('div');
            typeHeader.className = 'variation-type-header';
            typeHeader.textContent = `${variationType} variations:`;
            variationTypeDiv.appendChild(typeHeader);
            
            const variations = localization.variations[variationType];
            Object.keys(variations).forEach(variationKey => {
                const variation = variations[variationKey];
                if (variation.stringUnit) {
                    const variationEntry = this.createVariationEntry(
                        stringKey, lang, variationType, variationKey, variation.stringUnit
                    );
                    variationTypeDiv.appendChild(variationEntry);
                }
            });
            
            // Add button to add new variation
            const addVariationBtn = document.createElement('button');
            addVariationBtn.className = 'btn btn-secondary btn-sm add-variation-btn';
            addVariationBtn.textContent = `Add ${variationType} variation`;
            addVariationBtn.addEventListener('click', () => {
                this.addVariation(stringKey, lang, variationType);
            });
            variationTypeDiv.appendChild(addVariationBtn);
            
            variationContainer.appendChild(variationTypeDiv);
        });
        
        // Add button to add new variation type
        const addVariationTypeBtn = document.createElement('button');
        addVariationTypeBtn.className = 'btn btn-secondary btn-sm';
        addVariationTypeBtn.textContent = 'Add variation type';
        addVariationTypeBtn.addEventListener('click', () => {
            this.addVariationType(stringKey, lang);
        });
        variationContainer.appendChild(addVariationTypeBtn);
        
        container.appendChild(variationContainer);
    }

    createVariationEntry(stringKey, lang, variationType, variationKey, stringUnit) {
        const entryDiv = document.createElement('div');
        entryDiv.className = 'variation-entry';
        
        const keyInput = document.createElement('input');
        keyInput.type = 'text';
        keyInput.value = variationKey;
        keyInput.placeholder = 'Variation key (e.g., one, other)';
        keyInput.className = 'variation-key-input';
        
        const valueInput = document.createElement('input');
        valueInput.type = 'text';
        valueInput.value = stringUnit.value || '';
        valueInput.placeholder = 'Translation';
        valueInput.className = 'variation-value-input';
        
        const stateSelect = document.createElement('select');
        stateSelect.className = 'variation-state-select';
        ['new', 'translated', 'needs_review'].forEach(stateOption => {
            const option = document.createElement('option');
            option.value = stateOption;
            option.textContent = stateOption.replace('_', ' ');
            option.selected = stateOption === (stringUnit.state || 'new');
            stateSelect.appendChild(option);
        });
        
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-danger btn-sm';
        deleteBtn.textContent = '×';
        
        // Add event listeners
        keyInput.addEventListener('change', (e) => {
            this.updateVariationKey(stringKey, lang, variationType, variationKey, e.target.value);
        });
        
        valueInput.addEventListener('change', (e) => {
            this.updateVariationValue(stringKey, lang, variationType, variationKey, e.target.value);
            this.autoUpdateVariationState(stringKey, lang, variationType, variationKey, e.target.value, stateSelect);
        });
        
        valueInput.addEventListener('input', (e) => {
            this.autoUpdateVariationState(stringKey, lang, variationType, variationKey, e.target.value, stateSelect, true);
        });
        
        stateSelect.addEventListener('change', (e) => {
            this.updateVariationState(stringKey, lang, variationType, variationKey, e.target.value);
        });
        
        deleteBtn.addEventListener('click', async () => {
            await this.deleteVariation(stringKey, lang, variationType, variationKey);
        });
        
        entryDiv.appendChild(keyInput);
        entryDiv.appendChild(valueInput);
        entryDiv.appendChild(stateSelect);
        entryDiv.appendChild(deleteBtn);
        
        return entryDiv;
    }

    async addNewString() {
        const key = await this.showInputDialog('Add New String', 'Enter string key:', '');
        if (key && key.trim()) {
            if (!this.data.strings) {
                this.data.strings = {};
            }
            this.data.strings[key] = {
                comment: '',
                localizations: {
                    en: {
                        stringUnit: {
                            state: 'translated',
                            value: ''
                        }
                    }
                }
            };
            this.markModified();
            this.renderStringEntry(key, this.data.strings[key]);
            this.updateStatusBarCounts();
        }
    }

    deleteString(key) {
        if (confirm(`Delete string "${key}"?`)) {
            delete this.data.strings[key];
            const entry = Array.from(document.querySelectorAll('.string-entry')).find(e => {
                return e.dataset.key === key;
            });
            if (entry) {
                entry.remove();
            }
            this.markModified();
            this.updateStatusBarCounts();
        }
    }

    updateStringKey(oldKey, newKey) {
        if (oldKey !== newKey && newKey.trim()) {
            this.data.strings[newKey] = this.data.strings[oldKey];
            delete this.data.strings[oldKey];
            this.markModified();
            this.renderEditor(); // Need full re-render since key changed
        }
    }

    updateStringComment(key, comment) {
        if (this.data.strings[key]) {
            this.data.strings[key].comment = comment;
            this.markModified();
        }
    }

    updateLocalizationValue(stringKey, lang, value) {
        if (this.data.strings[stringKey]) {
            // Ensure localizations object exists
            if (!this.data.strings[stringKey].localizations) {
                this.data.strings[stringKey].localizations = {};
            }
            
            // Ensure the specific localization exists
            if (!this.data.strings[stringKey].localizations[lang]) {
                this.data.strings[stringKey].localizations[lang] = {
                    stringUnit: {
                        state: 'new',
                        value: ''
                    }
                };
            }
            
            // Ensure stringUnit exists
            if (!this.data.strings[stringKey].localizations[lang].stringUnit) {
                this.data.strings[stringKey].localizations[lang].stringUnit = {
                    state: 'new',
                    value: ''
                };
            }
            
            // Now set the value
            this.data.strings[stringKey].localizations[lang].stringUnit.value = value;
            this.markModified();
        }
    }

    updateLocalizationLang(stringKey, oldLang, newLang) {
        if (oldLang !== newLang && newLang.trim() && this.data.strings[stringKey]) {
            const localizations = this.data.strings[stringKey].localizations;
            localizations[newLang] = localizations[oldLang];
            delete localizations[oldLang];
            this.markModified();
            this.updateStringEntry(stringKey);
        }
    }

    async addLocalization(stringKey) {
        const lang = await this.showInputDialog('Add Localization', 'Enter language code (e.g., es, fr, de):', '');
        if (lang && lang.trim()) {
            // Ensure localizations object exists
            if (!this.data.strings[stringKey].localizations) {
                this.data.strings[stringKey].localizations = {};
            }
            
            // Convert array to object if needed (safety check)
            if (Array.isArray(this.data.strings[stringKey].localizations)) {
                this.data.strings[stringKey].localizations = {};
            }
            
            if (!this.data.strings[stringKey].localizations[lang]) {
                this.data.strings[stringKey].localizations[lang] = {
                    stringUnit: {
                        state: 'new',
                        value: ''
                    }
                };
                this.markModified();
                this.updateStringEntry(stringKey);
                this.updateStatusBarCounts();
                this.showNotification(`Added localization for "${lang}"`, 'success');
            } else {
                this.showNotification(`Localization for "${lang}" already exists`, 'warning');
            }
        }
    }

    async addLocalizationWithVariations(stringKey) {
        const lang = await this.showInputDialog('Add Localization with Variations', 'Enter language code (e.g., es, fr, de):', '');
        if (!lang || !lang.trim()) return;
        
        // Ensure localizations object exists
        if (!this.data.strings[stringKey].localizations) {
            this.data.strings[stringKey].localizations = {};
        }
        
        if (this.data.strings[stringKey].localizations[lang]) {
            this.showNotification(`Localization for "${lang}" already exists`, 'warning');
            return;
        }
        
        const variationType = await this.showInputDialog(
            `Select Variation Type for ${lang}`,
            'Enter variation type (e.g., plural, device, width):',
            'plural'
        );
        
        if (!variationType || !variationType.trim()) return;
        
        // Create localization with variations structure
        this.data.strings[stringKey].localizations[lang] = {
            variations: {
                [variationType]: {
                    other: {
                        stringUnit: {
                            state: 'new',
                            value: ''
                        }
                    }
                }
            }
        };
        
        this.markModified();
        this.updateStringEntry(stringKey);
        this.showNotification(`Added localization with ${variationType} variations for "${lang}"`, 'success');
    }

    async deleteLocalization(stringKey, lang) {
        const shouldDelete = await this.showConfirmDialog(
            'Delete Localization',
            `Delete localization for "${lang}"?`
        );
        if (shouldDelete) {
            delete this.data.strings[stringKey].localizations[lang];
            this.markModified();
            this.updateStringEntry(stringKey);
            this.updateStatusBarCounts();
        }
    }

    // Variation management functions
    updateLocalizationState(stringKey, lang, newState) {
        if (this.data.strings[stringKey]) {
            // Ensure localizations object exists
            if (!this.data.strings[stringKey].localizations) {
                this.data.strings[stringKey].localizations = {};
            }
            
            // Ensure the specific localization exists
            if (!this.data.strings[stringKey].localizations[lang]) {
                this.data.strings[stringKey].localizations[lang] = {
                    stringUnit: {
                        state: 'new',
                        value: ''
                    }
                };
            }
            
            // Ensure stringUnit exists
            if (!this.data.strings[stringKey].localizations[lang].stringUnit) {
                this.data.strings[stringKey].localizations[lang].stringUnit = {
                    state: 'new',
                    value: ''
                };
            }
            
            // Now set the state
            this.data.strings[stringKey].localizations[lang].stringUnit.state = newState;
            this.markModified();
        }
    }

    updateVariationKey(stringKey, lang, variationType, oldKey, newKey) {
        if (oldKey !== newKey && newKey.trim()) {
            const localization = this.data.strings[stringKey].localizations[lang];
            if (localization.variations && localization.variations[variationType] && localization.variations[variationType][oldKey]) {
                localization.variations[variationType][newKey] = localization.variations[variationType][oldKey];
                delete localization.variations[variationType][oldKey];
                this.markModified();
                this.updateStringEntry(stringKey);
            }
        }
    }

    updateVariationValue(stringKey, lang, variationType, variationKey, newValue) {
        const localization = this.data.strings[stringKey].localizations[lang];
        if (localization.variations && localization.variations[variationType] && localization.variations[variationType][variationKey]) {
            if (!localization.variations[variationType][variationKey].stringUnit) {
                localization.variations[variationType][variationKey].stringUnit = { state: 'new', value: '' };
            }
            localization.variations[variationType][variationKey].stringUnit.value = newValue;
            this.markModified();
        }
    }

    updateVariationState(stringKey, lang, variationType, variationKey, newState) {
        const localization = this.data.strings[stringKey].localizations[lang];
        if (localization.variations && localization.variations[variationType] && localization.variations[variationType][variationKey]) {
            if (!localization.variations[variationType][variationKey].stringUnit) {
                localization.variations[variationType][variationKey].stringUnit = { state: 'new', value: '' };
            }
            localization.variations[variationType][variationKey].stringUnit.state = newState;
            this.markModified();
        }
    }

    async addVariation(stringKey, lang, variationType) {
        let examples = 'one, other, zero';
        if (variationType === 'device') {
            examples = 'iphone, ipad, mac, other';
        } else if (variationType === 'width') {
            examples = 'compact, regular, other';
        }
        
        const variationKey = await this.showInputDialog(
            `Add ${variationType.charAt(0).toUpperCase() + variationType.slice(1)} Variation`,
            `Enter ${variationType} variation key (e.g., ${examples}):`,
            ''
        );
        
        if (variationKey && variationKey.trim()) {
            const localization = this.data.strings[stringKey].localizations[lang];
            if (!localization.variations) {
                localization.variations = {};
            }
            if (!localization.variations[variationType]) {
                localization.variations[variationType] = {};
            }
            
            localization.variations[variationType][variationKey] = {
                stringUnit: {
                    state: 'new',
                    value: ''
                }
            };
            
            this.markModified();
            this.updateStringEntry(stringKey);
        }
    }

    async addVariationType(stringKey, lang) {
        const variationType = await this.showInputDialog(
            `Add Variation Type for ${lang}`,
            'Enter variation type (e.g., plural, device, width):',
            ''
        );
        
        if (variationType && variationType.trim()) {
            const localization = this.data.strings[stringKey].localizations[lang];
            if (!localization.variations) {
                localization.variations = {};
            }
            
            localization.variations[variationType] = {
                other: {
                    stringUnit: {
                        state: 'new',
                        value: ''
                    }
                }
            };
            
            this.markModified();
            this.updateStringEntry(stringKey);
        }
    }

    async deleteVariation(stringKey, lang, variationType, variationKey) {
        const shouldDelete = await this.showConfirmDialog(
            'Delete Variation',
            `Delete variation "${variationKey}" for ${variationType}?`
        );
        if (shouldDelete) {
            const localization = this.data.strings[stringKey].localizations[lang];
            if (localization.variations && localization.variations[variationType]) {
                delete localization.variations[variationType][variationKey];
                
                // If no variations left in this type, remove the type
                if (Object.keys(localization.variations[variationType]).length === 0) {
                    delete localization.variations[variationType];
                }
                
                // If no variation types left, remove variations and convert to simple stringUnit
                if (Object.keys(localization.variations).length === 0) {
                    delete localization.variations;
                    localization.stringUnit = {
                        state: 'new',
                        value: ''
                    };
                }
                
                this.markModified();
                this.updateStringEntry(stringKey);
            }
        }
    }

    autoUpdateState(stringKey, lang, value, stateSelect, isInputEvent = false) {
        const trimmedValue = value ? value.trim() : '';
        const newState = trimmedValue === '' ? 'new' : 'translated';
        
        // Update the select dropdown
        stateSelect.value = newState;
        
        // Update the data only on change events, not input events
        if (!isInputEvent && this.data.strings[stringKey]) {
            // Ensure the localization structure exists before updating state
            if (!this.data.strings[stringKey].localizations) {
                this.data.strings[stringKey].localizations = {};
            }
            if (!this.data.strings[stringKey].localizations[lang]) {
                this.data.strings[stringKey].localizations[lang] = {
                    stringUnit: { state: 'new', value: '' }
                };
            }
            if (!this.data.strings[stringKey].localizations[lang].stringUnit) {
                this.data.strings[stringKey].localizations[lang].stringUnit = { state: 'new', value: '' };
            }
            
            this.data.strings[stringKey].localizations[lang].stringUnit.state = newState;
            this.markModifiedSilent();
        }
    }

    autoUpdateVariationState(stringKey, lang, variationType, variationKey, value, stateSelect, isInputEvent = false) {
        const trimmedValue = value ? value.trim() : '';
        const newState = trimmedValue === '' ? 'new' : 'translated';
        
        // Update the select dropdown
        stateSelect.value = newState;
        
        // Update the data only on change events, not input events
        if (!isInputEvent) {
            const localization = this.data.strings[stringKey].localizations[lang];
            if (localization && localization.variations && 
                localization.variations[variationType] && 
                localization.variations[variationType][variationKey]) {
                
                if (!localization.variations[variationType][variationKey].stringUnit) {
                    localization.variations[variationType][variationKey].stringUnit = { state: 'new', value: '' };
                }
                localization.variations[variationType][variationKey].stringUnit.state = newState;
                this.markModifiedSilent();
            }
        }
    }

    markModifiedSilent() {
        this.isModified = true;
        if (this.fileInfo) {
            this.fileInfo.textContent = this.fileInfo.textContent.replace(' (saved)', '') + ' (modified)';
        }
        
        // Debounced progress update to avoid excessive calls
        clearTimeout(this.progressUpdateTimeout);
        this.progressUpdateTimeout = setTimeout(() => {
            this.updateProgressIndicators();
            // Reapply filter if one is active
            if (this.filterLanguage.value) {
                this.applyFilter();
            }
        }, 500);
    }

    markModified() {
        this.isModified = true;
        if (this.fileInfo) {
            this.fileInfo.textContent = this.fileInfo.textContent.replace(' (saved)', '') + ' (modified)';
        }
        // Update progress indicators when data changes
        this.updateProgressIndicators();
        // Reapply filter if one is active
        if (this.filterLanguage && this.filterLanguage.value) {
            this.applyFilter();
        }
    }

    async exportFile() {
        try {
            // Ensure data integrity and clean AI review data before export
            this.data = this.fixParsedData(this.data);
            const cleanData = this.cleanDataForExport(this.data);
            
            const response = await fetch('/backend/index.php/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ data: cleanData })
            });

            const result = await response.json();
            if (result.success) {
                const filename = this.currentFileId ? 
                    this.editorTitle.textContent : 
                    'exported.xcstrings';
                this.downloadFile(result.xcstring, filename);
            } else {
                this.showNotification('Error generating file: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error exporting file: ' + error.message, 'error');
        }
    }

    downloadFile(content, filename) {
        const blob = new Blob([content], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    // AI Features
    updateAIUI() {
        if (!this.aiConfig || !this.aiConfig.ai_enabled || !this.currentUser) {
            this.aiSection.style.display = 'none';
            return;
        }
        
        this.aiSection.style.display = 'block';
        
        // Populate AI providers
        this.aiProvider.innerHTML = '<option value="">Select AI Provider</option>';
        Object.keys(this.aiConfig.ai_providers).forEach(provider => {
            const option = document.createElement('option');
            option.value = provider;
            option.textContent = provider.charAt(0).toUpperCase() + provider.slice(1);
            this.aiProvider.appendChild(option);
        });
        
        // Set default provider if available
        if (Object.keys(this.aiConfig.ai_providers).length > 0) {
            const defaultProvider = Object.keys(this.aiConfig.ai_providers)[0];
            this.aiProvider.value = defaultProvider;
            this.selectedAIProvider = defaultProvider;
            this.updateAIModels();
        }
    }
    
    updateAIModels() {
        const provider = this.aiProvider.value;
        this.aiModel.innerHTML = '<option value="">Select Model</option>';
        
        if (provider && this.aiConfig.ai_providers[provider]) {
            this.selectedAIProvider = provider;
            const models = this.aiConfig.ai_providers[provider].models;
            models.forEach(model => {
                const option = document.createElement('option');
                option.value = model;
                option.textContent = model;
                this.aiModel.appendChild(option);
            });
            
            // Set first model as default
            if (models.length > 0) {
                this.aiModel.value = models[0];
                this.selectedAIModel = models[0];
            }
        }
    }
    
    async translateMissingLocalizations() {
        if (!this.selectedAIProvider || !this.selectedAIModel) {
            this.showNotification('Please select an AI provider and model first', 'warning');
            return;
        }
        
        if (!this.data || !this.data.strings) {
            this.showNotification('No strings to translate', 'warning');
            return;
        }
        
        const sourceLanguage = this.data.sourceLanguage || 'en';
        const allLanguages = new Set();
        
        // Collect all target languages
        Object.values(this.data.strings).forEach(stringData => {
            if (stringData.localizations) {
                Object.keys(stringData.localizations).forEach(lang => {
                    if (lang !== sourceLanguage) {
                        allLanguages.add(lang);
                    }
                });
            }
        });
        
        if (allLanguages.size === 0) {
            this.showNotification('No target languages found to translate to. Add localizations for other languages first.', 'warning');
            return;
        }
        
        this.aiOperationCounts = { translated: 0, total: 0 };
        let totalItemsToTranslate = 0;
        
        // Helper function to get source text (use key if no explicit source localization)
        const getSourceText = (stringKey, stringData) => {
            const explicitSourceText = stringData.localizations?.[sourceLanguage]?.stringUnit?.value;
            if (explicitSourceText && explicitSourceText.trim() !== '') {
                return explicitSourceText;
            }
            // Use the key as source text if no explicit source localization exists
            return stringKey;
        };
        
        // Count total items for progress tracking
        for (const targetLanguage of allLanguages) {
            let languageCount = 0;
            for (const [stringKey, stringData] of Object.entries(this.data.strings)) {
                const sourceText = getSourceText(stringKey, stringData);
                if (sourceText && !this.isTranslated(stringData, targetLanguage)) {
                    totalItemsToTranslate++;
                    languageCount++;
                }
            }
            console.log(`Language ${targetLanguage}: ${languageCount} missing translations`);
        }
        
        console.log(`Total items to translate: ${totalItemsToTranslate}`);
        console.log(`Available languages: ${Array.from(allLanguages).join(', ')}`);
        
        if (totalItemsToTranslate === 0) {
            this.showNotification('No missing translations found', 'info');
            return;
        }
        
        this.showAIProgress('AI Batch Translation', totalItemsToTranslate);
        this.showNotification('Starting AI batch translation...', 'info');
        
        for (const targetLanguage of allLanguages) {
            // Collect items that need translation for this language
            const itemsToTranslate = [];
            
            for (const [stringKey, stringData] of Object.entries(this.data.strings)) {
                const sourceText = getSourceText(stringKey, stringData);
                const targetLocalization = stringData.localizations?.[targetLanguage];
                
                // Only translate if source exists and target is not translated (using same logic as filter)
                if (sourceText && !this.isTranslated(stringData, targetLanguage)) {
                    itemsToTranslate.push({
                        key: stringKey,
                        text: sourceText
                    });
                }
            }
            
            if (itemsToTranslate.length === 0) continue;
            
            // Process in batches of 15 to avoid overwhelming the AI and hitting token limits
            const batchSize = 15;
            const batches = [];
            for (let i = 0; i < itemsToTranslate.length; i += batchSize) {
                batches.push(itemsToTranslate.slice(i, i + batchSize));
            }
            
            this.showNotification(`Translating ${itemsToTranslate.length} strings to ${targetLanguage} in ${batches.length} parallel batches...`, 'info');
            
            // Process all batches in parallel with concurrency limit of 3
            const maxConcurrency = 3;
            const results = await this.processBatchesInParallel(
                batches,
                async (batch, batchIndex) => {
                    try {
                        const translations = await this.batchTranslateText(
                            batch,
                            sourceLanguage,
                            targetLanguage
                        );
                        
                        // Apply translations immediately as each batch completes
                        for (const translation of translations) {
                            const stringData = this.data.strings[translation.key];
                            if (stringData) {
                                // Ensure localizations structure exists
                                if (!stringData.localizations) {
                                    stringData.localizations = {};
                                }
                                if (!stringData.localizations[targetLanguage]) {
                                    stringData.localizations[targetLanguage] = {
                                        stringUnit: { state: 'new', value: '' }
                                    };
                                }
                                
                                // Update with translation and set state to needs_review
                                stringData.localizations[targetLanguage].stringUnit.value = translation.translation;
                                stringData.localizations[targetLanguage].stringUnit.state = 'needs_review';
                                
                                this.aiOperationCounts.translated++;
                                
                                // Update progress immediately
                                this.updateAIProgress(this.aiOperationCounts.translated, totalItemsToTranslate, '', `Translated "${translation.key}" to ${targetLanguage}`);
                                
                                // Update UI immediately
                                console.log(`Updating UI for translated key: ${translation.key}`);
                                this.updateStringEntryUI(translation.key);
                            }
                        }
                        
                        return { batchIndex, translations, batch };
                    } catch (error) {
                        console.error(`Batch translation failed for ${targetLanguage} batch ${batchIndex + 1}:`, error);
                        throw error;
                    }
                },
                maxConcurrency,
                `${targetLanguage} translation`
            );
            
            this.markModified();
        }
        
        // Hide AI progress and show completion
        this.hideAIProgress();
        
        if (this.aiOperationCounts.translated > 0) {
            this.updateProgressIndicators();
            this.showNotification(`Translated ${this.aiOperationCounts.translated} strings using AI`, 'success');
        } else {
            this.showNotification('No missing translations found', 'info');
        }
    }
    
    async proofreadAllLocalizations() {
        if (!this.selectedAIProvider || !this.selectedAIModel) {
            this.showNotification('Please select an AI provider and model first', 'warning');
            return;
        }
        
        if (!this.data || !this.data.strings) {
            this.showNotification('No strings to proofread', 'warning');
            return;
        }
        
        this.aiOperationCounts = { proofread: 0, total: 0 };
        let totalItemsToProofread = 0;
        
        // Group items by language for batch processing and count total
        const itemsByLanguage = {};
        
        for (const [stringKey, stringData] of Object.entries(this.data.strings)) {
            if (stringData.localizations) {
                for (const [language, localization] of Object.entries(stringData.localizations)) {
                    const text = localization.stringUnit?.value;
                    if (text) {
                        if (!itemsByLanguage[language]) {
                            itemsByLanguage[language] = [];
                        }
                        itemsByLanguage[language].push({
                            key: stringKey,
                            text: text,
                            localization: localization
                        });
                        totalItemsToProofread++;
                    }
                }
            }
        }
        
        if (totalItemsToProofread === 0) {
            this.showNotification('No localizations found to proofread', 'info');
            return;
        }
        
        this.showAIProgress('AI Batch Proofreading', totalItemsToProofread);
        this.showNotification('Starting AI batch proofreading...', 'info');
        
        // Process each language in batches
        for (const [language, items] of Object.entries(itemsByLanguage)) {
            if (items.length === 0) continue;
            
            // Process in batches of 15
            const batchSize = 15;
            const batches = [];
            for (let i = 0; i < items.length; i += batchSize) {
                batches.push(items.slice(i, i + batchSize));
            }
            
            this.showNotification(`Proofreading ${items.length} strings in ${language} in ${batches.length} parallel batches...`, 'info');
            
            // Process all batches in parallel with concurrency limit of 3
            const maxConcurrency = 3;
            const results = await this.processBatchesInParallel(
                batches,
                async (batch, batchIndex) => {
                    try {
                        const batchItems = batch.map(item => ({
                            key: item.key,
                            text: item.text
                        }));
                        
                        const reviews = await this.batchProofreadText(batchItems, language);
                        
                        // Apply reviews immediately as each batch completes
                        for (const review of reviews) {
                            const item = batch.find(item => item.key === review.key);
                            if (item) {
                                // Store proofreading result
                                if (!item.localization.stringUnit.ai_review) {
                                    item.localization.stringUnit.ai_review = {};
                                }
                                item.localization.stringUnit.ai_review = {
                                    status: review.status,
                                    feedback: review.feedback
                                };
                                this.aiOperationCounts.proofread++;
                                
                                // Update progress immediately
                                this.updateAIProgress(this.aiOperationCounts.proofread, totalItemsToProofread, '', `Proofread "${review.key}" in ${language}: ${review.status}`);
                                
                                // Update UI immediately
                                console.log(`Updating UI for proofread key: ${review.key}, status: ${review.status}`);
                                this.updateStringEntryUI(review.key);
                            }
                        }
                        
                        return { batchIndex, reviews, batch };
                    } catch (error) {
                        console.error(`Batch proofreading failed for ${language} batch ${batchIndex + 1}:`, error);
                        throw error;
                    }
                },
                maxConcurrency,
                `${language} proofreading`
            );
        }
        
        // Hide AI progress and show completion
        this.hideAIProgress();
        
        if (this.aiOperationCounts.proofread > 0) {
            this.showNotification(`Proofread ${this.aiOperationCounts.proofread} localizations using AI`, 'success');
        } else {
            this.showNotification('No localizations found to proofread', 'info');
        }
    }
    
    async translateText(text, sourceLanguage, targetLanguage, stringKey) {
        const response = await fetch('/backend/index.php/ai/translate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text: text,
                source_language: sourceLanguage,
                target_language: targetLanguage,
                string_key: stringKey,
                context_strings: this.data.strings,
                provider: this.selectedAIProvider,
                model: this.selectedAIModel
            })
        });
        
        const result = await response.json();
        if (!result.success) {
            throw new Error(result.error || 'Translation failed');
        }
        
        return result.translation;
    }
    
    async proofreadText(text, language, stringKey) {
        const response = await fetch('/backend/index.php/ai/proofread', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text: text,
                language: language,
                string_key: stringKey,
                context_strings: this.data.strings,
                provider: this.selectedAIProvider,
                model: this.selectedAIModel
            })
        });
        
        const result = await response.json();
        if (!result.success) {
            throw new Error(result.error || 'Proofreading failed');
        }
        
        return result.review;
    }
    
    async retryWithBackoff(asyncFn, maxRetries = 3, baseDelay = 1000) {
        for (let attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return await asyncFn();
            } catch (error) {
                if (attempt === maxRetries) {
                    throw error; // Final attempt failed, re-throw the error
                }
                
                // Exponential backoff: 1s, 2s, 4s delays
                const delay = baseDelay * Math.pow(2, attempt - 1);
                console.warn(`Attempt ${attempt} failed, retrying in ${delay}ms:`, error.message);
                await new Promise(resolve => setTimeout(resolve, delay));
            }
        }
    }
    
    async processBatchesInParallel(batches, processBatchFn, maxConcurrency = 3, operationName = 'operation') {
        const totalBatches = batches.length;
        let completed = 0;
        let lastProgress = 0;
        
        // Create semaphore to limit concurrency
        const semaphore = new Array(maxConcurrency).fill(null).map(() => Promise.resolve());
        let semaphoreIndex = 0;
        
        // Process all batches with concurrency control
        const batchPromises = batches.map(async (batch, batchIndex) => {
            // Wait for available slot
            const currentSemaphore = semaphoreIndex % maxConcurrency;
            await semaphore[currentSemaphore];
            
            // Create new promise for this slot
            const batchPromise = (async () => {
                try {
                    const data = await processBatchFn(batch, batchIndex);
                    completed++;
                    
                    // Progress is now handled by the main progress bar, no need for notifications
                    
                    return { success: true, batchIndex, data };
                } catch (error) {
                    completed++;
                    console.error(`Batch ${batchIndex + 1} failed:`, error);
                    return { success: false, batchIndex, error };
                }
            })();
            
            // Update semaphore slot
            semaphore[currentSemaphore] = batchPromise.catch(() => {}); // Ignore errors for semaphore
            semaphoreIndex++;
            
            return batchPromise;
        });
        
        // Wait for all batches to complete
        const results = await Promise.all(batchPromises);
        
        // Sort results by batch index to maintain order
        results.sort((a, b) => a.batchIndex - b.batchIndex);
        
        const successCount = results.filter(r => r.success).length;
        const failCount = results.filter(r => !r.success).length;
        
        // Batch completion messages are now handled by the main functions
        
        return results;
    }
    
    async batchTranslateText(items, sourceLanguage, targetLanguage) {
        return await this.retryWithBackoff(async () => {
            const response = await fetch('/backend/index.php/ai/batch-translate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    items: items,
                    source_language: sourceLanguage,
                    target_language: targetLanguage,
                    provider: this.selectedAIProvider,
                    model: this.selectedAIModel
                })
            });
            
            const result = await response.json();
            if (!result.success) {
                throw new Error(result.error || 'Batch translation failed');
            }
            
            return result.translations;
        });
    }
    
    async batchProofreadText(items, language) {
        return await this.retryWithBackoff(async () => {
            const response = await fetch('/backend/index.php/ai/batch-proofread', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    items: items,
                    language: language,
                    provider: this.selectedAIProvider,
                    model: this.selectedAIModel
                })
            });
            
            const result = await response.json();
            if (!result.success) {
                throw new Error(result.error || 'Batch proofreading failed');
            }
            
            return result.reviews;
        });
    }
    
    createAIControls(stringKey, lang, localization) {
        const controls = {
            proofreadIndicator: null,
            buttons: [],
            feedback: null
        };
        
        if (!this.aiConfig || !this.aiConfig.ai_enabled || !this.currentUser) {
            return controls;
        }
        
        const text = localization.stringUnit?.value;
        const aiReview = localization.stringUnit?.ai_review;
        
        // Create proofreading indicator (green checkmark for good status)
        if (aiReview && aiReview.status === 'good') {
            const indicator = document.createElement('span');
            indicator.className = 'ai-indicator ai-good';
            indicator.innerHTML = '✓';
            indicator.title = 'AI proofreading: Good';
            controls.proofreadIndicator = indicator;
        }
        
        // Create proofreading feedback display
        if (aiReview && (aiReview.status === 'wording' || aiReview.status === 'issue')) {
            const feedbackDiv = document.createElement('div');
            feedbackDiv.className = `ai-feedback ai-${aiReview.status}`;
            
            const statusText = aiReview.status === 'wording' ? 'Wording Suggestion' : 'Issue Found';
            feedbackDiv.innerHTML = `
                <strong>${statusText}:</strong> ${aiReview.feedback || 'No feedback provided'}
            `;
            controls.feedback = feedbackDiv;
        }
        
        // Create AI action buttons
        if (text) {
            // Proofread button
            const proofreadBtn = document.createElement('button');
            proofreadBtn.className = 'btn btn-sm btn-secondary ai-btn';
            proofreadBtn.textContent = 'AI Proofread';
            proofreadBtn.addEventListener('click', async () => {
                await this.proofreadSingleLocalization(stringKey, lang);
            });
            controls.buttons.push(proofreadBtn);
        }
        
        // Translate button (for missing localizations)
        const sourceLanguage = this.data.sourceLanguage || 'en';
        const sourceText = this.data.strings[stringKey]?.localizations?.[sourceLanguage]?.stringUnit?.value;
        if (sourceText && lang !== sourceLanguage && (!text || text.trim() === '')) {
            const translateBtn = document.createElement('button');
            translateBtn.className = 'btn btn-sm btn-primary ai-btn';
            translateBtn.textContent = 'AI Translate';
            translateBtn.addEventListener('click', async () => {
                await this.translateSingleLocalization(stringKey, lang);
            });
            controls.buttons.push(translateBtn);
        }
        
        return controls;
    }
    
    async proofreadSingleLocalization(stringKey, lang) {
        if (!this.selectedAIProvider || !this.selectedAIModel) {
            this.showNotification('Please select an AI provider and model first', 'warning');
            return;
        }
        
        const localization = this.data.strings[stringKey]?.localizations?.[lang];
        const text = localization?.stringUnit?.value;
        
        if (!text) {
            this.showNotification('No text to proofread', 'warning');
            return;
        }
        
        try {
            this.showNotification('AI proofreading in progress...', 'info');
            const review = await this.proofreadText(text, lang, stringKey);
            
            // Store the review result
            if (!localization.stringUnit.ai_review) {
                localization.stringUnit.ai_review = {};
            }
            localization.stringUnit.ai_review = review;
            
            // Update the UI
            this.updateStringEntry(stringKey);
            this.showNotification('AI proofreading completed', 'success');
        } catch (error) {
            this.showNotification('AI proofreading failed: ' + error.message, 'error');
        }
    }
    
    async translateSingleLocalization(stringKey, lang) {
        if (!this.selectedAIProvider || !this.selectedAIModel) {
            this.showNotification('Please select an AI provider and model first', 'warning');
            return;
        }
        
        const sourceLanguage = this.data.sourceLanguage || 'en';
        const sourceText = this.data.strings[stringKey]?.localizations?.[sourceLanguage]?.stringUnit?.value;
        
        if (!sourceText) {
            this.showNotification('No source text to translate from', 'warning');
            return;
        }
        
        try {
            this.showNotification('AI translation in progress...', 'info');
            const translation = await this.translateText(sourceText, sourceLanguage, lang, stringKey);
            
            // Ensure localization structure exists
            if (!this.data.strings[stringKey].localizations) {
                this.data.strings[stringKey].localizations = {};
            }
            if (!this.data.strings[stringKey].localizations[lang]) {
                this.data.strings[stringKey].localizations[lang] = {
                    stringUnit: { state: 'new', value: '' }
                };
            }
            
            // Update with translation and set state to needs_review
            this.data.strings[stringKey].localizations[lang].stringUnit.value = translation;
            this.data.strings[stringKey].localizations[lang].stringUnit.state = 'needs_review';
            
            this.markModified();
            this.updateStringEntry(stringKey);
            this.updateProgressIndicators();
            this.showNotification('AI translation completed', 'success');
        } catch (error) {
            this.showNotification('AI translation failed: ' + error.message, 'error');
        }
    }

    // Notification System
    showNotification(message, type = 'info', duration = 5000) {
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.innerHTML = `
            <span>${message}</span>
            <button class="close-btn" onclick="this.parentElement.remove()">&times;</button>
        `;
        
        this.notifications.appendChild(notification);
        
        // Auto-hide after duration
        setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, duration);
        
        // Click to dismiss
        notification.addEventListener('click', () => notification.remove());
    }

    // Confirmation Modal
    showConfirmDialog(title, message) {
        return new Promise((resolve) => {
            // Ensure modal is hidden first to reset state
            this.confirmModal.style.display = 'none';
            
            // Set new content
            this.confirmTitle.textContent = title;
            this.confirmMessage.textContent = message;
            
            // Small delay to ensure DOM update, then show
            setTimeout(() => {
                this.confirmModal.style.display = 'block';
            }, 10);
            
            const handleOk = () => {
                this.hideConfirmModal();
                resolve(true);
            };
            
            const handleCancel = () => {
                this.hideConfirmModal();
                resolve(false);
            };
            
            // Remove previous listeners
            this.confirmOk.replaceWith(this.confirmOk.cloneNode(true));
            this.confirmOk = document.getElementById('confirmOk');
            this.confirmCancel.replaceWith(this.confirmCancel.cloneNode(true));
            this.confirmCancel = document.getElementById('confirmCancel');
            
            // Add new listeners
            this.confirmOk.addEventListener('click', handleOk);
            this.confirmCancel.addEventListener('click', handleCancel);
        });
    }

    hideConfirmModal() {
        this.confirmModal.style.display = 'none';
        // Clear content to prevent stale data
        this.confirmTitle.textContent = '';
        this.confirmMessage.textContent = '';
    }

    // Input Modal
    showInputDialog(title, label, defaultValue = '') {
        return new Promise((resolve) => {
            this.inputModal.style.display = 'block';
            
            const handleSubmit = (e) => {
                e.preventDefault();
                const value = this.inputField.value.trim();
                if (value) {
                    this.hideInputModal();
                    resolve(value);
                }
            };
            
            const handleCancel = () => {
                this.hideInputModal();
                resolve(null);
            };
            
            // Remove previous listeners
            this.inputForm.replaceWith(this.inputForm.cloneNode(true));
            this.inputForm = document.getElementById('inputForm');
            this.inputCancel.replaceWith(this.inputCancel.cloneNode(true));
            this.inputCancel = document.getElementById('inputCancel');
            this.inputField = document.getElementById('inputField');
            this.closeInputModal.replaceWith(this.closeInputModal.cloneNode(true));
            this.closeInputModal = document.getElementById('closeInputModal');
            
            // Re-assign title and label elements after cloning (they may have been replaced)
            this.inputTitle = document.getElementById('inputTitle');
            this.inputLabel = document.getElementById('inputLabel');
            
            // Update title and label after re-assignment
            this.inputTitle.textContent = title;
            this.inputLabel.textContent = label;
            
            // Set value and focus after re-assignment
            this.inputField.value = defaultValue;
            this.inputField.focus();
            
            // Add new listeners
            this.inputForm.addEventListener('submit', handleSubmit);
            this.inputCancel.addEventListener('click', handleCancel);
            this.closeInputModal.addEventListener('click', handleCancel);
        });
    }

    hideInputModal() {
        this.inputModal.style.display = 'none';
        // Clear all content to prevent stale data
        this.inputField.value = '';
        this.inputTitle.textContent = '';
        this.inputLabel.textContent = '';
    }

    // Utility function to escape HTML attributes
    escapeHtml(text) {
        if (!text) return '';
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // Fix data structure issues from PHP parsing
    fixParsedData(data) {
        if (!data || !data.strings) return data;
        
        // Fix each string's localizations if they're arrays instead of objects
        Object.keys(data.strings).forEach(stringKey => {
            const stringData = data.strings[stringKey];
            
            // Ensure localizations property exists
            if (!stringData.localizations) {
                stringData.localizations = {};
            } else if (Array.isArray(stringData.localizations)) {
                // Convert empty array [] back to empty object {}
                if (stringData.localizations.length === 0) {
                    stringData.localizations = {};
                }
            }
        });
        
        return data;
    }
    
    cleanDataForExport(data) {
        if (!data || !data.strings) return data;
        
        // Create a deep copy to avoid modifying the original data
        const cleanData = JSON.parse(JSON.stringify(data));
        
        // Remove AI review data from all localizations
        for (const [key, stringData] of Object.entries(cleanData.strings)) {
            if (stringData.localizations) {
                for (const [lang, localization] of Object.entries(stringData.localizations)) {
                    if (localization.stringUnit && localization.stringUnit.ai_review) {
                        delete localization.stringUnit.ai_review;
                    }
                    
                    // Also clean variations if they exist
                    if (localization.variations) {
                        for (const [variationType, variations] of Object.entries(localization.variations)) {
                            for (const [variationKey, variation] of Object.entries(variations)) {
                                if (variation.stringUnit && variation.stringUnit.ai_review) {
                                    delete variation.stringUnit.ai_review;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return cleanData;
    }

    // AI Progress Management
    showAIProgress(title, totalItems = 0) {
        this.statusAIText.textContent = totalItems > 0 ? `${title}: 0 / ${totalItems}` : title;
        this.statusAIProgressFill.style.width = '0%';
        this.statusAIOperation.style.display = 'flex';
        
        this.aiProgressState = {
            total: totalItems,
            completed: 0,
            isActive: true
        };
    }
    
    updateAIProgress(completed, total, message = '', details = '') {
        if (!this.aiProgressState?.isActive) return;
        
        const percentage = total > 0 ? (completed / total) * 100 : 0;
        this.statusAIProgressFill.style.width = `${percentage}%`;
        
        if (message) {
            this.statusAIText.textContent = message;
        } else {
            // Extract title from current text and update with progress
            const currentText = this.statusAIText.textContent;
            const title = currentText.split(':')[0];
            this.statusAIText.textContent = `${title}: ${completed} / ${total}`;
        }
        
        this.aiProgressState.completed = completed;
    }
    
    hideAIProgress() {
        this.statusAIOperation.style.display = 'none';
        this.aiProgressState = { isActive: false };
    }
    
    // Navigate to next incomplete string
    navigateToNextIncomplete() {
        if (!this.data || !this.data.strings) return;
        
        const stringEntries = document.querySelectorAll('.string-entry');
        const currentScrollTop = window.scrollY;
        
        // Find all incomplete entries that are visible (not filtered out)
        const incompleteEntries = Array.from(stringEntries).filter(entry => {
            if (entry.classList.contains('filtered-out')) return false;
            
            const stringKey = entry.dataset.key;
            const stringData = this.data.strings[stringKey];
            if (!stringData) return false;
            
            return this.isStringIncomplete(stringData);
        });
        
        if (incompleteEntries.length === 0) {
            this.showNotification('No incomplete strings found', 'info');
            return;
        }
        
        // Find the next incomplete entry after current scroll position
        let nextEntry = null;
        for (const entry of incompleteEntries) {
            const entryTop = entry.offsetTop;
            if (entryTop > currentScrollTop + 50) { // Add small offset to avoid getting stuck on current entry
                nextEntry = entry;
                break;
            }
        }
        
        // If no entry found after current position, wrap to first incomplete entry
        if (!nextEntry) {
            nextEntry = incompleteEntries[0];
        }
        
        // Scroll to the entry
        const targetTop = nextEntry.offsetTop - 100; // Offset for better visibility
        window.scrollTo({ top: targetTop, behavior: 'smooth' });
        
        // Optionally highlight the entry briefly
        nextEntry.style.backgroundColor = '#e6f2ff';
        setTimeout(() => {
            nextEntry.style.backgroundColor = '';
        }, 1500);
    }
    
    // Check if a string is incomplete (has missing or non-translated localizations)
    isStringIncomplete(stringData) {
        if (!stringData.localizations) return true;
        
        const sourceLanguage = this.data.sourceLanguage || 'en';
        const allLanguages = new Set();
        
        // Collect all languages in the project
        Object.values(this.data.strings).forEach(str => {
            if (str.localizations) {
                Object.keys(str.localizations).forEach(lang => allLanguages.add(lang));
            }
        });
        
        // Check if this string is incomplete in any language
        for (const lang of allLanguages) {
            if (lang !== sourceLanguage) {
                if (!this.isTranslated(stringData, lang)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    // Update status bar string counts
    updateStatusBarCounts() {
        if (!this.data || !this.data.strings) {
            this.statusTotalStrings.textContent = '0';
            this.statusIncompleteStrings.textContent = '0';
            return;
        }
        
        const totalStrings = Object.keys(this.data.strings).length;
        let incompleteCount = 0;
        
        Object.values(this.data.strings).forEach(stringData => {
            if (this.isStringIncomplete(stringData)) {
                incompleteCount++;
            }
        });
        
        this.statusTotalStrings.textContent = totalStrings.toString();
        this.statusIncompleteStrings.textContent = incompleteCount.toString();
        
        // Disable Next button if no incomplete strings
        this.statusNextBtn.disabled = incompleteCount === 0;
    }
    
    // Update specific string entry in UI without full re-render
    updateStringEntryUI(stringKey) {
        console.log(`Attempting to update UI for key: ${stringKey}`);
        // Use a more robust method to find the element by iterating through all string entries
        // This avoids CSS selector escaping issues with special characters
        const stringEntry = Array.from(document.querySelectorAll('.string-entry')).find(entry => {
            return entry.dataset.key === stringKey;
        });
        if (!stringEntry) {
            console.warn(`String entry not found for key: ${stringKey}`);
            return;
        }
        
        const stringData = this.data.strings[stringKey];
        if (!stringData) {
            console.warn(`String data not found for key: ${stringKey}`);
            return;
        }
        
        console.log(`String data for ${stringKey}:`, stringData.localizations);
        
        // Find the localizations container and re-render it
        const localizationsContainer = stringEntry.querySelector('.localizations');
        if (localizationsContainer) {
            console.log(`Re-rendering localizations for ${stringKey}`);
            this.renderLocalizations(localizationsContainer, stringKey, stringData.localizations);
        } else {
            console.warn(`Localizations container not found for key: ${stringKey}`);
        }
    }

    // Version History Methods
    
    async showVersionHistory() {
        if (!this.currentFileId) {
            this.showNotification('No file loaded', 'error');
            return;
        }
        
        try {
            // Load versions first
            const versionsResponse = await fetch(`/backend/index.php/files/${this.currentFileId}/versions`);
            const versionsResult = await versionsResponse.json();
            
            if (!versionsResult.success) {
                this.showNotification('Failed to load versions: ' + (versionsResult.error || 'Unknown error'), 'error');
                return;
            }
            
            // Try to load stats, but fallback to default stats if it fails
            let stats = {
                total_versions: 0,
                unique_contributors: 0,
                total_size_bytes: 0
            };
            
            try {
                const statsResponse = await fetch(`/backend/index.php/files/${this.currentFileId}/version-stats`);
                const statsResult = await statsResponse.json();
                
                if (statsResult.success && statsResult.stats) {
                    stats = statsResult.stats;
                } else {
                    console.warn('Stats endpoint failed, using calculated stats');
                    // Calculate basic stats from versions data
                    if (versionsResult.versions && versionsResult.versions.length > 0) {
                        stats.total_versions = versionsResult.versions.length;
                        stats.unique_contributors = new Set(versionsResult.versions.map(v => v.created_by_user_id)).size;
                        stats.total_size_bytes = versionsResult.versions.reduce((sum, v) => sum + (v.size_bytes || 0), 0);
                    }
                }
            } catch (statsError) {
                console.warn('Stats endpoint error, using calculated stats:', statsError);
                // Calculate basic stats from versions data
                if (versionsResult.versions && versionsResult.versions.length > 0) {
                    stats.total_versions = versionsResult.versions.length;
                    stats.unique_contributors = new Set(versionsResult.versions.map(v => v.created_by_user_id)).size;
                    stats.total_size_bytes = versionsResult.versions.reduce((sum, v) => sum + (v.size_bytes || 0), 0);
                }
            }
            
            this.renderVersionHistory(versionsResult.versions, stats);
            this.versionHistoryModal.style.display = 'block';
            
        } catch (error) {
            this.showNotification('Error loading version history: ' + error.message, 'error');
        }
    }
    
    hideVersionHistory() {
        this.versionHistoryModal.style.display = 'none';
    }
    
    async showManageSharesModal() {
        if (!this.currentFileId) {
            this.showNotification('No file loaded', 'error');
            return;
        }
        
        try {
            // Load current shares and pending shares
            const response = await fetch(`/backend/index.php/files/${this.currentFileId}/shares`);
            const result = await response.json();
            
            if (!result.success) {
                this.showNotification('Failed to load shares: ' + (result.error || 'Unknown error'), 'error');
                return;
            }
            
            // Update modal title
            this.manageSharesTitle.textContent = `Manage Shares - ${this.editorTitle.textContent}`;
            
            // Render current shares
            this.renderSharesList(result.shares, this.currentSharesList, 'current');
            
            // Render pending shares
            this.renderSharesList(result.pending_shares, this.pendingSharesList, 'pending');
            
            // Clear the add share form
            this.shareEmail.value = '';
            this.shareCanEdit.checked = false;
            
            // Show modal
            this.manageSharesModal.style.display = 'block';
            
        } catch (error) {
            this.showNotification('Error loading shares: ' + error.message, 'error');
        }
    }
    
    hideManageSharesModal() {
        this.manageSharesModal.style.display = 'none';
    }
    
    renderSharesList(shares, container, type) {
        if (!shares || shares.length === 0) {
            container.innerHTML = '<div class="empty-state">No shares</div>';
            return;
        }
        
        container.innerHTML = shares.map(share => {
            const isCurrentShare = type === 'current';
            const email = isCurrentShare ? share.email : share.shared_with_email;
            const canEdit = share.can_edit;
            const createdAt = new Date(share.created_at).toLocaleDateString();
            const userName = isCurrentShare ? share.name : null;
            
            return `
                <div class="share-item">
                    <div class="share-info">
                        <div class="share-email">${email}</div>
                        <div class="share-details">
                            ${userName ? `${userName} • ` : ''}Added ${createdAt}
                        </div>
                    </div>
                    <div class="share-permissions">
                        <span class="permission-badge ${canEdit ? 'permission-edit' : 'permission-view'}">
                            ${canEdit ? 'Edit' : 'View'}
                        </span>
                    </div>
                    <div class="share-actions">
                        <button class="btn btn-danger btn-sm" onclick="editor.removeShare('${type}', ${share.id})">
                            Remove
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    }
    
    async removeShare(type, shareId) {
        if (!confirm('Are you sure you want to remove this share?')) {
            return;
        }
        
        try {
            const endpoint = type === 'current' 
                ? `/backend/index.php/files/${this.currentFileId}/shares/${shareId}`
                : `/backend/index.php/files/${this.currentFileId}/pending-shares/${shareId}`;
                
            const response = await fetch(endpoint, {
                method: 'DELETE'
            });
            
            const result = await response.json();
            if (result.success) {
                this.showNotification('Share removed successfully', 'success');
                // Refresh the shares modal
                this.showManageSharesModal();
            } else {
                this.showNotification('Failed to remove share: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error removing share: ' + error.message, 'error');
        }
    }
    
    async addNewShare() {
        const email = this.shareEmail.value.trim();
        const canEdit = this.shareCanEdit.checked;
        
        if (!email) {
            this.showNotification('Please enter an email address', 'error');
            return;
        }
        
        try {
            const response = await fetch('/backend/index.php/files/share', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    file_id: this.currentFileId,
                    email: email,
                    can_edit: canEdit
                })
            });
            
            const result = await response.json();
            if (result.success) {
                this.showNotification('File shared successfully!', 'success');
                // Clear form and refresh shares
                this.shareEmail.value = '';
                this.shareCanEdit.checked = false;
                this.showManageSharesModal();
            } else {
                this.showNotification('Failed to share file: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error sharing file: ' + error.message, 'error');
        }
    }
    
    showUploadVersionModal() {
        this.uploadVersionModal.style.display = 'block';
        this.versionFile.value = '';
        this.versionComment.value = '';
        this.uploadFileInfo.textContent = '';
        this.uploadFileInfo.className = 'file-info';
        this.uploadProgress.style.display = 'none';
        this.uploadProgressFill.style.width = '0%';
    }
    
    hideUploadVersionModal() {
        this.uploadVersionModal.style.display = 'none';
    }
    
    validateUploadFile(file) {
        if (!file) {
            this.uploadFileInfo.textContent = '';
            this.uploadFileInfo.className = 'file-info';
            return false;
        }
        
        // Check file extension
        if (!file.name.toLowerCase().endsWith('.xcstrings')) {
            this.uploadFileInfo.textContent = 'Please select a .xcstrings file';
            this.uploadFileInfo.className = 'file-info invalid';
            return false;
        }
        
        // Check file size (max 10MB)
        const maxSize = 10 * 1024 * 1024;
        if (file.size > maxSize) {
            this.uploadFileInfo.textContent = 'File is too large (max 10MB)';
            this.uploadFileInfo.className = 'file-info invalid';
            return false;
        }
        
        this.uploadFileInfo.textContent = `Selected: ${file.name} (${Math.round(file.size / 1024)} KB)`;
        this.uploadFileInfo.className = 'file-info valid';
        return true;
    }
    
    async handleVersionUpload() {
        const file = this.versionFile.files[0];
        if (!file) {
            this.showNotification('Please select a file', 'error');
            return;
        }
        
        if (!this.validateUploadFile(file)) {
            return;
        }
        
        if (!this.currentFileId) {
            this.showNotification('No file currently open', 'error');
            return;
        }
        
        try {
            // Show progress
            this.uploadProgress.style.display = 'block';
            this.uploadProgressText.textContent = 'Uploading...';
            this.uploadProgressFill.style.width = '0%';
            
            const formData = new FormData();
            formData.append('file', file);
            formData.append('comment', this.versionComment.value || 'Uploaded new version');
            
            const response = await fetch(`/backend/index.php/files/${this.currentFileId}/upload-version`, {
                method: 'POST',
                body: formData
            });
            
            // Simulate progress for better UX
            let progress = 0;
            const progressInterval = setInterval(() => {
                progress += 10;
                this.uploadProgressFill.style.width = Math.min(progress, 90) + '%';
            }, 100);
            
            const result = await response.json();
            
            clearInterval(progressInterval);
            this.uploadProgressFill.style.width = '100%';
            this.uploadProgressText.textContent = 'Complete!';
            
            if (result.success) {
                this.showNotification(result.message || 'Version uploaded successfully', 'success');
                this.hideUploadVersionModal();
                
                // Refresh version history if open
                if (this.versionHistoryModal.style.display === 'block') {
                    await this.showVersionHistory();
                }
                
                // Refresh the current file to show the new version
                await this.loadFile(this.currentFileId);
            } else {
                this.showNotification(result.error || 'Upload failed', 'error');
            }
        } catch (error) {
            console.error('Upload error:', error);
            this.showNotification('Upload failed: ' + error.message, 'error');
        } finally {
            setTimeout(() => {
                this.uploadProgress.style.display = 'none';
            }, 1000);
        }
    }
    
    showPresignedUrlModal() {
        this.presignedUrlModal.style.display = 'block';
        this.urlCommentPrefix.value = '';
        this.generatedUrlSection.style.display = 'none';
        this.loadExistingPresignedUrls();
    }
    
    hidePresignedUrlModal() {
        this.presignedUrlModal.style.display = 'none';
    }
    
    async generatePresignedUrl() {
        if (!this.currentFileId) {
            this.showNotification('No file currently open', 'error');
            return;
        }
        
        try {
            const response = await fetch(`/backend/index.php/files/${this.currentFileId}/generate-upload-url`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    comment_prefix: this.urlCommentPrefix.value || null
                })
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.displayGeneratedUrl(result.data);
                this.showNotification('Upload URL generated successfully', 'success');
                // Refresh existing URLs
                this.loadExistingPresignedUrls();
            } else {
                this.showNotification(result.error || 'Failed to generate URL', 'error');
            }
        } catch (error) {
            console.error('Generate URL error:', error);
            this.showNotification('Failed to generate URL: ' + error.message, 'error');
        }
    }
    
    displayGeneratedUrl(data) {
        this.generatedUrl.value = data.upload_url;
        this.urlExpiration.textContent = new Date(data.expires_at).toLocaleString();
        
        // Generate curl example
        const curlCommand = `curl -X PUT "${data.upload_url}" \\
  -H "Content-Type: application/json" \\
  -H "X-Comment: Your version comment here" \\
  --data-binary "@your-file.xcstrings"`;
        
        this.curlExample.textContent = curlCommand;
        this.generatedUrlSection.style.display = 'block';
    }
    
    async loadExistingPresignedUrls() {
        if (!this.currentFileId) return;
        
        try {
            const response = await fetch(`/backend/index.php/files/${this.currentFileId}/upload-urls`);
            const result = await response.json();
            
            if (result.success) {
                this.renderExistingUrls(result.urls);
            }
        } catch (error) {
            console.error('Error loading existing URLs:', error);
        }
    }
    
    renderExistingUrls(urls) {
        if (!urls || urls.length === 0) {
            this.existingUrlsList.innerHTML = '<p class="empty-state">No upload URLs created yet</p>';
            return;
        }
        
        this.existingUrlsList.innerHTML = urls.map(url => {
            const isExpired = new Date(url.expires_at) < new Date();
            const isUsed = url.is_used;
            
            let statusClass = '';
            let statusText = 'Active';
            
            if (isUsed) {
                statusClass = 'used';
                statusText = `Used ${new Date(url.used_at).toLocaleDateString()}`;
            } else if (isExpired) {
                statusClass = 'expired';
                statusText = 'Expired';
            }
            
            return `
                <div class="url-item ${statusClass}">
                    <div class="url-item-info">
                        <div class="url-item-title">
                            ${url.comment_prefix || 'Upload URL'} 
                            <span class="status">(${statusText})</span>
                        </div>
                        <div class="url-item-details">
                            Created: ${new Date(url.created_at).toLocaleDateString()} | 
                            Expires: ${new Date(url.expires_at).toLocaleDateString()}
                        </div>
                    </div>
                    <div class="url-item-actions">
                        ${!isUsed && !isExpired ? `
                            <button class="btn btn-sm btn-secondary" onclick="editor.copyToClipboard('${this.buildUploadUrl(url.token)}')">
                                Copy URL
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="editor.revokePresignedUrl(${url.id})">
                                Revoke
                            </button>
                        ` : ''}
                    </div>
                </div>
            `;
        }).join('');
    }
    
    buildUploadUrl(token) {
        const baseUrl = window.location.origin;
        return `${baseUrl}/backend/index.php/upload/${token}`;
    }
    
    async revokePresignedUrl(urlId) {
        const confirmed = await this.showConfirmDialog(
            'Revoke Upload URL',
            'Are you sure you want to revoke this upload URL? It will no longer be usable.'
        );
        
        if (!confirmed) return;
        
        try {
            const response = await fetch(`/backend/index.php/files/${this.currentFileId}/upload-urls/${urlId}`, {
                method: 'DELETE'
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showNotification('Upload URL revoked successfully', 'success');
                this.loadExistingPresignedUrls();
            } else {
                this.showNotification(result.error || 'Failed to revoke URL', 'error');
            }
        } catch (error) {
            console.error('Revoke URL error:', error);
            this.showNotification('Failed to revoke URL: ' + error.message, 'error');
        }
    }
    
    copyToClipboard(text) {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text).then(() => {
                this.showNotification('Copied to clipboard', 'success');
            }).catch(err => {
                console.error('Failed to copy:', err);
                this.fallbackCopyToClipboard(text);
            });
        } else {
            this.fallbackCopyToClipboard(text);
        }
    }
    
    fallbackCopyToClipboard(text) {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        try {
            document.execCommand('copy');
            this.showNotification('Copied to clipboard', 'success');
        } catch (err) {
            this.showNotification('Failed to copy to clipboard', 'error');
        }
        document.body.removeChild(textArea);
    }
    
    renderVersionHistory(versions, stats) {
        const fileName = this.editorTitle.textContent;
        this.versionHistoryTitle.textContent = `Version History - ${fileName}`;
        
        // Ensure stats is valid object with default values
        const safeStats = {
            total_versions: stats.total_versions || 0,
            unique_contributors: stats.unique_contributors || 0,
            total_size_bytes: stats.total_size_bytes || 0
        };
        
        // Update stats
        this.versionCount.textContent = `${safeStats.total_versions} version${safeStats.total_versions !== 1 ? 's' : ''}`;
        this.versionContributors.textContent = `${safeStats.unique_contributors} contributor${safeStats.unique_contributors !== 1 ? 's' : ''}`;
        this.versionSize.textContent = `${Math.round(safeStats.total_size_bytes / 1024)} KB total`;
        
        // Render version list
        if (!versions || !Array.isArray(versions) || versions.length === 0) {
            this.versionList.innerHTML = '<p class="empty-state">No versions found</p>';
            return;
        }
        
        const currentVersionNumber = Math.max(...versions.map(v => v.version_number || 0));
        
        this.versionList.innerHTML = versions.map(version => {
            const isCurrentVersion = (version.version_number || 0) === currentVersionNumber;
            const sizeKB = Math.round((version.size_bytes || 0) / 1024);
            const versionDate = new Date(version.created_at || Date.now());
            
            return `
                <div class="version-item ${isCurrentVersion ? 'current-version' : ''}" data-version="${version.version_number}">
                    <div class="version-header">
                        <div class="version-info">
                            <div class="version-number">
                                Version ${version.version_number || 'Unknown'}
                                ${isCurrentVersion ? ' (Current)' : ''}
                            </div>
                            <div class="version-meta">
                                by ${version.created_by_name || version.created_by_email || 'Unknown'} • 
                                ${versionDate.toLocaleDateString()} ${versionDate.toLocaleTimeString()} • 
                                <span class="version-size">${sizeKB} KB</span>
                            </div>
                            ${version.comment ? `<div class="version-comment">"${this.escapeHtml(version.comment)}"</div>` : ''}
                        </div>
                        <div class="version-actions">
                            <button class="btn btn-secondary btn-sm" onclick="editor.previewVersion(${version.version_number})">
                                Preview
                            </button>
                            ${!isCurrentVersion && this.canEditCurrentFile() ? `
                                <button class="btn btn-primary btn-sm" onclick="editor.revertToVersion(${version.version_number})">
                                    Restore
                                </button>
                            ` : ''}
                            ${!isCurrentVersion && this.canEditCurrentFile() && versions.length > 1 ? `
                                <button class="btn btn-danger btn-sm" onclick="editor.deleteVersion(${version.version_number})">
                                    Delete
                                </button>
                            ` : ''}
                        </div>
                    </div>
                    <div class="version-preview" id="version-preview-${version.version_number}">
                        <pre></pre>
                    </div>
                </div>
            `;
        }).join('');
    }
    
    async previewVersion(versionNumber) {
        const previewElement = document.getElementById(`version-preview-${versionNumber}`);
        const preElement = previewElement.querySelector('pre');
        
        if (previewElement.classList.contains('expanded')) {
            // Hide preview
            previewElement.classList.remove('expanded');
            return;
        }
        
        try {
            const response = await fetch(`/backend/index.php/files/${this.currentFileId}/versions/${versionNumber}`);
            const result = await response.json();
            
            if (result.success) {
                // Format the JSON content nicely
                const content = JSON.parse(result.version.content);
                const formattedContent = JSON.stringify(content, null, 2);
                
                // Truncate if too long
                const maxLength = 2000;
                const displayContent = formattedContent.length > maxLength 
                    ? formattedContent.substring(0, maxLength) + '\n... (truncated)'
                    : formattedContent;
                
                preElement.textContent = displayContent;
                previewElement.classList.add('expanded');
            } else {
                this.showNotification('Failed to load version content', 'error');
            }
        } catch (error) {
            this.showNotification('Error loading version: ' + error.message, 'error');
        }
    }
    
    async revertToVersion(versionNumber) {
        const shouldRevert = await this.showConfirmDialog(
            'Restore Version',
            `Are you sure you want to restore to version ${versionNumber}? This will create a new version with the content from version ${versionNumber}.`
        );
        
        if (!shouldRevert) return;
        
        const comment = await this.showInputDialog(
            'Restore Version',
            'Enter a comment for this restoration:',
            `Restored to version ${versionNumber}`
        );
        
        if (comment === null) return;
        
        try {
            const response = await fetch(`/backend/index.php/files/${this.currentFileId}/revert`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    version_number: versionNumber,
                    comment: comment
                })
            });
            
            const result = await response.json();
            if (result.success) {
                this.showNotification('Version restored successfully', 'success');
                // Reload the file to show the restored content
                await this.loadFile(this.currentFileId);
                // Refresh version history
                await this.showVersionHistory();
            } else {
                this.showNotification('Failed to restore version: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error restoring version: ' + error.message, 'error');
        }
    }
    
    async deleteVersion(versionNumber) {
        const shouldDelete = await this.showConfirmDialog(
            'Delete Version',
            `Are you sure you want to delete version ${versionNumber}? This action cannot be undone.`
        );
        
        if (!shouldDelete) return;
        
        try {
            const response = await fetch(`/backend/index.php/files/${this.currentFileId}/versions/${versionNumber}`, {
                method: 'DELETE'
            });
            
            const result = await response.json();
            if (result.success) {
                this.showNotification('Version deleted successfully', 'success');
                // Refresh version history
                await this.showVersionHistory();
            } else {
                this.showNotification('Failed to delete version: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error deleting version: ' + error.message, 'error');
        }
    }
    
    canEditCurrentFile() {
        // Check if current user can edit the current file
        // For now, return true if user is logged in and has a file loaded
        return this.currentUser && this.currentFileId;
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Debug function for testing data integrity
    debugDataIntegrity() {
        console.log('=== DATA INTEGRITY DEBUG ===');
        console.log('Full data structure:', this.data);
        
        if (this.data && this.data.strings) {
            Object.keys(this.data.strings).forEach(stringKey => {
                const stringData = this.data.strings[stringKey];
                console.log(`String: ${stringKey}`);
                console.log(`  Comment: ${stringData.comment}`);
                console.log(`  Localizations:`, stringData.localizations);
                
                if (stringData.localizations) {
                    Object.keys(stringData.localizations).forEach(lang => {
                        const loc = stringData.localizations[lang];
                        if (loc.stringUnit) {
                            console.log(`    ${lang}: "${loc.stringUnit.value}" (${loc.stringUnit.state})`);
                        } else if (loc.variations) {
                            console.log(`    ${lang}: [variations]`);
                        }
                    });
                }
            });
        }
        
        console.log('=== END DEBUG ===');
        return this.data;
    }

    // Invite management methods
    updateInviteUI() {
        if (this.config?.can_create_invites && this.currentUser) {
            this.invitesTab.style.display = 'block';
        } else {
            this.invitesTab.style.display = 'none';
        }
    }

    async createInvite() {
        try {
            const email = await this.showInputDialog(
                'Create Invite', 
                'Email (optional - leave empty for general invite):', 
                ''
            );
            
            if (email === null) return;
            
            const payload = {};
            if (email) {
                payload.email = email;
            }
            
            const response = await fetch('/backend/index.php/auth/invites/create', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            
            const result = await response.json();
            if (result.success) {
                this.showNotification('Invite created successfully!', 'success');
                this.loadUserInvites();
                
                const baseUrl = window.location.origin + window.location.pathname;
                const inviteUrl = `${baseUrl}?invite=${result.invite.token}`;
                
                this.showInviteCreatedDialog(inviteUrl, result.invite.expires_at);
            } else {
                this.showNotification('Failed to create invite: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error creating invite: ' + error.message, 'error');
        }
    }

    async loadUserInvites() {
        if (!this.currentUser) return;
        
        try {
            const response = await fetch('/backend/index.php/auth/invites/my');
            const result = await response.json();
            
            if (result.success) {
                this.renderInvitesList(result.invites);
            } else {
                this.showNotification('Failed to load invites: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error loading invites: ' + error.message, 'error');
        }
    }

    renderInvitesList(invites) {
        if (!invites || invites.length === 0) {
            this.invitesList.innerHTML = '<p class="no-items">No invites created yet.</p>';
            return;
        }

        const invitesHtml = invites.map(invite => {
            const isExpired = new Date(invite.expires_at) < new Date();
            const isUsed = invite.used_at;
            let statusText = 'Active';
            let statusClass = 'status-active';
            
            if (isUsed) {
                statusText = `Used by ${invite.used_by_name}`;
                statusClass = 'status-used';
            } else if (isExpired) {
                statusText = 'Expired';
                statusClass = 'status-expired';
            }
            
            const baseUrl = window.location.origin + window.location.pathname;
            const inviteUrl = `${baseUrl}?invite=${invite.token}`;
            
            return `
                <div class="invite-item">
                    <div class="invite-info">
                        <div class="invite-email">${invite.email || 'General invite'}</div>
                        <div class="invite-meta">
                            <span class="invite-created">Created: ${new Date(invite.created_at).toLocaleDateString()}</span>
                            <span class="invite-expires">Expires: ${new Date(invite.expires_at).toLocaleDateString()}</span>
                            <span class="invite-status ${statusClass}">${statusText}</span>
                        </div>
                        ${!isUsed && !isExpired ? `<div class="invite-url">${inviteUrl}</div>` : ''}
                    </div>
                    <div class="invite-actions">
                        ${!isUsed && !isExpired ? `
                            <button class="btn btn-sm btn-secondary" onclick="editor.copyToClipboard('${inviteUrl}')">Copy Link</button>
                            <button class="btn btn-sm btn-danger" onclick="editor.revokeInvite(${invite.id})">Revoke</button>
                        ` : ''}
                    </div>
                </div>
            `;
        }).join('');

        this.invitesList.innerHTML = invitesHtml;
    }

    async revokeInvite(inviteId) {
        try {
            const confirmed = await this.showConfirmDialog(
                'Revoke Invite',
                'Are you sure you want to revoke this invite? This action cannot be undone.'
            );
            
            if (!confirmed) return;
            
            const response = await fetch(`/backend/index.php/auth/invites/${inviteId}`, {
                method: 'DELETE'
            });
            
            const result = await response.json();
            if (result.success) {
                this.showNotification('Invite revoked successfully', 'success');
                this.loadUserInvites();
            } else {
                this.showNotification('Failed to revoke invite: ' + result.error, 'error');
            }
        } catch (error) {
            this.showNotification('Error revoking invite: ' + error.message, 'error');
        }
    }

    copyToClipboard(text) {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text).then(() => {
                this.showNotification('Copied to clipboard!', 'success');
            }).catch(() => {
                this.fallbackCopyToClipboard(text);
            });
        } else {
            this.fallbackCopyToClipboard(text);
        }
    }

    fallbackCopyToClipboard(text) {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        
        try {
            document.execCommand('copy');
            this.showNotification('Copied to clipboard!', 'success');
        } catch (err) {
            this.showNotification('Failed to copy to clipboard', 'error');
        }
        
        document.body.removeChild(textArea);
    }

    async showInviteCreatedDialog(inviteUrl, expiresAt) {
        const confirmed = await this.showConfirmDialog(
            'Invite Created Successfully',
            `Your invite link has been created!\n\nExpires: ${new Date(expiresAt).toLocaleDateString()}\n\n${inviteUrl}\n\nClick OK to copy the link to your clipboard.`
        );
        
        if (confirmed) {
            this.copyToClipboard(inviteUrl);
        }
    }
}

// Initialize the editor
const editor = new XCStringEditor();