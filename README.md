# XCString Editor

A web-based editor for Apple's .xcstrings localization files with user management, file sharing, and AI-powered translation capabilities.

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

## Requirements

- Java 21+
- Maven 3.8+
- Modern web browser
- Database (SQLite/MySQL/PostgreSQL)

## Installation & Setup

### Option 1: Docker (Recommended)

```bash
docker build -t xcstring-editor .
docker run -p 8080:8080 xcstring-editor
```

Or with Docker Compose:

```bash
docker-compose up
```

### Option 2: Manual Setup

1. Clone/download the project:
   ```bash
   git clone <repository-url>
   cd xcstringtool
   ```

2. Configure the database via environment variables or `backend/src/main/resources/application.yml`.

   For SQLite (default), no additional configuration is needed. For MySQL or PostgreSQL, set:
   ```bash
   export DB_DRIVER=mysql        # or 'postgres'
   export DB_HOST=localhost
   export DB_PORT=3306
   export DB_NAME=xcstring_editor
   export DB_USERNAME=your_username
   export DB_PASSWORD=your_password
   ```

3. Build and run with Maven:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

4. Open your browser and visit: http://localhost:8080

## Configuration

All configuration is managed through `application.yml` and can be overridden via environment variables.

### Database Settings
- **DB_DRIVER**: `sqlite`, `mysql`, or `postgres` (default: `sqlite`)
- **DB_HOST**, **DB_PORT**, **DB_NAME**: Connection parameters for MySQL/PostgreSQL
- **DB_USERNAME**, **DB_PASSWORD**: Database credentials
- **DB_SQLITE_PATH**: Path to SQLite database file (default: `./data/database.sqlite`)

### User Registration
- **REGISTRATION_ENABLED**: Enable/disable new user registration (default: `true`)
- **REGISTRATION_ALLOWED_DOMAINS**: Restrict registration to specific email domains
- **REGISTRATION_INVITE_DOMAINS**: Restrict registration to invite-only domains

### File Limits
- **FILES_MAX_FILE_SIZE**: Maximum file size in bytes (default: 10MB)
- **FILES_MAX_FILES_PER_USER**: Maximum files per user (default: 100)

### Session Settings
- **SESSION_LIFETIME**: Session duration in seconds (default: 604800 / 7 days)
- **SESSION_COOKIE_NAME**: Session cookie name
- **SESSION_COOKIE_SECURE**: Enable secure cookies (default: `false`)
- **SESSION_COOKIE_HTTPONLY**: Enable HTTP-only cookies (default: `true`)

### AI Integration
- **AI_ENABLED**: Enable/disable AI features (default: `false`)
- **AI_DEFAULT_PROVIDER**: Default AI provider (`openai`, `anthropic`, `openai-compatible`, `zai`, `deepl`)
- **AI_DEFAULT_MODEL**: Default model to use
- **AI_PROMPTS_DIR**: Directory for customizable prompt templates (default: `./config/prompts`)
- Provider-specific settings: `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, etc.

#### Customizing AI Prompts

The translation and proofreading prompts are bundled inside the JAR and extracted to `AI_PROMPTS_DIR` on first startup:

```
./config/prompts/
├── translation.txt      # Prompt template for AI translation
└── proofreading.txt     # Prompt template for AI proofreading
```

These files are **never overwritten** once they exist, so you can edit them freely to change tone, wording, target audience, or any other instruction. Templates use `${var}` placeholders that are substituted at runtime:

| Template | Placeholders |
| --- | --- |
| `translation.txt` | `${sourceLanguage}`, `${targetLanguage}`, `${itemsJson}` |
| `proofreading.txt` | `${language}`, `${itemsJson}` |

After editing, restart the application to load the updated templates. To reset to defaults, delete the files and restart — they will be re-extracted from the JAR.

### OAuth2 Authentication
- **OAUTH2_ENABLED**: Enable/disable OAuth2 authentication (default: `false`)
- **APP_BASE_URL**: Your application's base URL for OAuth2 redirects
- Provider-specific settings: See [OAuth2 Setup Guide](#oauth2-setup-guide)

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

All endpoints use the `/api/` prefix for backward compatibility with the frontend.

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/user` - Get current user info

### File Management
- `GET /api/files/my` - Get user's files
- `GET /api/files/shared` - Get files shared with user
- `GET /api/files/public` - Get public files
- `GET /api/files/{id}` - Get specific file
- `POST /api/files/save` - Save new file
- `POST /api/files/update` - Update existing file
- `POST /api/files/share` - Share file with user
- `DELETE /api/files/{id}` - Delete file

### XCString Processing
- `POST /api/parse` - Parse xcstrings content
- `POST /api/generate` - Generate xcstrings from data
- `GET /api/test` - Test API connectivity

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

### Migrations

