class XCStringEditor {
    constructor() {
        this.data = null;
        this.initializeElements();
        this.setupEventListeners();
    }

    initializeElements() {
        this.uploadArea = document.getElementById('uploadArea');
        this.fileInput = document.getElementById('fileInput');
        this.editorSection = document.getElementById('editorSection');
        this.stringsContainer = document.getElementById('stringsContainer');
        this.addStringBtn = document.getElementById('addStringBtn');
        this.exportBtn = document.getElementById('exportBtn');
    }

    setupEventListeners() {
        this.uploadArea.addEventListener('click', () => this.fileInput.click());
        this.uploadArea.addEventListener('dragover', this.handleDragOver.bind(this));
        this.uploadArea.addEventListener('dragleave', this.handleDragLeave.bind(this));
        this.uploadArea.addEventListener('drop', this.handleDrop.bind(this));
        this.fileInput.addEventListener('change', this.handleFileSelect.bind(this));
        this.addStringBtn.addEventListener('click', this.addNewString.bind(this));
        this.exportBtn.addEventListener('click', this.exportFile.bind(this));
    }

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
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ content })
            });

            const result = await response.json();
            if (result.success) {
                this.data = result.data;
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
        }
    }

    updateStringKey(oldKey, newKey) {
        if (oldKey !== newKey && newKey.trim()) {
            this.data.strings[newKey] = this.data.strings[oldKey];
            delete this.data.strings[oldKey];
            this.renderEditor();
        }
    }

    updateStringComment(key, comment) {
        if (this.data.strings[key]) {
            this.data.strings[key].comment = comment;
        }
    }

    updateLocalizationValue(stringKey, lang, value) {
        if (this.data.strings[stringKey] && this.data.strings[stringKey].localizations[lang]) {
            this.data.strings[stringKey].localizations[lang].stringUnit.value = value;
        }
    }

    updateLocalizationLang(stringKey, oldLang, newLang) {
        if (oldLang !== newLang && newLang.trim() && this.data.strings[stringKey]) {
            const localizations = this.data.strings[stringKey].localizations;
            localizations[newLang] = localizations[oldLang];
            delete localizations[oldLang];
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
                this.renderEditor();
            }
        }
    }

    deleteLocalization(stringKey, lang) {
        if (confirm(`Delete localization for "${lang}"?`)) {
            delete this.data.strings[stringKey].localizations[lang];
            this.renderEditor();
        }
    }

    async exportFile() {
        try {
            const response = await fetch('/backend/index.php/generate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ data: this.data })
            });

            const result = await response.json();
            if (result.success) {
                this.downloadFile(result.xcstring, 'edited.xcstrings');
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