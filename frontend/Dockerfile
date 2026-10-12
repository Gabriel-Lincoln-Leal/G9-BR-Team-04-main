# Compilar o Angular
FROM node:24-bookworm-slim AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build-prod

# Servir a interface com Nginx
FROM nginx:stable-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist/techmind-frontend/browser/ /usr/share/nginx/html/

EXPOSE 80