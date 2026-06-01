Your text replies for discussions should be short.
While the project has a Docker Compose, its just for development. Otherwise I host on Railway.com
All the code between frontend and backend dealing with data should correspond with DTO data types called ...Response or ...Request.


After implementing code, always end with a suggested commit message

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

If in read-only mode, it means I will be asking for either planning, explanations, or for code suggestions.
For planning, you may roughly keep in mind the project goal: Parivant should be a tool that helps users make better 
informed decisions when betting on Stryktipset, Topptipset or Europatipset. I already gather predictions about matches from the web,
a model that spits out suggested selections for any round of matches, and I intend on aggregating important news relevant to figuring out which team is more likely to win.
In addition, I show live scores currently.