# Backend Deployment

The backend is packaged as a provider-neutral OCI/Docker image. No cloud provider is selected by the repository.

## Build

From the repository root:

```bash
docker build -f backend/Dockerfile -t beyond-human-kitchen-api:0.4.0 .
```

## Run locally

Provide AI provider configuration through runtime environment variables. Never put provider credentials in the image or Android application.

```bash
docker run --rm \
  -p 8000:8000 \
  -e AI_API_KEY='...' \
  -e AI_MODEL='...' \
  -e AI_BASE_URL='https://api.openai.com/v1' \
  beyond-human-kitchen-api:0.4.0
```

Verify the catalogue:

```bash
curl --fail http://127.0.0.1:8000/health
```

The response must report `"ok": true` and `"recipeCount": 424`.

## Production requirements

The selected hosting provider must:

1. Run this image with an externally reachable HTTPS endpoint.
2. Keep `AI_API_KEY`, `AI_MODEL`, and optional `AI_BASE_URL` in secret/environment configuration.
3. Provide outbound HTTPS access to the configured AI provider.
4. Provide health/readiness monitoring using `GET /health`.
5. Provide TLS termination or an equivalent HTTPS reverse proxy.
6. Keep administrative access at the hosting platform; the Android client must never receive provider credentials.

The image listens on port 8000.

## Deployment verification

After deployment, verify the real HTTPS endpoint:

```bash
curl --fail https://<deployed-host>/health
```

Then verify:

- `/health` reports 424 recipes.
- `/v1/recipes/search` returns catalogue results.
- `/v1/ai/chat` succeeds with the configured provider.
- `/v1/import/url` imports a permitted public HTTPS recipe page and returns `status: "draft"` and `requiresConfirmation: true`.
- localhost/private-address URL targets are rejected.
- The Android release is built with the real HTTPS `BACKEND_BASE_URL`.
- The Android client completes chat and URL-import flows against the deployed endpoint.
- Offline catalogue search continues to work when the backend is unavailable.

Do not mark live deployment verified until these checks have been executed against the actual deployed service and evidence recorded.
