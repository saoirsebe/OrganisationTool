FROM node:20-slim
LABEL authors="saoir"
WORKDIR /app/backend

COPY backend/package.json ./
RUN npm ci --omit=dev

WORKDIR /app
COPY . .

WORKDIR /app/backend
EXPOSE 3000
USER node
CMD ["node", "src/server.js"]