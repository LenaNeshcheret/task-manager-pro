import { useEffect, useMemo, useState } from 'react';
import keycloak from './keycloak';
import { apiBaseUrl, buildApiUrl, keycloakConfig } from './config';
import './styles.css';

const initialState = {
  initializing: true,
  authenticated: false,
  me: null,
  error: '',
};

function formatError(error) {
  if (!error) {
    return 'Unknown error';
  }

  if (typeof error === 'string') {
    return error;
  }

  return error.message || 'Unknown error';
}

export default function App() {
  const [appState, setAppState] = useState(initialState);

  useEffect(() => {
    let mounted = true;

    async function bootstrap() {
      try {
        const authenticated = await keycloak.init({
          onLoad: 'check-sso',
          pkceMethod: 'S256',
          checkLoginIframe: false,
          silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        });

        if (!mounted) {
          return;
        }

        if (!authenticated) {
          setAppState({
            initializing: false,
            authenticated: false,
            me: null,
            error: '',
          });
          return;
        }

        keycloak.onTokenExpired = () => {
          keycloak.updateToken(30).catch(() => {
            keycloak.login({ redirectUri: window.location.origin });
          });
        };

        const me = await loadMe();
        if (!mounted) {
          return;
        }

        setAppState({
          initializing: false,
          authenticated: true,
          me,
          error: '',
        });
      } catch (error) {
        if (!mounted) {
          return;
        }

        setAppState({
          initializing: false,
          authenticated: false,
          me: null,
          error: formatError(error),
        });
      }
    }

    bootstrap();

    return () => {
      mounted = false;
    };
  }, []);

  const username = useMemo(() => {
    return keycloak.tokenParsed?.preferred_username || appState.me?.email || 'anonymous';
  }, [appState.me]);

  async function handleLogin() {
    await keycloak.login({
      redirectUri: window.location.origin,
    });
  }

  async function handleLogout() {
    await keycloak.logout({
      redirectUri: window.location.origin,
    });
  }

  async function handleReloadProfile() {
    try {
      setAppState(current => ({
        ...current,
        error: '',
      }));

      const me = await loadMe();
      setAppState(current => ({
        ...current,
        authenticated: true,
        me,
      }));
    } catch (error) {
      setAppState(current => ({
        ...current,
        error: formatError(error),
      }));
    }
  }

  return (
    <main className="shell">
      <section className="hero">
        <p className="eyebrow">Task Manager UI</p>
        <h1>Keycloak login and backend identity in one screen.</h1>
        <p className="lede">
          This Vite client signs in with Keycloak, calls <code>GET /api/v1/me</code>,
          and renders the active user plus granted roles.
        </p>
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <p className="panel-label">Session</p>
            <h2>{appState.initializing ? 'Initializing session' : appState.authenticated ? username : 'Signed out'}</h2>
          </div>
          <div className="actions">
            {appState.authenticated ? (
              <>
                <button className="ghost" onClick={handleReloadProfile} type="button">
                  Reload profile
                </button>
                <button onClick={handleLogout} type="button">
                  Logout
                </button>
              </>
            ) : (
              <button onClick={handleLogin} type="button">
                Login with Keycloak
              </button>
            )}
          </div>
        </div>

        <div className="grid">
          <article className="card accent">
            <p className="card-label">Backend status</p>
            <h3>
              {appState.initializing
                ? 'Waiting for session check'
                : appState.authenticated && appState.me
                  ? 'Connected'
                  : 'Not connected'}
            </h3>
            <p className="card-copy">
              API base URL: <code>{apiBaseUrl}</code>
            </p>
            {appState.me ? (
              <p className="card-copy">
                Backend identity: <strong>{appState.me.email}</strong>
              </p>
            ) : null}
          </article>

          <article className="card">
            <p className="card-label">Roles</p>
            <div className="chips">
              {appState.me?.roles?.length ? (
                appState.me.roles.map(role => (
                  <span className="chip" key={role}>
                    {role}
                  </span>
                ))
              ) : (
                <span className="muted">No roles loaded yet.</span>
              )}
            </div>
          </article>

          <article className="card">
            <p className="card-label">OIDC config</p>
            <dl className="meta">
              <div>
                <dt>Keycloak URL</dt>
                <dd>{keycloakConfig.url}</dd>
              </div>
              <div>
                <dt>Realm</dt>
                <dd>{keycloakConfig.realm}</dd>
              </div>
              <div>
                <dt>Client ID</dt>
                <dd>{keycloakConfig.clientId}</dd>
              </div>
            </dl>
          </article>
        </div>

        {appState.error ? (
          <div className="error-box" role="alert">
            <strong>Request failed.</strong>
            <span>{appState.error}</span>
          </div>
        ) : null}
      </section>
    </main>
  );
}

async function loadMe() {
  if (!keycloak.authenticated) {
    throw new Error('User is not authenticated.');
  }

  await keycloak.updateToken(30);

  const response = await fetch(buildApiUrl('/v1/me'), {
    headers: {
      Authorization: `Bearer ${keycloak.token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`GET /api/v1/me failed with status ${response.status}`);
  }

  return response.json();
}
