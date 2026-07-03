# Contexte de build : la racine du dépôt (workspace pnpm).
FROM node:24-alpine AS build
RUN corepack enable
WORKDIR /app
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY frontend/package.json frontend/package.json
RUN pnpm install --frozen-lockfile --ignore-scripts
COPY frontend frontend
# La cible du proxy /api est résolue au build (routeRules Nitro)
ARG NUXT_API_PROXY_TARGET=http://backend:8080
ENV NUXT_API_PROXY_TARGET=${NUXT_API_PROXY_TARGET}
RUN pnpm --dir frontend build

FROM node:24-alpine
RUN addgroup -S belay && adduser -S belay -G belay
USER belay
WORKDIR /app
COPY --from=build /app/frontend/.output .output
EXPOSE 3000
ENV NITRO_HOST=0.0.0.0 NITRO_PORT=3000
CMD ["node", ".output/server/index.mjs"]
