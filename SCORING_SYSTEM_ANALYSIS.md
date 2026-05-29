# Degen Scoring System Analysis

## 1. SCORING TYPES DEFINITIONS

### Backend Entity: `ScoringType`

**Location:** [backend/src/main/java/com/degen/backend/entity/ScoringType.java](backend/src/main/java/com/degen/backend/entity/ScoringType.java#L1)

```java
@Entity
@Table(name = "scoring_type")
public class ScoringType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scoring_type_name")
    private String scoringTypeName;  // e.g., "Stroke", "Nines", "Split Skins"
}
```

### Known Scoring Types (from codebase):

- **id=1**: Stroke (basic scoring, no game points)
- **id=4**: Nines (team-based, 9 points per hole)
- **id=5**: Split Skins (individual scoring with point splitting)

### Game Entity Relationship

**Location:** [backend/src/main/java/com/degen/backend/entity/Game.java](backend/src/main/java/com/degen/backend/entity/Game.java#L1)

```java
@Entity
@Table(name = "game")
public class Game {
    @ManyToOne
    @JoinColumn(name = "scoring_type_id", referencedColumnName = "scoring_type_id", nullable = true)
    private ScoringType scoringType;

    private String name;  // e.g., "Nines", "2-Man Best Ball", "2-Man Aggregate"
}
```

---

## 2. NINES SCORING TYPE CALCULATION

### Overview

Nines is a team-based competition where:

- Teams consist of **3 players**
- **9 points distributed per hole** among team members
- Points are allocated based on each player's performance relative to teammates

### Nines Points Allocation Per Hole

**Location:** [backend/src/main/java/com/degen/backend/util/GamePointsMigration.java#L128](backend/src/main/java/com/degen/backend/util/GamePointsMigration.java#L128)

```
Points Distribution Rules (for 3 players per team):
────────────────────────────────────────────────────────

Scenario 1 - No Ties (Standard):
  1st place (best net score):  5 points
  2nd place:                   3 points
  3rd place (worst score):     1 point
  TOTAL:                       9 points

Scenario 2 - Tie for 1st Place (2 players tied):
  1st place (each tied):       4 points
  3rd place:                   1 point
  TOTAL:                       9 points

Scenario 3 - All Three Tied:
  1st place (each):            3 points
  TOTAL:                       9 points

Scenario 4 - Tie for 2nd Place (1st unique, 2nd & 3rd tied):
  1st place:                   5 points
  2nd place (each tied):       2 points
  TOTAL:                       9 points
```

### Backend Calculation Logic

**Location:** [backend/src/main/java/com/degen/backend/service/TeamGamePointsService.java#L221](backend/src/main/java/com/degen/backend/service/TeamGamePointsService.java#L221)

```java
private List<Integer> calculatePlayerGamePoints(List<PlayerScorecard> sortedByScore) {
    // sortedByScore is sorted by netScore (ascending = best first)

    Integer score1 = sortedByScore.get(0).getNetScore();
    Integer score2 = sortedByScore.get(1).getNetScore();
    Integer score3 = sortedByScore.get(2).getNetScore();

    // All tied
    if (score1.equals(score2) && score2.equals(score3)) {
        return Arrays.asList(3, 3, 3);
    }

    // Tie for first place (1st and 2nd tied, 3rd different)
    if (score1.equals(score2) && !score2.equals(score3)) {
        return Arrays.asList(4, 4, 1);
    }

    // Tie for second place (1st different, 2nd and 3rd tied)
    if (!score1.equals(score2) && score2.equals(score3)) {
        return Arrays.asList(5, 2, 2);
    }

    // Standard: no ties
    return Arrays.asList(5, 3, 1);
}
```

### Frontend Calculation (Client-side)

**Location:** [frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.ts#L406](frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.ts#L406)

The component has a `calculateGamePointsForHole()` method that mirrors backend logic for real-time display:

```typescript
private calculateGamePointsForHole(playerId: number, hole: Hole): number {
    // Gets all 3 players' net scores for the hole
    // Sorts by net score (ascending = best first)
    // Returns 5, 4, 3, 2, or 1 based on player position and ties

    // Count ties
    const firstPlaceScores = sorted.filter(s => s.netScore === sorted[0].netScore);
    const secondPlaceScores = sorted.filter(s => s.netScore === sorted[1].netScore);

    // All tied
    if (firstPlaceScores.length === 3) return 3;

    // Tie for first
    if (firstPlaceScores.length === 2) {
        if (playerNetScore === sorted[0].netScore) return 4;
        return 1;
    }

    // Tie for second
    if (secondPlaceScores.length === 2) {
        if (playerNetScore === sorted[0].netScore) return 5;
        return 2;
    }

    // No ties
    if (playerNetScore === sorted[0].netScore) return 5;
    if (playerNetScore === sorted[1].netScore) return 3;
    return 1;
}
```

---

## 3. BACKEND DATABASE HANDLING OF SCORING TYPES

### PlayerScorecard Entity

**Location:** [backend/src/main/java/com/degen/backend/entity/PlayerScorecard.java](backend/src/main/java/com/degen/backend/entity/PlayerScorecard.java)

```java
@Entity
@Table(name = "player_scorecard")
public class PlayerScorecard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_tee_time_id", nullable = false)
    private RoundTeeTime roundTeeTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hole_id", nullable = false)
    private Hole hole;

    @Column(name = "gross_score", nullable = false)
    private Integer grossScore;

    @Column(name = "net_score")
    private Integer netScore;      // Calculated from gross + handicap

    @Column(name = "game_points")
    private Integer gamePoints;    // Points for this hole (Nines/Split Skins)
}
```

### Net Score Calculation

**Location:** [backend/src/main/java/com/degen/backend/service/PlayerScorecardService.java#L272](backend/src/main/java/com/degen/backend/service/PlayerScorecardService.java#L272)

```java
private Integer calculateNetScore(PlayerScorecard scorecard) {
    // Formula: Net Score = Gross Score - Strokes To Subtract

    int baseSubtraction = courseHandicap / 18;      // Strokes per hole
    int remainingSubtraction = courseHandicap % 18; // Extra strokes for hardest holes

    // Additional stroke if hole is among the hardest holes
    int additionalStroke = (holeHandicap <= remainingSubtraction) ? 1 : 0;

    int totalStrokesToSubtract = baseSubtraction + additionalStroke;
    int netScore = grossScore - totalStrokesToSubtract;

    return Math.max(0, netScore);
}
```

Example:

- Course Handicap: 11
  - Base subtraction: 11 / 18 = 0
  - Extra strokes: 11 % 18 = 11
  - For holes with handicap ≤ 11: subtract 1 stroke (the 11 hardest holes)
  - For other holes: no subtraction

### Team Net Score Calculation

**Location:** [backend/src/main/java/com/degen/backend/service/PlayerScorecardService.java#L211](backend/src/main/java/com/degen/backend/service/PlayerScorecardService.java#L211)

```java
private Integer calculateTeamNetScore(RoundTeam team, Long holeId, Long gameId, Long scoringTypeId) {
    // Get all team members' net scores
    List<Integer> netScores = // [player1Net, player2Net, player3Net]

    // 2-Man Best Ball (gameId=1) or Nines (gameId=3): BEST (lowest) score
    if (gameId != null && (gameId == 1L || gameId == 3L)) {
        return netScores.stream().min(Integer::compareTo).orElse(null);
    }

    // 2-Man Aggregate: SUM of all scores
    return netScores.stream().reduce(0, Integer::sum);
}
```

### Game Points Calculation Flow

**Location:** [backend/src/main/java/com/degen/backend/service/TeamGamePointsService.java#L51](backend/src/main/java/com/degen/backend/service/TeamGamePointsService.java#L51)

```
Flow for Nines:
1. Get all players' PlayerScorecards → Calculate net scores
2. Group by team per hole
3. For each team: calculate Team Best Net Score (lowest player net score)
4. For each hole: compare all teams' best net scores, allocate team points (5/3/1)
5. For each hole: compare players within each team, allocate player points (5/3/1)
6. Store both game_points values (team-level and player-level)
```

### TeamHoleScore Entity (Team-Level Results)

```java
@Entity
public class TeamHoleScore {
    @ManyToOne
    private RoundTeam roundTeam;

    @ManyToOne
    private Hole hole;

    @Column(name = "net_score")
    private Integer netScore;        // Best of team's net scores

    @Column(name = "game_points")
    private Integer gamePoints;      // Team points for this hole (5/3/1)
}
```

### Scoring Type Routing Logic

**Location:** [backend/src/main/java/com/degen/backend/service/TeamGamePointsService.java#L31](backend/src/main/java/com/degen/backend/service/TeamGamePointsService.java#L31)

```java
public void calculateAndSaveTeamGamePoints(Long tournamentRoundId) {
    if (tournamentRound.getGame().getId() == 4L) {
        // Individual game
        calculateAndSaveIndividualGamePoints(tournamentRoundId);
    } else {
        // Team-based game (Nines, 2-Man Aggregate, etc.)
        // Routing based on scoringTypeId:
        Long scoringTypeId = tournamentRound.getScoringType().getId();

        if (scoringTypeId == 4L) {
            // NINES: Calculate both team points and player points
            calculateTeamPointsForHole(...);
            calculatePlayerPointsForHole(...);
        }
        if (scoringTypeId == 5L) {
            // SPLIT SKINS: Calculate individual split points
            calculateSplitSkinsGamePoints(...);
        }
    }
}
```

---

## 4. FRONTEND SCORECARD DISPLAY LOGIC

### Component Structure

**Location:** [frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.ts](frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.ts)

### Display Conditional Logic

**Location:** [frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.html#L82](frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.html#L82)

Points column is shown ONLY for specific scoring types:

```typescript
@if (
  getGameType() === "Nines" ||
  getGameType() === "Stableford" ||
  getScoringType() === "Split Skins"
) {
  <div class="column-points">POINTS</div>
}
```

### Game Points Display Per Hole

**Location:** [frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.html#L150](frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.html#L150)

```html
<div class="score-container">
  <input type="number" [value]="getPlayerScore(playerId, hole.id) || ''" />

  @if (getPlayerScore(playerId, hole.id)) {
  <span class="net-score">
    {{ playerScore.scores[hole.id].netScore || "-" }}
  </span>

  @if (getGameType() === "Nines" || ...) {
  <span class="game-points">
    {{ formatGamePoints(playerScore.scores[hole.id].gamePoints) }}
  </span>
  } }
</div>
```

This displays three values stacked in each hole cell:

1. **Input field**: Gross score entry
2. **Net score**: Calculated from gross + handicap (small text)
3. **Game points**: Points for this hole (5, 3, 1, 4, 2, or 3 for Nines)

### Game Points Totals Display

**Location:** [frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.html#L181](frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.html#L181)

```html
<div class="column-out">
  <div class="score-total">
    <span class="gross-total">{{ getPlayerNineTotal(playerId, 1, 9) }}</span>
    <span class="net-total">{{ getPlayerNineNetTotal(playerId, 1, 9) }}</span>
    @if (getGameType() === "Nines" || ...) {
    <span class="game-points-total">
      {{ formatGamePoints(getPlayerNineGamePoints(playerId, 1, 9)) }}
    </span>
    }
  </div>
</div>
```

Shows totals for Front 9, Back 9, and Overall:

- Gross total
- Net total
- Game points total (sum of all hole points)

### Format Game Points Helper

**Location:** [frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.ts#L382](frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.ts#L382)

```typescript
formatGamePoints(points: number | undefined): string {
  if (points === undefined || points === null) {
    return '0';
  }

  const scoringType = this.getScoringType();

  if (scoringType === 'Split Skins') {
    // Convert from cents to decimals (e.g., 50 cents = 0.50)
    return (points / 100).toFixed(2);
  }

  // For Nines: display as integer (5, 3, 1, 4, 2)
  return points.toString();
}
```

### Game Points Calculation Methods

**Location:** [frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.ts#L729](frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.ts#L729)

```typescript
// Get game points for a specific nine holes
getPlayerNineGamePoints(playerId: number, startHole: number, endHole: number): number {
  return this.holes()
    .filter(h => h.holeNumber >= startHole && h.holeNumber <= endHole)
    .reduce((total, hole) => {
      return total + (playerScore.scores[hole.id]?.gamePoints || 0);
    }, 0);
}

// Get total game points for all 18 holes
getPlayerTotalGamePoints(playerId: number): number {
  return this.holes().reduce((total, hole) => {
    return total + (playerScore.scores[hole.id]?.gamePoints || 0);
  }, 0);
}
```

### Getting Game Type and Scoring Type

**Location:** [frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.ts#L369](frontend/src/app/tournament/enter-scorecard/enter-scorecard.component.ts#L369)

```typescript
getGameType(): string {
  return this.selectedRound()?.game?.name || '';
}

getScoringType(): string {
  return this.selectedRound()?.scoringType?.scoringTypeName || '';
}
```

### Score Display Layout

The scorecard displays in each hole cell:

```
┌─────────────────────┐
│    Gross Score      │  (input field - editable)
│    Net Score        │  (calculated, small text)
│    Game Points      │  (5/3/1 for Nines)
└─────────────────────┘
```

Example for Nines hole result:

```
┌─────────────────┐
│       4         │  Gross
│       4         │  Net (par 4, no handicap strokes)
│       5         │  Points (best player)
└─────────────────┘
```

### Totals Display Layout

Front 9 / Back 9 / Totals columns show:

```
┌─────────────────┐
│     37/39/76    │  Gross Score: 37 (front 9) + 39 (back 9) = 76 total
│     36/38/74    │  Net Score
│     14/11/25    │  Game Points Total
└─────────────────┘
```

---

## 5. KEY DATA STRUCTURES

### TournamentRound (Contains Scoring Type Info)

```java
@Entity
public class TournamentRound {
    @ManyToOne
    private Game game;              // References the game type (Nines, 2-Man, etc.)

    @ManyToOne
    private ScoringType scoringType; // How points are scored (Stroke, Nines, Split Skins)
}
```

### Data Flow for Nines Game on a Single Hole:

```
Player 1          Player 2          Player 3
Gross: 5          Gross: 4          Gross: 4
HCP: 8 strokes    HCP: 8 strokes    HCP: 8 strokes
     ↓                 ↓                ↓
   Net: 5           Net: 4            Net: 4
     ↓ ──────────────────────────────────↓
     │  calculateTeamNetScore()          │
     │  (Best of 5, 4, 4 = 4)            │
     │  → TeamHoleScore.netScore = 4     │
     │  → TeamHoleScore.gamePoints = TBD │
     └──────────────────────────────────┘
           ↓
  calculatePlayerPointsForHole()
  Sort by net: [P2(4), P3(4), P1(5)]
  Tie for 1st: P2=4pts, P3=4pts, P1=1pt
       ↓
PlayerScorecard.gamePoints = [1, 4, 4]
```

---

## 6. SUMMARY TABLE: SCORE CALCULATIONS

| Component              | Type       | Calculation                | Stored Where               |
| ---------------------- | ---------- | -------------------------- | -------------------------- |
| **Gross Score**        | Individual | User input                 | PlayerScorecard.grossScore |
| **Net Score**          | Individual | gross - (handicap strokes) | PlayerScorecard.netScore   |
| **Team Net**           | Team       | Min(players' netScores)    | TeamHoleScore.netScore     |
| **Player Game Points** | Individual | 5/3/1 vs teammates         | PlayerScorecard.gamePoints |
| **Team Game Points**   | Team       | 5/3/1 vs other teams       | TeamHoleScore.gamePoints   |

---

## 7. SCORING TYPE MATRIX

| Scoring Type ID | Name        | Game Type        | Points Allocation         | Display in Scorecard    |
| --------------- | ----------- | ---------------- | ------------------------- | ----------------------- |
| 1               | Stroke      | Individual/Team  | None                      | No                      |
| 4               | Nines       | Team (3 players) | 5/3/1 per player per hole | Yes, game points column |
| 5               | Split Skins | Individual       | Decimal (cents stored)    | Yes, as 0.00 format     |

---

## 8. KEY FILES REFERENCE

### Backend

- **Entities**: [entity/](backend/src/main/java/com/degen/backend/entity/)
  - ScoringType.java
  - Game.java
  - PlayerScorecard.java
  - TeamHoleScore.java

- **Services**: [service/](backend/src/main/java/com/degen/backend/service/)
  - ScoringTypeService.java
  - TeamGamePointsService.java
  - PlayerScorecardService.java

- **Utils**: [util/](backend/src/main/java/com/degen/backend/util/)
  - GamePointsMigration.java (Nines calculation example)

### Frontend

- **Component**: [enter-scorecard/](frontend/src/app/tournament/enter-scorecard/)
  - enter-scorecard.component.ts (main logic)
  - enter-scorecard.component.html (display)
  - enter-scorecard.component.scss (styling)
