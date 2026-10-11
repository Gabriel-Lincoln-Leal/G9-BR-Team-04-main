#!/usr/bin/env bash
set -euo pipefail
host=techmind.140-238-236-255.sslip.io
root=$(awk '/^[[:space:]]*root / {gsub(/;/,"",$2); print $2; exit}' /etc/nginx/sites-available/techmind)
test -f "$root/index.html"
install -d -m 755 /var/www/letsencrypt/.well-known/acme-challenge
cp /etc/nginx/sites-available/techmind /etc/techmind/nginx-before-https.conf
cat > /etc/nginx/sites-available/techmind <<EOF
server {
    listen 80 default_server;
    server_name $host;
    location ^~ /.well-known/acme-challenge/ { root /var/www/letsencrypt; }
    location / { return 301 https://$host\$request_uri; }
}
EOF
nginx -t
systemctl restart nginx
iptables -C INPUT -p tcp -m multiport --dports 80,443 -j ACCEPT 2>/dev/null || iptables -I INPUT 1 -p tcp -m multiport --dports 80,443 -j ACCEPT
install -d /etc/iptables
iptables-save > /etc/iptables/rules.v4
certbot certonly --webroot -w /var/www/letsencrypt -d "$host" --non-interactive --agree-tos --register-unsafely-without-email
cat >> /etc/nginx/sites-available/techmind <<EOF
server {
    listen 443 ssl;
    server_name $host;
    ssl_certificate /etc/letsencrypt/live/$host/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$host/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    root $root;
    index index.html;
    client_max_body_size 2m;
    add_header X-Content-Type-Options nosniff always;
    add_header Referrer-Policy strict-origin-when-cross-origin always;
    location ~ ^/(conteudo|users)(/|\$) {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 120s;
    }
    location / { try_files \$uri \$uri/ /index.html; }
}
EOF
nginx -t
systemctl reload nginx
install -d /etc/letsencrypt/renewal-hooks/deploy
printf '#!/bin/sh\nsystemctl reload nginx\n' > /etc/letsencrypt/renewal-hooks/deploy/techmind-nginx
chmod 755 /etc/letsencrypt/renewal-hooks/deploy/techmind-nginx
systemctl enable --now certbot.timer
echo "HTTPS available: https://$host"
