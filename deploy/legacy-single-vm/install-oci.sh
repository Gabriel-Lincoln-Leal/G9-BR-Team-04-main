#!/usr/bin/env bash
set -euo pipefail
test "$(id -u)" -eq 0
test -f /home/ubuntu/techmind-oci.zip
command -v unzip >/dev/null || { apt-get update; apt-get install -y unzip; }
release="/opt/techmind/releases/$(date -u +%Y%m%dT%H%M%SZ)"
install -d "$release" /opt/techmind/python /opt/techmind/logs
unzip -q /home/ubuntu/techmind-oci.zip -d "$release"
test -f "$release/backend/techmind.jar"
test -f "$release/frontend/index.html"
test -f "$release/python/models/modelo.pkl"
id techmind >/dev/null 2>&1 || adduser --system --group --home /opt/techmind techmind
install -o techmind -g techmind -m 640 "$release/backend/techmind.jar" /opt/techmind/techmind.jar
cp -a "$release/python/." /opt/techmind/python/
chown -R techmind:techmind /opt/techmind/python /opt/techmind/logs
install -d -m 700 /etc/techmind
if ! test -f /etc/techmind/techmind.env; then
    umask 077
    printf 'TECHMIND_API_KEY=%s\nOPENBLAS_NUM_THREADS=1\nOMP_NUM_THREADS=1\nMKL_NUM_THREADS=1\n' "$(openssl rand -hex 32)" > /etc/techmind/techmind.env
fi
chmod 600 /etc/techmind/techmind.env
sudo -u techmind python3.12 -m venv /opt/techmind/python/.venv
sudo -u techmind /opt/techmind/python/.venv/bin/python -m pip install --no-cache-dir --only-binary=:all: -r /opt/techmind/python/requirements.txt
cat > /etc/systemd/system/techmind-python.service <<'EOF'
[Unit]
Description=TechMind Python classifier
Wants=network-online.target
After=network-online.target
[Service]
User=techmind
Group=techmind
WorkingDirectory=/opt/techmind/python
EnvironmentFile=/etc/techmind/techmind.env
ExecStart=/opt/techmind/python/.venv/bin/python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --workers 1 --limit-concurrency 4
Restart=on-failure
RestartSec=10
NoNewPrivileges=true
PrivateTmp=true
ProtectHome=true
ProtectSystem=full
[Install]
WantedBy=multi-user.target
EOF
cat > /etc/systemd/system/techmind-backend.service <<'EOF'
[Unit]
Description=TechMind Java backend
Wants=network-online.target techmind-python.service
After=network-online.target techmind-python.service
[Service]
User=techmind
Group=techmind
WorkingDirectory=/opt/techmind
EnvironmentFile=/etc/techmind/techmind.env
ExecStart=/usr/bin/java -Xms64m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=32m -XX:+ExitOnOutOfMemoryError -jar /opt/techmind/techmind.jar
Restart=on-failure
RestartSec=15
NoNewPrivileges=true
PrivateTmp=true
ProtectHome=true
ProtectSystem=full
[Install]
WantedBy=multi-user.target
EOF
cat > /etc/nginx/sites-available/techmind <<EOF
server {
    listen 127.0.0.1:80;
    server_name _;
    root $release/frontend;
    index index.html;
    client_max_body_size 2m;
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
chmod 755 /opt/techmind /opt/techmind/releases "$release" "$release/frontend"
find "$release/frontend" -type d -exec chmod 755 {} \;
find "$release/frontend" -type f -exec chmod 644 {} \;
ln -sfn /etc/nginx/sites-available/techmind /etc/nginx/sites-enabled/techmind
if test -L /etc/nginx/sites-enabled/default; then unlink /etc/nginx/sites-enabled/default; fi
nginx -t
systemctl daemon-reload
systemctl enable techmind-python nginx
systemctl restart techmind-python
systemctl restart nginx
echo 'Python installed; backend awaits database configuration; frontend available via SSH tunnel.'
