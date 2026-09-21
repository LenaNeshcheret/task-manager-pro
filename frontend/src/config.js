function trimTrailingSlash(value) {
  return value.endsWith('/') ? value.slice(0, -1) : value;
}

const rawApiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api';

export const apiBaseUrl = trimTrailingSlash(rawApiBaseUrl);

export const keycloakConfig = {
  url: trimTrailingSlash(import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8081'),
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'task-manager',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'task-manager-api',
};

export function buildApiUrl(pathname) {
  const normalizedPath = pathname.startsWith('/') ? pathname : `/${pathname}`;
  return `${apiBaseUrl}${normalizedPath}`;
}
