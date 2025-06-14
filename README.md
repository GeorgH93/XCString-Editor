# XCString Editor

A web-based editor for Apple's .xcstrings localization files with user management and file sharing capabilities.

## License

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0) - see the [LICENSE](LICENSE) file for details.

The AGPL-3.0 ensures that any modifications or improvements to this software, including when used as a web service, must be made available under the same license terms.

## Features

### Core Features
- Upload and parse .xcstrings files
- Visual editor for string keys, comments, and localizations
- Add/edit/delete string entries
- Multi-language localization support
- Export modified files
- Responsive design

### User Management
- User registration and authentication
- Guest mode (local editing without saving)
- Authenticated mode (server-side file storage)
- File sharing between users
- Public file visibility
- Email domain restrictions (configurable)

### File Management
- Save files to server (authenticated users)
- Share files with other users (read-only or edit permissions)
- File versioning and history
- Public file discovery

## Project Structure

```
xcstringtool/
├── backend/
│   ├── index.php          # Main API router
│   ├── Database.php       # Database abstraction layer
│   ├── Auth.php          # Authentication system
│   ├── FileManager.php   # File storage and sharing
│   └── schema.sql        # Database schema
├── public/
│   ├── index.html        # Main web interface
│   ├── styles.css        # CSS styling
│   └── script.js         # Frontend JavaScript
├── config.php            # Configuration file
├── data/                 # SQLite database storage (auto-created)
├── Dockerfile           # Docker configuration
├── package.json         # Project configuration
└── README.md           # This file
```

## Requirements

- PHP 8.0+ with extensions:
  - JSON
  - PDO
  - SQLite3 (for SQLite) / MySQL (for MySQL) / PostgreSQL (for PostgreSQL)
- Modern web browser
- Database (SQLite/MySQL/PostgreSQL)

## Installation & Setup

### Option 1: Docker (Recommended)

```bash
docker build -t xcstring-editor .
docker run -p 8080:80 xcstring-editor
```

### Option 2: Manual Setup

1. Clone/download the project:
   ```bash
   git clone <repository-url>
   cd xcstringtool
   ```

2. Configure the database in `config.php`:
   ```php
   // For SQLite (default)
   'database' => [
       'driver' => 'sqlite',
       'sqlite_path' => __DIR__ . '/data/database.sqlite',
   ],
   
   // For MySQL
   'database' => [
       'driver' => 'mysql',
       'host' => 'localhost',
       'port' => 3306,
       'database' => 'xcstring_editor',
       'username' => 'your_username',
       'password' => 'your_password',
   ],
   ```

3. Start the development server:
   ```bash
   php -S localhost:8080 -t public
   ```

4. Open your browser and visit: http://localhost:8080

## Configuration

Edit `config.php` to customize:

### Database Settings
- **driver**: `sqlite`, `mysql`, or `postgres`
- **connection parameters**: host, port, database name, credentials

### User Registration
- **enabled**: Enable/disable new user registration
- **allowed_domains**: Restrict registration to specific email domains
- **require_email_verification**: Future feature for email verification

### File Limits
- **max_file_size**: Maximum file size (default: 10MB)
- **max_files_per_user**: Maximum files per user (default: 100)

### Session Settings
- **lifetime**: Session duration (default: 7 days)
- **cookie settings**: Security options for session cookies

### OAuth2 Authentication
- **enabled**: Enable/disable OAuth2 authentication
- **base_url**: Your application's base URL for OAuth2 redirects
- **providers**: Configure OAuth2 providers (Google, GitHub, Microsoft, GitLab)

## Usage

### Guest Mode (No Account Required)
1. **Upload File**: Drag and drop or click to select a .xcstrings file
2. **Edit Strings**: Modify keys, comments, and translations
3. **Export**: Download the modified file
4. *Note: Changes are not saved to the server*

