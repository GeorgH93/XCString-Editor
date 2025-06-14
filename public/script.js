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
    }

    setupEventListeners() {
        // Original listeners
        this.uploadArea.addEventListener('click', () => this.fileInput.click());
        this.uploadArea.addEventListener('dragover', this.handleDragOver.bind(this));
        this.uploadArea.addEventListener('dragleave', this.handleDragLeave.bind(this));
        this.uploadArea.addEventListener('drop', this.handleDrop.bind(this));
        this.fileInput.addEventListener('change', this.handleFileSelect.bind(this));
        this.addStringBtn.addEventListener('click', this.addNewString.bind(this));
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
                alert('OAuth2 login failed: ' + urlParams.get('oauth_error'));
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
                alert('Login failed: ' + result.error);
            }
        } catch (error) {
            alert('Login error: ' + error.message);
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
                alert('Registration successful! Please login.');
                this.showLoginForm();
            } else {
                alert('Registration failed: ' + result.error);
            }
        } catch (error) {
            alert('Registration error: ' + error.message);
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
        this.uploadSection.style.display = 'none';
    }

    hideFileManagement() {
        this.fileManagementSection.style.display = 'none';
        this.uploadSection.style.display = 'block';
    }

    hideEditor() {
        this.editorSection.style.display = 'none';
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
                alert('Failed to load file: ' + result.error);
            }
        } catch (error) {
            alert('Error loading file: ' + error.message);
        }
    }

    async deleteFile(fileId) {
        if (!confirm('Are you sure you want to delete this file?')) return;
        
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
                alert('Failed to delete file: ' + result.error);
            }
        } catch (error) {
            alert('Error deleting file: ' + error.message);
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
        
        const fileName = this.currentFileId ? 
            this.editorTitle.textContent : 
            prompt('Enter file name:', 'Untitled.xcstrings');
        
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
                alert('Failed to save file: ' + result.error);
            }
        } catch (error) {
            alert('Error saving file: ' + error.message);
        }
    }

    async shareCurrentFile() {
        if (!this.currentFileId) return;
        
        const email = prompt('Enter email address to share with:');
        if (!email) return;
        
        const canEdit = confirm('Allow editing? (Cancel for read-only access)');
        
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
                alert('File shared successfully!');
            } else {
                alert('Failed to share file: ' + result.error);
            }
        } catch (error) {
            alert('Error sharing file: ' + error.message);
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
                this.fileInfo.textContent = 'Uploaded file (not saved)';
                
                // Show save button if user is logged in
                this.saveBtn.style.display = this.currentUser ? 'inline-block' : 'none';
                this.shareBtn.style.display = 'none';
                
                this.renderEditor();
            } else {
                alert('Error parsing file: ' + result.error);
            }
        } catch (error) {
            alert('Error processing file: ' + error.message);
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
        this.editorSection.style.display = 'block';
        this.stringsContainer.innerHTML = '';

        if (this.data && this.data.strings) {
            Object.keys(this.data.strings).forEach(key => {
                this.renderStringEntry(key, this.data.strings[key]);
            });
        }
    }

    renderStringEntry(key, stringData) {
        const entryDiv = document.createElement('div');
        entryDiv.className = 'string-entry';
        entryDiv.dataset.key = key;

        const comment = stringData.comment || '';
        const localizations = stringData.localizations || {};

        entryDiv.innerHTML = `
            <div class="string-entry-header">
                <div class="string-key">${key}</div>
                <button class="btn btn-danger btn-sm delete-string-btn" onclick="editor.deleteString('${key}')">Delete</button>
            </div>
            <div class="string-details">
                <div class="form-group">
                    <label>Key:</label>
                    <input type="text" class="string-key-input" value="${key}" onchange="editor.updateStringKey('${key}', this.value)">
                </div>
                <div class="form-group">
                    <label>Comment:</label>
                    <textarea class="string-comment-input" onchange="editor.updateStringComment('${key}', this.value)">${comment}</textarea>
                </div>
                <div class="form-group">
                    <label>Localizations:</label>
                    <div class="localizations" data-key="${key}">
                        ${this.renderLocalizations(key, localizations)}
                    </div>
                    <button class="btn btn-secondary add-localization-btn" onclick="editor.addLocalization('${key}')">Add Localization</button>
                </div>
            </div>
        `;

        this.stringsContainer.appendChild(entryDiv);
    }

    renderLocalizations(stringKey, localizations) {
        let html = '';
        Object.keys(localizations).forEach(lang => {
            const localization = localizations[lang];
            const value = localization.stringUnit ? localization.stringUnit.value : '';
            html += `
                <div class="localization-entry">
                    <input type="text" value="${lang}" onchange="editor.updateLocalizationLang('${stringKey}', '${lang}', this.value)" placeholder="Language">
                    <input type="text" value="${value}" onchange="editor.updateLocalizationValue('${stringKey}', '${lang}', this.value)" placeholder="Translation">
                    <button class="btn btn-danger btn-sm" onclick="editor.deleteLocalization('${stringKey}', '${lang}')">×</button>
                </div>
            `;
        });
        return html;
    }

    addNewString() {
        const key = prompt('Enter string key:');
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

    addLocalization(stringKey) {
        const lang = prompt('Enter language code (e.g., es, fr, de):');
        if (lang && lang.trim()) {
            if (!this.data.strings[stringKey].localizations[lang]) {
                this.data.strings[stringKey].localizations[lang] = {
                    stringUnit: {
                        state: 'translated',
                        value: ''
                    }
                };
                this.markModified();
                this.renderEditor();
            }
        }
    }

    deleteLocalization(stringKey, lang) {
        if (confirm(`Delete localization for "${lang}"?`)) {
            delete this.data.strings[stringKey].localizations[lang];
            this.markModified();
            this.renderEditor();
        }
    }

    markModified() {
        this.isModified = true;
        if (this.fileInfo) {
            this.fileInfo.textContent = this.fileInfo.textContent.replace(' (saved)', '') + ' (modified)';
        }
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
                alert('Error generating file: ' + result.error);
            }
        } catch (error) {
            alert('Error exporting file: ' + error.message);
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
}

// Initialize the editor
const editor = new XCStringEditor();