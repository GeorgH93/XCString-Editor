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
                if (this.currentUser) {
                    this.showFileManagement();
                    this.loadUserFiles();
                }
            }
        } catch (error) {
            console.error('Auth check failed:', error);
        }
    }

    updateAuthUI() {
        if (this.currentUser) {
            this.authSection.innerHTML = `
                <div class="user-info">
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