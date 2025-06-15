FROM php:8.3-apache

# Enable Apache rewrite module
RUN a2enmod rewrite

# Set working directory
WORKDIR /var/www/html

# Copy application files
COPY public/ /var/www/html/
COPY backend/ /var/www/html/backend/
COPY config.php /var/www/html/
COPY init-db.php /var/www/html/

# Configure Apache to serve from current directory
RUN echo '<Directory /var/www/html>\n\
    Options Indexes FollowSymLinks\n\
    AllowOverride All\n\
    Require all granted\n\
</Directory>' > /etc/apache2/conf-available/xcstring-editor.conf

RUN a2enconf xcstring-editor

# Create startup script
RUN echo '#!/bin/bash\n\
# Create data directory for SQLite\n\
mkdir -p /var/www/html/data\n\
chown -R www-data:www-data /var/www/html/data\n\
\n\
# Initialize database\n\
echo "Checking database initialization..."\n\
php /var/www/html/init-db.php\n\
\n\
# Ensure proper permissions\n\
chown -R www-data:www-data /var/www/html/data\n\
\n\
# Start Apache\n\
exec apache2-foreground' > /usr/local/bin/start-xcstring-editor.sh

RUN chmod +x /usr/local/bin/start-xcstring-editor.sh

# Expose port 80
EXPOSE 80

# Start with our custom script
CMD ["/usr/local/bin/start-xcstring-editor.sh"]