### Authenticated Mode (Account Required)
1. **Register/Login**: Create an account or login
2. **File Management**: Access "My Files", "Shared", and "Public" tabs
3. **Create New**: Click "New File" to start from scratch
4. **Save Files**: Click "Save" to store files on the server
5. **Share Files**: Click "Share" to collaborate with other users
6. **Load Files**: Click any file in your lists to edit

### File Sharing
- **Read-only**: Share files for viewing and copying
- **Edit permissions**: Allow others to modify your files
- **Public files**: Make files visible to all users

## API Endpoints

### Authentication
- `POST /backend/index.php/auth/register` - User registration
- `POST /backend/index.php/auth/login` - User login
- `POST /backend/index.php/auth/logout` - User logout
- `GET /backend/index.php/auth/user` - Get current user info

### File Management
- `GET /backend/index.php/files/my` - Get user's files
- `GET /backend/index.php/files/shared` - Get files shared with user
- `GET /backend/index.php/files/public` - Get public files
- `GET /backend/index.php/files/{id}` - Get specific file
- `POST /backend/index.php/files/save` - Save new file
- `POST /backend/index.php/files/update` - Update existing file
- `POST /backend/index.php/files/share` - Share file with user
- `DELETE /backend/index.php/files/{id}` - Delete file

### XCString Processing
- `POST /backend/index.php/parse` - Parse xcstrings content
- `POST /backend/index.php/generate` - Generate xcstrings from data
- `GET /backend/index.php/test` - Test API connectivity

## Database Support

### SQLite (Default)
- Zero configuration
- File-based storage
- Perfect for development and small deployments

### MySQL/MariaDB
- High performance
- Suitable for production environments
- Requires separate database server

### PostgreSQL
- Advanced features
- Enterprise-grade reliability
- Requires separate database server

## Security Features

- Password hashing with PHP's `password_hash()`
- Session-based authentication
- CSRF protection via proper HTTP methods
- Input validation and sanitization
- Email domain restrictions
- File size and count limits

## Development

The application uses:
- **Backend**: PHP with PDO for database abstraction
- **Frontend**: Vanilla JavaScript with modern ES6+ features
- **Styling**: CSS Grid and Flexbox for responsive layout
- **Database**: Multi-engine support (SQLite/MySQL/PostgreSQL)

### Extending the Application

#### Backend Changes
- `backend/index.php` - Main API router
- `backend/Auth.php` - Authentication logic
- `backend/FileManager.php` - File operations
- `backend/Database.php` - Database abstraction

#### Frontend Changes
- `public/script.js` - Application logic
- `public/styles.css` - Styling and layout
- `public/index.html` - HTML structure

#### Database Changes
- `backend/schema.sql` - Database schema
- `config.php` - Configuration options

## OAuth2 Setup Guide

XCString Editor supports OAuth2 authentication with popular providers. This allows users to login with their existing accounts instead of creating new passwords.

### Supported Providers

- **Google** - Gmail/Google Workspace accounts
- **GitHub** - GitHub accounts  
- **Microsoft** - Microsoft/Azure AD accounts
- **GitLab** - GitLab.com or self-hosted GitLab

### Configuration Steps

1. **Enable OAuth2** in `config.php`:
   ```php
   'oauth2' => [
       'enabled' => true,
       'base_url' => 'https://your-domain.com', // Your app's URL
       // ...
   ]
   ```

2. **Configure each provider** you want to support:

#### Google OAuth2 Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google+ API
4. Go to "Credentials" → "Create Credentials" → "OAuth 2.0 Client IDs"
5. Choose "Web application"
6. Add authorized redirect URIs:
   - `https://your-domain.com/backend/index.php/auth/oauth/google/callback`
7. Copy the Client ID and Client Secret to `config.php`:
   ```php
   'google' => [
       'enabled' => true,
       'client_id' => 'your-google-client-id',
       'client_secret' => 'your-google-client-secret',
       'redirect_uri' => 'https://your-domain.com/backend/index.php/auth/oauth/google/callback',
   ],
   ```

