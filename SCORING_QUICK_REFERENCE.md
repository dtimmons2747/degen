# Degen Scoring System - Quick Reference

## Nines Scoring Formula (9 points per hole per team)

### Tie Scenarios:

```
Scenario 1: NO TIES
  1st: 5 pts  |  2nd: 3 pts  |  3rd: 1 pt  = 9 total

Scenario 2: TIE FOR 1ST (2 players)
  1st: 4 pts  |  1st: 4 pts  |  3rd: 1 pt  = 9 total

Scenario 3: ALL TIED
  1st: 3 pts  |  1st: 3 pts  |  1st: 3 pts  = 9 total

Scenario 4: TIE FOR 2ND (1st unique, 2nd & 3rd tied)
  1st: 5 pts  |  2nd: 2 pts  |  2nd: 2 pts  = 9 total
```

## Database Schema (Key Relationships)

```
ScoringType (1:N) ← Game
     ↑
     │
     └─ TournamentRound
          ├─ RoundTeam (3 players)
          │  └─ TeamHoleScore (netScore, gamePoints per hole)
          │
          └─ RoundTeeTime
             └─ PlayerScorecard (grossScore, netScore, gamePoints per player)
```

## Score Calculation Pipeline

```
1. ENTER GROSS SCORE
   ↓
2. CALCULATE NET SCORE
   NetScore = GrossScore - (CourseHandicap ÷ 18) - bonus strokes for hard holes
   ↓
3. CALCULATE TEAM NET SCORE (for Nines)
   TeamNetScore = MIN(Player1Net, Player2Net, Player3Net)
   ↓
4. ASSIGN PLAYER POINTS (within team)
   Sort 3 players by NetScore → Assign 5/3/1 or 4/4/1, etc.
   ↓
5. ASSIGN TEAM POINTS (across all teams)
   Sort all teams by TeamNetScore → Assign 5/3/1 or 4/4/1, etc.
   ↓
6. DISPLAY IN SCORECARD
   [Gross Score]
   [Net Score]
   [Game Points] ← This is 5, 3, 1, 4, 2, or 3
```

## Frontend Display (Nines Example)

### Per-Hole Cell:

```
Hole 1 (Par 4)
┌────────────┐
│    4       │ ← Gross score (input)
│    4       │ ← Net score (calculated)
│    5       │ ← Game points (1st/best player)
└────────────┘
```

### Totals Row:

```
OUT Total
┌────────────────────┐
│  37 | 36 | 14      │
│  ↑    ↑    ↑        │
│  │    │    └─ Game Points Total
│  │    └─ Net Total
│  └─ Gross Total
└────────────────────┘
```

## Nines Scoring Logic in Code

### Backend (calculatePlayerGamePoints):

```
Input: List<PlayerScorecard> sorted by NetScore (ascending)
Output: List<Integer> points (one per player)

if (all 3 scores equal)           → [3, 3, 3]
if (score[0] == score[1])         → [4, 4, 1]
if (score[1] == score[2])         → [5, 2, 2]
else (no ties)                    → [5, 3, 1]
```

### Frontend (calculateGamePointsForHole):

```
1. Get all 3 players' NetScores for this hole
2. Sort ascending (best first)
3. Identify tie patterns
4. Return appropriate points (5, 4, 3, 2, or 1)
```

## Points Totaling

### 9 Holes:

```
Hole 1: 5 pts
Hole 2: 3 pts
Hole 3: 1 pt
Hole 4: 4 pts
Hole 5: 4 pts
Hole 6: 1 pt
Hole 7: 5 pts
Hole 8: 3 pts
Hole 9: 2 pts
────────────
Total: 28 pts (out of 81 possible per 9 holes)
```

### Full Round:

```
Front 9: 28 pts
Back 9:  26 pts
────────────────
Total:   54 pts (out of 162 possible per 18 holes)
```

## Data Storage Details

### Column: `gamePoints`

**PlayerScorecard.gamePoints:**

- Stores: Integer (5, 3, 1, 4, 2, or 3)
- Per: Individual player, per hole
- Set by: Backend service after score entry

**TeamHoleScore.gamePoints:**

- Stores: Integer (5, 3, 1, 4, 2, or 3)
- Per: Team, per hole
- Set by: calculateTeamPointsForHole()

**SplitSkins points:**

- Stores: Integer (as cents: 50 = $0.50, 33 = $0.33)
- Display: Divided by 100 and shown as decimal

## Frontend Signal/Computed Pattern

```typescript
// Signals
playerScores = signal<PlayerScore[]>([])
selectedRound = computed(() => rounds().find(...))

// In PlayerScore interface
scores: { [holeId: number]: { gamePoints?: number } }

// Computing totals
getPlayerNineGamePoints(playerId, startHole, endHole): number {
  return holes().reduce((total, hole) =>
    total + (playerScore.scores[hole.id]?.gamePoints || 0), 0)
}
```

## Conditional Display Rules

```typescript
Show POINTS column if:
  ✓ getGameType() === "Nines"
  ✓ getGameType() === "Stableford"
  ✓ getScoringType() === "Split Skins"

Hide POINTS column if:
  ✗ Stroke (individual)
  ✗ 2-Man Aggregate (group score competition)
```

## Key Backend Files

| File                        | Purpose                                  | Key Method                     |
| --------------------------- | ---------------------------------------- | ------------------------------ |
| GamePointsMigration.java    | Migration script showing Nines algorithm | calculateGamePointsForHole()   |
| TeamGamePointsService.java  | Calculates team & player points          | calculatePlayerPointsForHole() |
| PlayerScorecardService.java | Saves scores, calculates net             | calculateNetScore()            |

## Key Frontend Files

| File                           | Purpose                   | Key Method                   |
| ------------------------------ | ------------------------- | ---------------------------- |
| enter-scorecard.component.ts   | Main logic & calculations | calculateGamePointsForHole() |
| enter-scorecard.component.html | Display template          | Conditional @if blocks       |
| enter-scorecard.component.scss | Styling                   | .game-points class           |

## Important Notes

1. **Nines requires exactly 3 players** - doesn't process with 2 or 4 players
2. **Points are calculated per hole** - not cumulative
3. **Net score depends on handicap** - same gross score can be different net based on hole handicap
4. **Frontend mirrors backend logic** - calculates game points in real-time during score entry
5. **Team net score is BEST of players** - not average or sum
6. **Player game points are within team** - player 1 competes against players 2 & 3
7. **Storage uses cents for decimals** - Split Skins stores as 50 (meaning $0.50)
