import { Component, inject, signal, ChangeDetectionStrategy, computed, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface Tournament {
  id: number;
  year: number;
  location: string;
}

interface TournamentRound {
  id: number;
  day: string;
  game: { id: number; name: string };
  course?: { id: number; name: string };
  scoringType?: { id: number; scoringTypeName: string };
}

interface LeaderboardEntry {
  playerId: number;
  playerName: string;
  roundPoints: Record<number, number>;
  totalPoints: number;
}

interface RoundLeaderboardEntry {
  playerId: number;
  playerName: string;
  score: number | null; // Net score relative to par
  thru: number; // Last hole completed
  roundPoints: number;
  totalPoints: number;
}

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="leaderboard-container">
      <h2>Leaderboard</h2>

      <!-- Tabs for switching between views -->
      <div class="leaderboard-tabs">
        <button 
          class="tab-button"
          [class.active]="viewMode() === 'tournament'"
          (click)="setViewMode('tournament')"
        >
          Tournament
        </button>
        <button 
          class="tab-button"
          [class.active]="viewMode() === 'round'"
          (click)="setViewMode('round')"
        >
          Current Round
        </button>
      </div>

      <!-- Tournament Leaderboard View -->
      @if (viewMode() === 'tournament') {
        <div class="controls">
          <div class="control-group">
            <label for="tournament-select">Tournament:</label>
            <select
              id="tournament-select"
              [(ngModel)]="selectedTournamentId"
              (change)="onTournamentChange(selectedTournamentId() || 0)"
            >
              <option [ngValue]="null">Select Tournament</option>
              @for (tournament of tournaments(); track tournament.id) {
                <option [ngValue]="tournament.id">
                  {{ tournament.year }} - {{ tournament.location }}
                </option>
              }
            </select>
          </div>
        </div>

        @if (selectedTournamentId() && leaderboardData().length > 0) {
          <div class="leaderboard-table-container">
            <table class="leaderboard-table">
              <thead>
                <tr>
                  <th class="rank">Rank</th>
                  <th class="player">Player</th>
                  @for (round of tournamentRounds(); track round.id) {
                    <th class="round">{{ round.day | date: "MM/dd" }}</th>
                  }
                  <th class="total">Total</th>
                </tr>
              </thead>
              <tbody>
                @for (
                  entry of leaderboardData();
                  track entry.playerId;
                  let rank = $index
                ) {
                  <tr>
                    <td class="rank">{{ rank + 1 }}</td>
                    <td class="player">
                      @if (isMobile()) {
                        {{ entry.playerName.split(' ').pop() }}
                      } @else {
                        {{ entry.playerName }}
                      }
                    </td>
                    @for (round of tournamentRounds(); track round.id) {
                      <td class="round" [style.background-color]="getRoundPointsColor(entry, round.id)">
                        {{ getRoundPoints(entry, round.id) }}
                      </td>
                    }
                    <td class="total">{{ formatPoints(entry.totalPoints) }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }

        @if (selectedTournamentId() && leaderboardData().length === 0) {
          <div class="empty-state">
            <p>No leaderboard data available for this tournament.</p>
          </div>
        }

        @if (!selectedTournamentId()) {
          <div class="empty-state">
            <p>Select a tournament to view the leaderboard.</p>
          </div>
        }
      }

      <!-- Round Leaderboard View -->
      @if (viewMode() === 'round') {
        <div class="controls">
          <div class="control-group">
            <label for="tournament-select-round">Tournament:</label>
            <select
              id="tournament-select-round"
              [(ngModel)]=\"selectedTournamentIdRound\"
              (change)=\"onTournamentChangeRound(selectedTournamentIdRound() || 0)\"
            >
              <option [ngValue]=\"null\">Select Tournament</option>
              @for (tournament of tournaments(); track tournament.id) {
                <option [ngValue]=\"tournament.id\">
                  {{ tournament.year }} - {{ tournament.location }}
                </option>
              }
            </select>
          </div>
          @if (tournamentRoundsRound().length > 0) {
            <div class="control-group">
              <label for="round-select">Round:</label>
              <select
                id="round-select"
                [(ngModel)]="selectedRoundId"
                (change)="onRoundChange(selectedRoundId() || 0)"
              >
                <option [ngValue]="null">Select Round</option>
                @for (round of tournamentRoundsRound(); track round.id) {
                  <option [ngValue]="round.id">
                    {{ round.day | date: "MMM dd, yyyy" }}
                    @if (round.course?.name) {
                      - {{ round.course?.name }}
                    }
                  </option>
                }
              </select>
            </div>
          }
        </div>

        @if (selectedRoundId() && roundLeaderboardData().length > 0) {
          <div class="leaderboard-table-container">
            <table class="leaderboard-table">
              <thead>
                <tr>
                  <th class="rank">Rank</th>
                  <th class="player">Player</th>
                  <th class="score" (click)="onColumnSort('score')" style="cursor: pointer; user-select: none;">
                    Score
                    @if (sortColumn() === 'score') {
                      {{ sortDirection() === 'asc' ? '▼' : '▲' }}
                    }
                  </th>
                  <th class="thru" (click)="onColumnSort('thru')" style="cursor: pointer; user-select: none;">
                    Thru
                    @if (sortColumn() === 'thru') {
                      {{ sortDirection() === 'asc' ? '▼' : '▲' }}
                    }
                  </th>
                  <th class="round-points" (click)="onColumnSort('roundPoints')" style="cursor: pointer; user-select: none;">
                    Rd Pts
                    @if (sortColumn() === 'roundPoints') {
                      {{ sortDirection() === 'asc' ? '▼' : '▲' }}
                    }
                  </th>
                  <th class="total" (click)="onColumnSort('totalPoints')" style="cursor: pointer; user-select: none;">
                    Total
                    @if (sortColumn() === 'totalPoints') {
                      {{ sortDirection() === 'asc' ? '▼' : '▲' }}
                    }
                  </th>
                </tr>
              </thead>
              <tbody>
                @for (
                  entry of roundLeaderboardData();
                  track entry.playerId;
                  let rank = $index
                ) {
                  <tr>
                    <td class="rank">{{ rank + 1 }}</td>
                    <td class="player">
                      @if (isMobile()) {
                        {{ entry.playerName.split(' ').pop() }}
                      } @else {
                        {{ entry.playerName }}
                      }
                    </td>
                    <td class="score" [style.background-color]="getScoreColor(entry)">
                      @if (entry.score !== null) {
                        @if (entry.score === 0) {
                          E
                        } @else {
                          {{ entry.score > 0 ? '+' : '' }}{{ entry.score }}
                        }
                      } @else {
                        -
                      }
                    </td>
                    <td class="thru">{{ entry.thru }}</td>
                    <td class="round-points" [style.background-color]="getRoundPointsColorRound(entry)">{{ entry.roundPoints }}</td>
                    <td class="total" [style.background-color]="getTotalPointsColor(entry)">{{ entry.totalPoints }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }

        @if (selectedRoundId() && roundLeaderboardData().length === 0) {
          <div class="empty-state">
            <p>No leaderboard data available for this round.</p>
          </div>
        }

        @if (!selectedRoundId()) {
          <div class="empty-state">
            <p>Select a round to view live leaderboard.</p>
          </div>
        }
      }
    </div>
  `,
  styleUrl: './leaderboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaderboardComponent {
  private http = inject(HttpClient);

  // Tournament view signals
  tournaments = signal<Tournament[]>([]);
  selectedTournamentId = signal<number | null>(null);
  tournamentRounds = signal<TournamentRound[]>([]);
  leaderboardData = signal<LeaderboardEntry[]>([]);
  isLoading = signal(false);

  // Round view signals
  viewMode = signal<'tournament' | 'round'>('tournament');
  selectedTournamentIdRound = signal<number | null>(null);
  tournamentRoundsRound = signal<TournamentRound[]>([]);
  selectedRoundId = signal<number | null>(null);
  roundLeaderboardData = signal<RoundLeaderboardEntry[]>([]);
  sortColumn = signal<'score' | 'thru' | 'roundPoints' | 'totalPoints'>('score');
  sortDirection = signal<'asc' | 'desc'>('asc');
  isMobile = signal(false);

  constructor() {
    this.loadTournaments();
    this.checkIsMobile();
    this.setDefaultViewMode();
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: any) {
    this.checkIsMobile();
  }

  private checkIsMobile() {
    this.isMobile.set(window.innerWidth <= 768);
  }

  private setDefaultViewMode() {
    this.viewMode.set(this.isMobile() ? 'round' : 'tournament');
  }

  setViewMode(mode: 'tournament' | 'round') {
    this.viewMode.set(mode);
  }

  loadTournaments() {
    this.http.get<Tournament[]>(`${environment.apiUrl}/api/tournaments`).subscribe({
      next: (data) => {
        this.tournaments.set(data);
        // Auto-select 2026 tournament for round view and load it
        const tournament2026 = data.find(t => t.year === 2026);
        if (tournament2026) {
          this.selectedTournamentIdRound.set(tournament2026.id);
          this.loadTournamentRoundsForRound(tournament2026.id);
          // Also auto-select 2026 for tournament view
          this.selectedTournamentId.set(tournament2026.id);
          this.loadTournamentRounds(tournament2026.id);
          this.loadLeaderboard(tournament2026.id);
        }
      },
      error: (err) => {
        console.error('Error loading tournaments:', err);
        alert('Error loading tournaments');
      }
    });
  }

  // Tournament view methods
  onTournamentChange(tournamentId: number) {
    this.selectedTournamentId.set(tournamentId);
    this.tournamentRounds.set([]);
    this.leaderboardData.set([]);

    if (tournamentId) {
      this.loadTournamentRounds(tournamentId);
      this.loadLeaderboard(tournamentId);
    }
  }

  loadTournamentRounds(tournamentId: number) {
    this.http.get<TournamentRound[]>(`${environment.apiUrl}/api/tournament-rounds?tournamentId=${tournamentId}`).subscribe({
      next: (data) => {
        // Sort by day
        const sorted = data.sort((a, b) => new Date(a.day).getTime() - new Date(b.day).getTime());
        this.tournamentRounds.set(sorted);
      },
      error: (err) => {
        console.error('Error loading rounds:', err);
      }
    });
  }

  loadLeaderboard(tournamentId: number) {
    this.isLoading.set(true);
    this.http.get<LeaderboardEntry[]>(`${environment.apiUrl}/api/leaderboard/tournament/${tournamentId}`).subscribe({
      next: (data) => {
        this.leaderboardData.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading leaderboard:', err);
        alert('Error loading leaderboard');
        this.isLoading.set(false);
      }
    });
  }

  // Round view methods
  onTournamentChangeRound(tournamentId: number) {
    this.selectedTournamentIdRound.set(tournamentId);
    this.tournamentRoundsRound.set([]);
    this.selectedRoundId.set(null);
    this.roundLeaderboardData.set([]);

    if (tournamentId) {
      this.loadTournamentRoundsForRound(tournamentId);
    }
  }

  loadTournamentRoundsForRound(tournamentId: number) {
    this.http.get<TournamentRound[]>(`${environment.apiUrl}/api/tournament-rounds?tournamentId=${tournamentId}`).subscribe({
      next: (data) => {
        // Sort by day
        const sorted = data.sort((a, b) => new Date(a.day).getTime() - new Date(b.day).getTime());
        this.tournamentRoundsRound.set(sorted);
        
        // Auto-detect current round
        const currentRound = this.findCurrentRound(sorted);
        if (currentRound) {
          this.selectedRoundId.set(currentRound.id);
          this.loadRoundLeaderboard(currentRound.id);
        }
      },
      error: (err) => {
        console.error('Error loading rounds:', err);
      }
    });
  }

  private findCurrentRound(rounds: TournamentRound[]): TournamentRound | null {
    if (rounds.length === 0) return null;

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // Check for exact date match
    for (const round of rounds) {
      const roundDate = new Date(round.day);
      roundDate.setHours(0, 0, 0, 0);
      if (roundDate.getTime() === today.getTime()) {
        return round;
      }
    }

    // If before all rounds, return first
    const firstRoundDate = new Date(rounds[0].day);
    firstRoundDate.setHours(0, 0, 0, 0);
    if (today.getTime() < firstRoundDate.getTime()) {
      return rounds[0];
    }

    // If after all rounds, return last
    return rounds[rounds.length - 1];
  }

  onRoundChange(roundId: number) {
    this.selectedRoundId.set(roundId);
    this.roundLeaderboardData.set([]);

    if (roundId) {
      this.loadRoundLeaderboard(roundId);
    }
  }

  loadRoundLeaderboard(roundId: number) {
    this.isLoading.set(true);
    this.http.get<RoundLeaderboardEntry[]>(`${environment.apiUrl}/api/leaderboard/round/${roundId}`).subscribe({
      next: (data) => {
        // Sort by score (ascending - lower is better relative to par)
        const sorted = data.sort((a, b) => {
          // If either has null score, put non-null first
          if (a.score === null && b.score === null) return 0;
          if (a.score === null) return 1;
          if (b.score === null) return -1;
          return a.score - b.score;
        });
        this.roundLeaderboardData.set(sorted);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading round leaderboard:', err);
        this.roundLeaderboardData.set([]);
        this.isLoading.set(false);
      }
    });
  }

  getRoundPoints(entry: LeaderboardEntry, roundId: number): string {
    const points = (entry.roundPoints as any)[roundId];
    if (!points && points !== 0) return '-';
    // Format: no decimals for whole numbers, 2 decimals otherwise
    return Number.isInteger(points) ? String(points) : points.toFixed(2);
  }

  getRoundPointsColor(entry: LeaderboardEntry, roundId: number): string {
    const points = (entry.roundPoints as any)[roundId];
    if (!points && points !== 0) return 'white';

    // Get all entries' scores for this round
    const allEntries = this.leaderboardData();
    const roundScores = allEntries
      .map(e => (e.roundPoints as any)[roundId])
      .filter(score => score !== undefined && score !== null)
      .sort((a, b) => b - a); // Sort descending (higher score is better)

    if (roundScores.length === 0) return 'white';

    // Find unique scores for this round
    const uniqueScores = [...new Set(roundScores)];
    
    // Find the rank of the current player's score (0-based index in unique scores)
    const scoreIndex = uniqueScores.indexOf(points);
    
    // Color scale (same as scorecard):
    // Best (1st) = Dark Green #4CAF50
    // Good = Medium Green #81C784
    // Middle = Light Green #C8E6C9 -> White
    // Bad = Light Red #FFCDD2
    // Worst = Dark Red #E57373
    
    const totalUnique = uniqueScores.length;
    
    if (totalUnique === 1) return '#FFFFFF'; // Only one unique score, white
    
    // Map position to color
    // Position 0 (best) to middle = green transition
    // Middle to end (worst) = red transition
    const middle = Math.ceil(totalUnique / 2) - 1;
    
    if (scoreIndex <= middle) {
      // Green side: best to middle
      if (scoreIndex === 0) {
        return '#9FD79F'; // Light green - best
      } else if (scoreIndex === 1 && totalUnique > 2) {
        return '#B3E5B3'; // Medium light green
      } else {
        return '#E8F5E9'; // Very light green
      }
    } else {
      // Red side: middle to worst
      const posFromEnd = totalUnique - 1 - scoreIndex;
      if (posFromEnd === 0) {
        return '#F5A9A9'; // Light red - worst
      } else if (posFromEnd === 1 && totalUnique > 2) {
        return '#FFCCCC'; // Medium light red
      } else {
        return '#FFEBEE'; // Very light red
      }
    }
  }

  private getColorFromRatio(ratio: number): string {
    // Clamp ratio between 0 and 1
    ratio = Math.max(0, Math.min(1, ratio));

    if (ratio <= 0.5) {
      // Green to White transition (0 to 0.5)
      const normalizedRatio = ratio / 0.5;
      const r = Math.round(76 + (255 - 76) * normalizedRatio);
      const g = 255;
      const b = Math.round(80 + (255 - 80) * normalizedRatio);
      return `rgb(${r}, ${g}, ${b})`;
    } else {
      // White to Red transition (0.5 to 1)
      const normalizedRatio = (ratio - 0.5) / 0.5;
      const r = 255;
      const g = Math.round(255 - (255 - 229) * normalizedRatio);
      const b = Math.round(255 - (255 - 115) * normalizedRatio);
      return `rgb(${r}, ${g}, ${b})`;
    }
  }

  formatPoints(points: number): string {
    // Format total points: no decimals for whole numbers, 2 decimals otherwise
    return Number.isInteger(points) ? String(points) : points.toFixed(2);
  }

  onColumnSort(column: 'score' | 'thru' | 'roundPoints' | 'totalPoints') {
    // If clicking same column, toggle direction; otherwise set new column with asc
    if (this.sortColumn() === column) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortColumn.set(column);
      this.sortDirection.set('asc');
    }
    this.sortRoundLeaderboard();
  }

  private sortRoundLeaderboard() {
    const data = [...this.roundLeaderboardData()];
    const column = this.sortColumn();
    const direction = this.sortDirection();

    data.sort((a, b) => {
      let aVal: any = 0;
      let bVal: any = 0;

      switch (column) {
        case 'score':
          aVal = a.score ?? 9999; // Nulls go to bottom
          bVal = b.score ?? 9999;
          break;
        case 'thru':
          aVal = a.thru;
          bVal = b.thru;
          break;
        case 'roundPoints':
          aVal = a.roundPoints;
          bVal = b.roundPoints;
          break;
        case 'totalPoints':
          aVal = a.totalPoints;
          bVal = b.totalPoints;
          break;
      }

      const result = aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
      return direction === 'asc' ? result : -result;
    });

    this.roundLeaderboardData.set(data);
  }

  getScoreColor(entry: RoundLeaderboardEntry): string {
    if (entry.score === null) return '#FFFFFF';

    const allScores = this.roundLeaderboardData()
      .map(e => e.score)
      .filter(s => s !== null) as number[];

    if (allScores.length === 0) return '#FFFFFF';

    const uniqueScores = [...new Set(allScores)].sort((a, b) => a - b);
    const scoreIndex = uniqueScores.indexOf(entry.score!);
    const totalUnique = uniqueScores.length;

    if (totalUnique === 1) return '#FFFFFF';

    // Lower score is better (negative is good), higher score is bad (positive)
    // Best = index 0 (green), Worst = index last (red)
    const middle = Math.ceil(totalUnique / 2) - 1;

    if (scoreIndex <= middle) {
      if (scoreIndex === 0) return '#9FD79F';
      else if (scoreIndex === 1 && totalUnique > 2) return '#B3E5B3';
      else return '#E8F5E9';
    } else {
      const posFromEnd = totalUnique - 1 - scoreIndex;
      if (posFromEnd === 0) return '#F5A9A9';
      else if (posFromEnd === 1 && totalUnique > 2) return '#FFCCCC';
      else return '#FFEBEE';
    }
  }

  getRoundPointsColorRound(entry: RoundLeaderboardEntry): string {
    const allPoints = this.roundLeaderboardData().map(e => e.roundPoints);
    
    if (allPoints.length === 0) return '#FFFFFF';

    const uniquePoints = [...new Set(allPoints)].sort((a, b) => b - a); // Sort desc for points (higher is better)
    const pointIndex = uniquePoints.indexOf(entry.roundPoints);
    const totalUnique = uniquePoints.length;

    if (totalUnique === 1) return '#FFFFFF';

    // Higher points is better (green), lower points is worse (red)
    const middle = Math.ceil(totalUnique / 2) - 1;

    if (pointIndex <= middle) {
      if (pointIndex === 0) return '#9FD79F';
      else if (pointIndex === 1 && totalUnique > 2) return '#B3E5B3';
      else return '#E8F5E9';
    } else {
      const posFromEnd = totalUnique - 1 - pointIndex;
      if (posFromEnd === 0) return '#F5A9A9';
      else if (posFromEnd === 1 && totalUnique > 2) return '#FFCCCC';
      else return '#FFEBEE';
    }
  }

  getTotalPointsColor(entry: RoundLeaderboardEntry): string {
    const allTotals = this.roundLeaderboardData().map(e => e.totalPoints);
    
    if (allTotals.length === 0) return '#FFFFFF';

    const uniqueTotals = [...new Set(allTotals)].sort((a, b) => b - a); // Sort desc (higher is better)
    const totalIndex = uniqueTotals.indexOf(entry.totalPoints);
    const totalUnique = uniqueTotals.length;

    if (totalUnique === 1) return '#FFFFFF';

    // Higher total points is better (green), lower is worse (red)
    const middle = Math.ceil(totalUnique / 2) - 1;

    if (totalIndex <= middle) {
      if (totalIndex === 0) return '#9FD79F';
      else if (totalIndex === 1 && totalUnique > 2) return '#B3E5B3';
      else return '#E8F5E9';
    } else {
      const posFromEnd = totalUnique - 1 - totalIndex;
      if (posFromEnd === 0) return '#F5A9A9';
      else if (posFromEnd === 1 && totalUnique > 2) return '#FFCCCC';
      else return '#FFEBEE';
    }
  }
}