#### GitHub OAuth2 Setup

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click "New OAuth App"
3. Fill in the details:
   - Application name: `XCString Editor`
   - Homepage URL: `https://your-domain.com`
   - Authorization callback URL: `https://your-domain.com/backend/index.php/auth/oauth/github/callback`
4. Copy the Client ID and Client Secret to `config.php`:
   ```php
   'github' => [
       'enabled' => true,
       'client_id' => 'your-github-client-id',
       'client_secret' => 'your-github-client-secret',
       'redirect_uri' => 'https://your-domain.com/backend/index.php/auth/oauth/github/callback',
   ],
   ```

#### Microsoft OAuth2 Setup

1. Go to [Azure App Registrations](https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps)
2. Click "New registration"
3. Fill in the details:
   - Name: `XCString Editor`
   - Supported account types: Choose based on your needs
   - Redirect URI: `Web` → `https://your-domain.com/backend/index.php/auth/oauth/microsoft/callback`
4. Go to "Certificates & secrets" → "New client secret"
5. Copy the Application (client) ID and client secret to `config.php`:
   ```php
   'microsoft' => [
       'enabled' => true,
       'client_id' => 'your-microsoft-client-id',
       'client_secret' => 'your-microsoft-client-secret',
       'redirect_uri' => 'https://your-domain.com/backend/index.php/auth/oauth/microsoft/callback',
       'tenant' => 'common', // or specific tenant ID
   ],
   ```

#### GitLab OAuth2 Setup

1. Go to [GitLab Applications](https://gitlab.com/-/profile/applications) (or your GitLab instance)
2. Click "Add new application"
3. Fill in the details:
   - Name: `XCString Editor`
   - Redirect URI: `https://your-domain.com/backend/index.php/auth/oauth/gitlab/callback`
   - Scopes: `read_user`
4. Copy the Application ID and Secret to `config.php`:
   ```php
   'gitlab' => [
       'enabled' => true,
       'client_id' => 'your-gitlab-application-id',
       'client_secret' => 'your-gitlab-secret',
       'redirect_uri' => 'https://your-domain.com/backend/index.php/auth/oauth/gitlab/callback',
       'instance_url' => 'https://gitlab.com', // or your GitLab instance URL
   ],
   ```

### Security Considerations

- Always use HTTPS in production for OAuth2 redirects
- Keep client secrets secure and never commit them to version control
- Use environment variables for sensitive configuration in production
- Regularly rotate OAuth2 client secrets
- Configure appropriate OAuth2 scopes (minimal required permissions)

### User Experience

When OAuth2 is enabled:
- Users see OAuth2 login buttons in the login/register modals
- Users can link multiple OAuth2 providers to one account
- Users can still use email/password if they have set one
- Avatar images are automatically imported from OAuth2 providers

## Troubleshooting

### Common Issues

1. **Database connection failed**
   - Check database configuration in `config.php`
   - Ensure database server is running (MySQL/PostgreSQL)
   - Verify file permissions for SQLite

2. **File upload fails**
   - Check PHP file upload limits (`upload_max_filesize`, `post_max_size`)
   - Verify file permissions on data directory

3. **Session issues**
   - Check PHP session configuration
   - Verify cookie settings in `config.php`

4. **Permission denied errors**
   - Ensure web server has write permissions to data directory
   - Check file ownership and permissions

5. **OAuth2 login fails**
   - Verify OAuth2 provider configuration in `config.php`
   - Check redirect URIs match exactly (including https/http)
   - Ensure OAuth2 is enabled in configuration
   - Check OAuth2 provider application settings

6. **OAuth2 callback errors**
   - Verify the base_url in `config.php` matches your domain
   - Check that OAuth2 provider allows your redirect URI
   - Ensure callback URLs use the correct protocol (https in production)

### Logs

- PHP errors: Check PHP error log
- Database errors: Enable debug mode in `config.php`
- Browser console: Check for JavaScript errors