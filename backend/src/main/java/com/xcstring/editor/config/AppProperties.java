package com.xcstring.editor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "xcstring")
@Data
public class AppProperties {
    private DatabaseProps database = new DatabaseProps();
    private RegistrationProps registration = new RegistrationProps();
    private SessionProps session = new SessionProps();
    private FilesProps files = new FilesProps();
    private OAuth2Props oauth2 = new OAuth2Props();
    private AiProps ai = new AiProps();
    private AppProps app = new AppProps();

    @Data
    public static class DatabaseProps {
        private String driver = "sqlite";
        private String host = "localhost";
        private int port = 3306;
        private String name = "xcstring_editor";
        private String username = "";
        private String password = "";
        private String sqlitePath = "./data/database.sqlite";
    }

    @Data
    public static class RegistrationProps {
        private boolean enabled = true;
        private List<String> allowedDomains = new ArrayList<>();
        private List<String> inviteDomains = new ArrayList<>();
    }

    @Data
    public static class SessionProps {
        private int lifetime = 604800;
        private String cookieName = "xcstring_session";
        private boolean cookieSecure = false;
        private boolean cookieHttpOnly = true;
    }

    @Data
    public static class FilesProps {
        private long maxFileSize = 10 * 1024 * 1024;
        private int maxFilesPerUser = 100;
    }

    @Data
    public static class OAuth2Props {
        private boolean enabled = false;
        private ProviderProps google = new ProviderProps();
        private ProviderProps github = new ProviderProps();
        private MicrosoftProviderProps microsoft = new MicrosoftProviderProps();
        private GitLabProviderProps gitlab = new GitLabProviderProps();
        private Map<String, CustomProviderProps> customProviders = new HashMap<>();
    }

    @Data
    public static class ProviderProps {
        private boolean enabled = false;
        private String clientId = "";
        private String clientSecret = "";
    }

    @Data
    public static class MicrosoftProviderProps extends ProviderProps {
        private String tenant = "common";
    }

    @Data
    public static class GitLabProviderProps extends ProviderProps {
        private String instanceUrl = "https://gitlab.com";
    }

    @Data
    public static class CustomProviderProps {
        private boolean enabled = false;
        private String displayName;
        private String clientId;
        private String clientSecret;
        private String authorizeUrl;
        private String tokenUrl;
        private String userInfoUrl;
        private String scope = "openid email profile";
        private String userIdField = "sub";
        private String userNameField = "name";
        private String userEmailField = "email";
        private String userAvatarField;
        private String iconSvg;
        private Map<String, String> additionalParams = new HashMap<>();
        private boolean allowRegistration = true;
    }

    @Data
    public static class AiProps {
        private boolean enabled = false;
        private String defaultProvider = "openai";
        private String defaultModel = "gpt-4o-mini";
        private ProviderAiConfig openai = new ProviderAiConfig();
        private ProviderAiConfig anthropic = new ProviderAiConfig();
        private ProviderAiConfig openaiCompatible = new ProviderAiConfig();
        private ProviderAiConfig zai = new ProviderAiConfig();
        private ProviderAiConfig deepl = new ProviderAiConfig();
    }

    @Data
    public static class ProviderAiConfig {
        private boolean enabled = false;
        private String apiKey = "";
        private String baseUrl = "";
        private List<String> models = new ArrayList<>();
    }

    @Data
    public static class AppProps {
        private String name = "XCString Editor";
        private boolean debug = false;
        private String baseUrl = "http://localhost:8080";
    }
}
