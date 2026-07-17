# Deployment-Guide

## 1. LWE-Server (Cloud-Hosting)

Empfohlen: Hetzner CX22 (~5€/Monat) oder Fly.io

### Voraussetzungen
- Ubuntu 24.04
- Podman installiert (`apt install podman podman-compose`)
- PostgreSQL 18 (lokal oder via Dienstleister)
- Domain mit DNS-Eintrag (z.B. `lwe.dein-server.de`)

### Einrichtung

```bash
git clone https://github.com/CenkSaatci/livingWorldEngine.git
cd livingWorldEngine

# .env erstellen
cat > .env << EOF
DB_HOST=localhost
DB_PORT=5432
DB_NAME=lwe
DB_USER=lwe
DB_PASSWORD=$(openssl rand -base64 32)
JWT_SECRET=$(openssl rand -base64 48)
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=https://lwe.dein-server.de
SPRING_PROFILES_ACTIVE=prod
EOF

# PostgreSQL starten
podman run -d --name lwe-db \
  -e POSTGRES_DB=lwe \
  -e POSTGRES_USER=lwe \
  -e POSTGRES_PASSWORD=$DB_PASSWORD \
  -v lwe-db-data:/var/lib/postgresql/data \
  -p 5432:5432 \
  docker.io/postgres:18

# App starten
podman-compose -f compose.prod.yml up -d

# SSL via Nginx Proxy Manager oder Caddy
```

### Nginx-Reverse-Proxy

```nginx
server {
    listen 443 ssl;
    server_name lwe.dein-server.de;

    ssl_certificate /etc/letsencrypt/live/.../fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/.../privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

---

## 2. AI-Bot (Lokal bei dir)

```bash
# Auf deinem Rechner mit GDX Spark
podman-compose -f compose.bot.yml up -d
```

Siehe `compose.bot.yml` und `compose.bot.env.example`.

---

## 3. Bot-User anlegen

```bash
# 1. Registriere einen Bot-User
curl -X POST https://lwe.dein-server.de/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"bot@lwe","username":"ai-bot","password":"geheim123"}'

# 2. Setze Rolle auf BOT (nur ADMIN)
curl -X PUT https://lwe.dein-server.de/api/v1/admin/users/{userId}/role \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{"role":"BOT"}'

# 3. Service-Login für API-Key
curl -X POST https://lwe.dein-server.de/api/v1/auth/service-login \
  -H "Content-Type: application/json" \
  -d '{"serviceUser":"ai-bot","servicePassword":"geheim123"}'
# → accessToken = LWE_API_KEY
```

---

## 4. Health-Check

```bash
# Zeigt DB + Redis + Bot Status
curl https://lwe.dein-server.de/actuator/health

# Antwort:
# {
#   "status": "UP",
#   "components": {
#     "db":       {"status": "UP"},
#     "redis":    {"status": "UP"},
#     "bot":      {"status": "UP"}
#   }
# }
```

---

## 5. Nützliche Kommandos

```bash
# Logs
podman logs -f lwe-backend
podman logs -f lwe-ai-bot

# Backup
pg_dump -h localhost -U lwe lwe > backup.sql

# Restore
psql -h localhost -U lwe lwe < backup.sql

# Migration-Status
mvn flyway:info

# Uploads sichern
tar czf uploads.tar.gz /pfad/zu/uploads/
```
