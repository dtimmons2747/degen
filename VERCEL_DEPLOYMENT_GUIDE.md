# Degen Frontend - Vercel Deployment Guide

## Status: Ready for Deployment ✅

The Degen Golf Tournament Management frontend has been fully configured for production deployment to Vercel with environment-based API configuration.

## Environment Configuration

### Local Development

- **File**: `frontend/src/environments/environment.ts`
- **API URL**: `http://localhost:8080`
- **Usage**: `npm start` (default development configuration)

### Production Deployment

- **File**: `frontend/src/environments/environment.prod.ts`
- **API URL**: `https://degen-smjo.onrender.com`
- **Build**: `npm run build -- --configuration production`

## Implementation Details

### Files Modified

#### 1. Environment Files Created

- `frontend/src/environments/environment.ts` - Local development config
- `frontend/src/environments/environment.prod.ts` - Production config with Render backend URL

#### 2. Angular Configuration

- Updated `frontend/angular.json` - Added fileReplacements for production build
- When building with `--configuration production`, Angular automatically swaps environment.ts → environment.prod.ts

#### 3. Components Updated (All localhost:8080 URLs Replaced)

- `add-course.component.ts`
- `add-players.component.ts`
- `handicap-calculator.component.ts`
- `add-tee-times.component.ts`
- `create-tournament.component.ts`
- `enter-scorecard.component.ts`
- `leaderboard.component.ts`

**Pattern Used**: All API calls now use template literals:

```typescript
`${environment.apiUrl}/api/endpoint`;
```

Instead of hardcoded:

```typescript
"http://localhost:8080/api/endpoint"; // ❌ Old way
```

## Vercel Deployment Steps

### 1. Connect Repository

```bash
# In Vercel dashboard:
# Import from Git repository (GitHub, GitLab, or Bitbucket)
# Select the Degen project folder
```

### 2. Build Configuration

- **Framework**: Angular
- **Build Command**: `cd frontend && npm run build -- --configuration production`
- **Output Directory**: `frontend/dist/frontend/browser`
- **Install Command**: `npm install`

### 3. Environment Variables

No environment variables needed in Vercel. The API URL is baked into the production build via `environment.prod.ts`.

### 4. Deploy

- Vercel will automatically:
  1. Install dependencies
  2. Run the build command with production configuration
  3. Use environment.prod.ts with `https://degen-smjo.onrender.com`
  4. Deploy the optimized dist folder

## Verification

### Local Testing

```bash
cd frontend
npm start
# Opens http://localhost:4200 using environment.ts (localhost:8080)
```

### Production Build Verification

```bash
cd frontend
npm run build -- --configuration production
# Outputs to frontend/dist/frontend/browser
# Uses environment.prod.ts (https://degen-smjo.onrender.com)
```

### Check Build Size

```
- main.312d965f18eac47a.js: 446.57 kB (94.82 kB gzipped)
- polyfills.06835b4915bc64c7.js: 34.80 kB (11.38 kB gzipped)
- styles.4b604a8a1db5c392.css: 614 bytes (274 bytes gzipped)
- Total: ~482.88 kB unminified (~107 kB gzipped)
```

## Backend Integration

### Render Backend

- **URL**: https://degen-smjo.onrender.com
- **Status**: ✅ Live and responding
- **Database**: MySQL on Filess.io

### Verification

The frontend will automatically use the Render backend for all API calls in production.

## Key Features Verified

✅ Team randomization with pair/triple prevention constraints  
✅ Handicap cascade updates with score-checking  
✅ 8-color team visualization in suggested teams and tee times table  
✅ Player selection interface (full-time pre-checked, part-time unchecked)  
✅ Course handicap calculation  
✅ Leaderboard with multi-view support  
✅ Round and tournament management  
✅ Score entry interface

## API Endpoints in Use

All endpoints now point to: `https://degen-smjo.onrender.com/api/`

- `GET /tournaments`
- `GET /tournament-rounds`
- `GET /round-tee-times`
- `GET /round-tee-times/player-selection/{tournamentRoundId}`
- `POST /round-tee-times/randomize-selection/{tournamentRoundId}`
- `POST /round-tee-times`
- `PUT /round-tee-times/{id}`
- `DELETE /round-tee-times/{id}`
- `GET /players`
- `POST /players`
- `GET /tournament-handicaps`
- `POST /tournament-handicaps`
- `PUT /tournament-handicaps/{id}`
- `DELETE /tournament-handicaps/{id}`
- `GET /courses`
- `POST /courses`
- `GET /games`
- `GET /scoring-types`
- `GET /handicap/player/{playerId}`
- And more...

## Next Steps

1. Connect Degen repository to Vercel
2. Configure build settings as shown above
3. Deploy (Vercel will handle everything automatically)
4. Test production deployment at provided Vercel URL
5. Monitor for any API connectivity issues

## Troubleshooting

If frontend shows errors connecting to API:

1. Verify Render backend is online: https://degen-smjo.onrender.com/api/games
2. Check CORS configuration on Render backend
3. Verify browser console for network errors
4. Ensure environment.prod.ts has correct URL

## Notes

- The production build includes all necessary files in `frontend/dist/frontend/browser/`
- Vercel will serve static files with caching headers
- All Angular change detection is configured for optimal performance
- Source maps are excluded from production build by default
