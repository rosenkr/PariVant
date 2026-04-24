This is a monorepo with:
- `frontend/`: React + Vite + TypeScript + MUI frontend.
- `backend/`: Java Spring Boot backend module.
- `domain/`: Java domain module containing POJOs and core domain types.

## Running the application

The application is usually run in one of two ways:

### Production / hosted deployment

- The app is deployed on Railway with a domain at parivant.se using Cloudflare DNS to connect to Railway.
- Do not change Railway-specific configuration unless the task explicitly involves deployment or production configuration.
- If a change may affect Railway deployment, mention it in the final summary.

### Local development

For localhost testing:

1. From the repository root, start the backend/supporting services with:

   ```bash
   docker compose up
   ```
2. In a separate terminal, start the frontend dev server: 
```npm run dev```

If in read-only mode, it means I will be asking for either planning or for code suggestions.