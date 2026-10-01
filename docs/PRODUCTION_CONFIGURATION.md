# Production Configuration

## Android backend endpoint

The Android app never embeds an AI provider credential. The backend endpoint is supplied at build time:

- `-PBACKEND_BASE_URL=https://<deployed-backend-host>`
- or the `BACKEND_BASE_URL` environment variable

Debug builds may use the repository placeholder endpoint when no deployment endpoint is supplied. Release builds reject that placeholder and require HTTPS.

Do not put API keys, bearer tokens, or provider credentials in Gradle files, Android resources, source code, or the APK.

## Backend AI configuration

The backend requires `AI_API_KEY` and `AI_MODEL` when AI chat is enabled. `AI_BASE_URL` is optional.

The backend returns a configuration error when required AI configuration is absent.

## Privacy and permissions

Camera access is requested only when the user starts the camera workflow. The app does not request broad storage permissions.

Cleartext HTTP is disabled. Android backup is disabled so the application database is not silently copied through platform backup.

## Database migration

Room no longer uses destructive migration fallback. Version 2 to version 3 has an explicit migration boundary that preserves the existing catalogue. Future user-owned data must receive explicit migrations before schema changes ship.

## Deployment verification

1. Provide a real HTTPS BACKEND_BASE_URL.
2. Provide AI_API_KEY and AI_MODEL to the backend deployment environment.
3. Verify /health reports the expected 424-recipe catalogue count.
4. Verify AI chat against the deployed provider.
5. Verify URL import and SSRF protections.
6. Run the complete Android and backend CI suite.
7. Do not distribute an APK built with the placeholder backend endpoint.