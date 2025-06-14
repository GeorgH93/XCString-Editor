# XCString Editor

A web-based editor for Apple's .xcstrings localization files, built with PHP backend and JavaScript frontend.

## Features

- Upload and parse .xcstrings files
- Visual editor for string keys, comments, and localizations
- Add/edit/delete string entries
- Multi-language localization support
- Export modified files
- Responsive design

## Project Structure

```
xcstringtool/
├── backend/
│   └── index.php          # PHP API for parsing/generating xcstrings
├── public/
│   ├── index.html         # Main web interface
│   ├── styles.css         # CSS styling
│   └── script.js          # Frontend JavaScript
├── package.json           # Project configuration
└── README.md             # This file
```

## Requirements

- PHP 7.4+ with JSON extension
- Modern web browser
- Local web server (built-in PHP server works)

## Installation & Setup

1. Navigate to the project directory:
   ```bash
   cd xcstringtool
   ```

2. Start the development server:
   ```bash
   npm run dev
   # or
   php -S localhost:8080 -t public
   ```

3. Open your browser and visit: http://localhost:8080

## Usage

1. **Upload File**: Drag and drop or click to select a .xcstrings file
2. **Edit Strings**: Modify keys, comments, and translations in the visual editor
3. **Add Localizations**: Click "Add Localization" to support new languages
4. **Export**: Click "Export" to download the modified .xcstrings file

## API Endpoints

- `GET /backend/index.php/test` - Test API connectivity
- `POST /backend/index.php/parse` - Parse xcstrings content
- `POST /backend/index.php/generate` - Generate xcstrings from data

## Development

The application uses:
- **Backend**: PHP for file processing and API
- **Frontend**: Vanilla JavaScript for the editor interface
- **Styling**: CSS Grid and Flexbox for responsive layout

To extend functionality, modify:
- `backend/index.php` for API changes
- `public/script.js` for frontend logic
- `public/styles.css` for styling updates