Database schema is managed by [Flyway](https://flywaydb.org/) migrations located in `backend/src/main/resources/db/migration/`. Migrations run automatically on application startup. No manual schema setup is required.

## Security Features

- Password hashing with BCrypt (Spring Security Crypto)
- Session-based authentication
- CSRF protection via proper HTTP methods
- Input validation and sanitization
- Email domain restrictions
- File size and count limits

## Development

The application uses:
- **Backend**: Java 21 + Spring Boot 3.4.5 + Maven
- **ORM**: Spring Data JPA / Hibernate
- **Migrations**: Flyway
- **Frontend**: Vanilla JavaScript with modern ES6+ features
- **Styling**: CSS Grid and Flexbox for responsive layout
- **Database**: Multi-engine support (SQLite/MySQL/PostgreSQL)

### Running Tests

```bash
cd backend
mvn test                  # Run all tests
mvn test -Dtest=AuthControllerTest  # Run specific test class
```

Tests use JUnit 5, Spring Boot Test, MockMvc, and an H2 in-memory database.

### Extending the Application

#### Backend Changes
- `backend/src/main/java/com/xcstring/editor/controller/` - REST controllers
- `backend/src/main/java/com/xcstring/editor/service/` - Business logic
- `backend/src/main/java/com/xcstring/editor/entity/` - JPA entities
- `backend/src/main/java/com/xcstring/editor/repository/` - Data access

#### Frontend Changes
- `public/script.js` - Application logic
- `public/styles.css` - Styling and layout
- `public/index.html` - HTML structure

#### Database Changes
- Add new Flyway migrations in `backend/src/main/resources/db/migration/`
- Follow the naming convention: `V{N}__Description.sql` (e.g., `V11__Add_new_table.sql`)
- Migrations run automatically on next application startup

## OAuth2 Setup Guide

XCString Editor supports OAuth2 authentication with popular providers. This allows users to login with their existing accounts instead of creating new passwords.

### Supported Providers

- **Google** - Gmail/Google Workspace accounts
- **GitHub** - GitHub accounts
- **Microsoft** - Microsoft/Azure AD accounts
- **GitLab** - GitLab.com or self-hosted GitLab

### Configuration Steps

1. **Enable OAuth2** via environment variables:
   ```bash
   export OAUTH2_ENABLED=true
   export APP_BASE_URL=https://your-domain.com
   ```

2. **Configure each provider** you want to support:

#### Google OAuth2 Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google+ API
4. Go to "Credentials" → "Create Credentials" → "OAuth 2.0 Client IDs"
5. Choose "Web application"
6. Add authorized redirect URIs:
   - `https://your-domain.com/api/auth/oauth/google/callback`
7. Set the environment variables:
   ```bash
   export OAUTH2_GOOGLE_ENABLED=true
   export OAUTH2_GOOGLE_CLIENT_ID=your-google-client-id
   export OAUTH2_GOOGLE_CLIENT_SECRET=your-google-client-secret
   ```

#### GitHub OAuth2 Setup

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click "New OAuth App"
3. Fill in the details:
   - Application name: `XCString Editor`
   - Homepage URL: `https://your-domain.com`
   - Authorization callback URL: `https://your-domain.com/api/auth/oauth/github/callback`
4. Set the environment variables:
   ```bash
   export OAUTH2_GITHUB_ENABLED=true
   export OAUTH2_GITHUB_CLIENT_ID=your-github-client-id
   export OAUTH2_GITHUB_CLIENT_SECRET=your-github-client-secret
   ```

#### Microsoft OAuth2 Setup

1. Go to [Azure App Registrations](https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps)
2. Click "New registration"
3. Fill in the details:
   - Name: `XCString Editor`
   - Supported account types: Choose based on your needs
   - Redirect URI: `Web` → `https://your-domain.com/api/auth/oauth/microsoft/callback`
4. Go to "Certificates & secrets" → "New client secret"
5. Set the environment variables:
   ```bash
   export OAUTH2_MICROSOFT_ENABLED=true
   export OAUTH2_MICROSOFT_CLIENT_ID=your-microsoft-client-id
   export OAUTH2_MICROSOFT_CLIENT_SECRET=your-microsoft-client-secret
   export OAUTH2_MICROSOFT_TENANT=common    # or specific tenant ID
   ```

#### GitLab OAuth2 Setup

1. Go to [GitLab Applications](https://gitlab.com/-/profile/applications) (or your GitLab instance)
2. Click "Add new application"
3. Fill in the details:
   - Name: `XCString Editor`
   - Redirect URI: `https://your-domain.com/api/auth/oauth/gitlab/callback`
   - Scopes: `read_user`
4. Set the environment variables:
   ```bash
   export OAUTH2_GITLAB_ENABLED=true
   export OAUTH2_GITLAB_CLIENT_ID=your-gitlab-application-id
   export OAUTH2_GITLAB_CLIENT_SECRET=your-gitlab-secret
   export OAUTH2_GITLAB_INSTANCE_URL=https://gitlab.com    # or your GitLab instance URL
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
   - Check database configuration via environment variables
   - Ensure database server is running (MySQL/PostgreSQL)
   - Verify file permissions for SQLite data directory

2. **File upload fails**
   - Check Spring Boot multipart limits (`spring.servlet.multipart.max-file-size`)
   - Verify file permissions on data directory

3. **Session issues**
   - Check session cookie configuration via environment variables
   - Verify `APP_BASE_URL` matches your actual domain

4. **Permission denied errors**
   - Ensure the application has write permissions to the data directory
   - Check file ownership and permissions

5. **OAuth2 login fails**
   - Verify OAuth2 provider configuration via environment variables
   - Check redirect URIs match exactly (including https/http)
   - Ensure `OAUTH2_ENABLED=true` and the specific provider is enabled
   - Check OAuth2 provider application settings

6. **OAuth2 callback errors**
   - Verify `APP_BASE_URL` matches your domain
   - Check that OAuth2 provider allows your redirect URI
   - Ensure callback URLs use the correct protocol (https in production)

### Logs

- Application logs: Check Spring Boot console output or configured log destination
- Database errors: Set `APP_DEBUG=debug` to enable verbose Hibernate SQL logging
- Browser console: Check for JavaScript errors
