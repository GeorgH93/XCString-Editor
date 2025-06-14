FROM php:8.3-apache

# Enable Apache rewrite module
RUN a2enmod rewrite

# Set working directory
WORKDIR /var/www/html

# Copy application files
COPY public/ /var/www/html/
COPY backend/ /var/www/html/backend/

# Configure Apache to serve from current directory
RUN echo '<Directory /var/www/html>\n\
    Options Indexes FollowSymLinks\n\
    AllowOverride All\n\
    Require all granted\n\
</Directory>' > /etc/apache2/conf-available/xcstring-editor.conf

RUN a2enconf xcstring-editor

# Expose port 80
EXPOSE 80

# Start Apache
CMD ["apache2-foreground"]