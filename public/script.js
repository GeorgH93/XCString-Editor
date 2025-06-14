class XCStringEditor {
    constructor() {
        this.data = null;
        this.currentUser = null;
        this.currentFileId = null;
        this.isModified = false;
        this.config = null;
        
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
    }

    async checkAuthStatus() {
        try {
            const response = await fetch('/backend/index.php/auth/user');
            const result = await response.json();
            
            if (result.success) {
                this.currentUser = result.user;
                this.config = result.config;
                this.updateAuthUI();
                this.updateOAuth2UI();
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
            
            return `
                <a href="/backend/index.php/auth/oauth/${provider}/redirect" class="oauth2-btn ${provider}">
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
    
    getProviderDisplayName(provider) {
        const names = {
            'google': 'Google',
            'github': 'GitHub',
            'microsoft': 'Microsoft',
            'gitlab': 'GitLab'
        };
        return names[provider] || provider;
    }
    
    getProviderIcon(provider) {
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
        return icons[provider] || '';
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
        
        try {
            const response = await fetch('/backend/index.php/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, email, password })
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
                this.data = JSON.parse(result.file.content);
                this.currentFileId = fileId;
                this.isModified = false;
                this.editorTitle.textContent = result.file.name;
                this.fileInfo.textContent = `by ${result.file.owner_name} • Updated: ${new Date(result.file.updated_at).toLocaleDateString()}`;
                
                // Show appropriate buttons
                this.saveBtn.style.display = this.currentUser ? 'inline-block' : 'none';
                this.shareBtn.style.display = this.currentUser ? 'inline-block' : 'none';
                
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
            const content = JSON.stringify(this.data);
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
            const response = await fetch('/backend/index.php/files/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: fileName,
                    content: JSON.stringify(this.data),
                    is_public: false
                })
            });
            
            const result = await response.json();
            if (result.success) {
                this.currentFileId = result.file_id;
                this.fileInfo.textContent = 'Saved';
                this.saveBtn.style.display = 'inline-block';
                this.shareBtn.style.display = 'inline-block';
                this.loadUserFiles(); // Refresh file list
                this.showNotification('File saved successfully!', 'success');
            } else {
                this.showNotification('Failed to save file: ' + result.error, 'error');
                this.fileInfo.textContent = 'Uploaded file (not saved)';
                this.saveBtn.style.display = 'inline-block';
                this.shareBtn.style.display = 'none';
            }
        } catch (error) {
            this.showNotification('Error saving file: ' + error.message, 'error');
            this.fileInfo.textContent = 'Uploaded file (not saved)';
            this.saveBtn.style.display = 'inline-block';
            this.shareBtn.style.display = 'none';
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
            'Allow editing? (Choose "Cancel" for read-only access)'
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
                this.data = result.data;
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
                    }
                } else {
                    this.fileInfo.textContent = 'Uploaded file (not saved)';
                    this.saveBtn.style.display = 'none';
                    this.shareBtn.style.display = 'none';
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
        }
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

    renderStringEntry(key, stringData) {
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
        keyInput.value = key; // Set value directly (no escaping needed)
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
        commentInput.value = comment; // Set value directly (no escaping needed)
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
        
        const langInput = document.createElement('input');
        langInput.type = 'text';
        langInput.value = lang;
        langInput.placeholder = 'Language';
        langInput.className = 'localization-lang-input';
        
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
        });
        
        stateSelect.addEventListener('change', (e) => {
            this.updateLocalizationState(stringKey, lang, e.target.value);
        });
        
        deleteBtn.addEventListener('click', () => {
            this.deleteLocalization(stringKey, lang);
        });
        
        entryDiv.appendChild(langInput);
        entryDiv.appendChild(valueInput);
        entryDiv.appendChild(stateSelect);
        entryDiv.appendChild(deleteBtn);
        
        container.appendChild(entryDiv);
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
        }
    }

    deleteString(key) {
        if (confirm(`Delete string "${key}"?`)) {
            delete this.data.strings[key];
            const entry = document.querySelector(`[data-key="${key}"]`);
            if (entry) {
                entry.remove();
            }
            this.markModified();
        }
    }

    updateStringKey(oldKey, newKey) {
        if (oldKey !== newKey && newKey.trim()) {
            this.data.strings[newKey] = this.data.strings[oldKey];
            delete this.data.strings[oldKey];
            this.markModified();
            this.renderEditor();
        }
    }

    updateStringComment(key, comment) {
        if (this.data.strings[key]) {
            this.data.strings[key].comment = comment;
            this.markModified();
        }
    }

    updateLocalizationValue(stringKey, lang, value) {
        if (this.data.strings[stringKey] && this.data.strings[stringKey].localizations[lang]) {
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
            this.renderEditor();
        }
    }

    async addLocalization(stringKey) {
        const lang = await this.showInputDialog('Add Localization', 'Enter language code (e.g., es, fr, de):', '');
        if (lang && lang.trim()) {
            // Ensure localizations object exists
            if (!this.data.strings[stringKey].localizations) {
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
                this.renderEditor();
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
        this.renderEditor();
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
            this.renderEditor();
        }
    }

    // Variation management functions
    updateLocalizationState(stringKey, lang, newState) {
        if (this.data.strings[stringKey] && this.data.strings[stringKey].localizations[lang]) {
            if (this.data.strings[stringKey].localizations[lang].stringUnit) {
                this.data.strings[stringKey].localizations[lang].stringUnit.state = newState;
                this.markModified();
            }
        }
    }

    updateVariationKey(stringKey, lang, variationType, oldKey, newKey) {
        if (oldKey !== newKey && newKey.trim()) {
            const localization = this.data.strings[stringKey].localizations[lang];
            if (localization.variations && localization.variations[variationType] && localization.variations[variationType][oldKey]) {
                localization.variations[variationType][newKey] = localization.variations[variationType][oldKey];
                delete localization.variations[variationType][oldKey];
                this.markModified();
                this.renderEditor();
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
            this.renderEditor();
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
            this.renderEditor();
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
                this.renderEditor();
            }
        }
    }

    markModified() {
        this.isModified = true;
        if (this.fileInfo) {
            this.fileInfo.textContent = this.fileInfo.textContent.replace(' (saved)', '') + ' (modified)';
        }
        // Update progress indicators when data changes
        this.updateProgressIndicators();
    }

    async exportFile() {
        try {
            const response = await fetch('/backend/index.php/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ data: this.data })
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
            this.confirmTitle.textContent = title;
            this.confirmMessage.textContent = message;
            this.confirmModal.style.display = 'block';
            
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
    }

    // Input Modal
    showInputDialog(title, label, defaultValue = '') {
        return new Promise((resolve) => {
            this.inputTitle.textContent = title;
            this.inputLabel.textContent = label;
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
        this.inputField.value = '';
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
}

// Initialize the editor
const editor = new XCStringEditor();