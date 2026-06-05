import { Component, OnInit, signal, computed, HostListener } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../environments/environment';

interface Tournament {
  id: number;
  year: number;
  location: string;
}

interface TournamentRound {
  id: number;
  tournament: Tournament;
  day: string;
  courseId?: number;
  game: { id: number; name: string };
  scoringType?: { id: number; scoringTypeName: string };
  course?: { 
    id: number; 
    name: string;
    slope?: number;
    rating?: number;
  };
  interGroup?: boolean;
}

interface RoundTeeTime {
  id: number;
  teeTime: string;
  player1Id: number | null;
  player2Id: number | null;
  player3Id: number | null;
  player4Id: number | null;
  tournamentRound: TournamentRound;
}

interface Hole {
  id: number;
  holeNumber: number;
  par: number;
  yards: number;
  handicap: number;
}

interface PlayerScorecard {
  id?: number;
  roundTeeTime: RoundTeeTime;
  player: { id: number; firstName?: string; lastName?: string };
  hole: { id: number };
  grossScore?: number;
  netScore?: number;
  gamePoints?: number;
  courseHandicap?: number;
}

interface PlayerScore {
  playerId: number;
  firstName: string;
  lastName: string;
  handicap?: number;
  courseHandicap?: number;
  scores: { [holeId: number]: { scorecardId?: number; grossScore?: number; netScore?: number; gamePoints?: number } };
}

interface RoundTeam {
  id: number;
  roundTeeTime: RoundTeeTime;
  player1Id: number;
  player2Id: number;
  player3Id?: number;
}

interface TeamScore {
  teamId: number;
  playerIds: number[];
  scores: { [holeId: number]: { grossScore?: number; netScore?: number; gamePoints?: number } };
}

@Component({
  selector: 'app-enter-scorecard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './enter-scorecard.component.html',
  styleUrls: ['./enter-scorecard.component.scss']
})
export class EnterScorecardComponent implements OnInit {
  tournaments = signal<Tournament[]>([]);
  rounds = signal<TournamentRound[]>([]);
  teeTimes = signal<RoundTeeTime[]>([]);
  holes = signal<Hole[]>([]);
  playerScores = signal<PlayerScore[]>([]);
  teamScores = signal<TeamScore[]>([]);
  roundTeams = signal<RoundTeam[]>([]);

  selectedTournamentId = signal<number | null>(null);
  selectedRoundId = signal<number | null>(null);
  selectedTeeTimeId = signal<number | null>(null);
  selectedCourseId = signal<number | null>(null);
  isMobile = signal(false);

  selectedTeeTime = computed(() => {
    const teeTimeId = this.selectedTeeTimeId();
    if (!teeTimeId) return undefined;
    return this.teeTimes().find(t => t.id === teeTimeId);
  });

  selectedRound = computed(() => {
    const roundId = this.selectedRoundId();
    if (!roundId) return undefined;
    return this.rounds().find(r => r.id === roundId);
  });

  playerIds = computed(() => {
    const teeTime = this.selectedTeeTime();
    if (!teeTime) return [];
    const ids = [];
    if (teeTime.player1Id) ids.push(teeTime.player1Id);
    if (teeTime.player2Id) ids.push(teeTime.player2Id);
    if (teeTime.player3Id) ids.push(teeTime.player3Id);
    if (teeTime.player4Id) ids.push(teeTime.player4Id);
    return ids;
  });

  constructor(private http: HttpClient) {
    this.checkIsMobile();
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: any) {
    this.checkIsMobile();
  }

  private checkIsMobile() {
    this.isMobile.set(window.innerWidth <= 768);
  }

  ngOnInit() {
    this.loadTournaments();
  }

  loadTournaments() {
    this.http.get<Tournament[]>(`${environment.apiUrl}/api/tournaments`).subscribe({
      next: (data) => {
        this.tournaments.set(data);
        // Auto-select 2026 tournament and load current round
        const tournament2026 = data.find(t => t.year === 2026);
        if (tournament2026) {
          this.selectedTournamentId.set(tournament2026.id);
          this.http.get<TournamentRound[]>(`${environment.apiUrl}/api/tournament-rounds?tournamentId=${tournament2026.id}`)
            .subscribe({
              next: (rounds) => {
                // Sort by day
                const sorted = rounds.sort((a, b) => new Date(a.day).getTime() - new Date(b.day).getTime());
                this.rounds.set(sorted);
                // Auto-select current round
                const currentRound = this.findCurrentRound(sorted);
                if (currentRound) {
                  this.selectedRoundId.set(currentRound.id);
                  this.onRoundChange(currentRound.id);
                }
              },
              error: (err) => {}
            });
        }
      },
      error: (err) => {}
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

  onTournamentChange(tournamentId: number | null) {
    this.selectedTournamentId.set(tournamentId);
    if (tournamentId) {
      this.rounds.set([]);
      this.teeTimes.set([]);
      this.holes.set([]);
      this.playerScores.set([]);
      this.selectedRoundId.set(null);
      this.selectedTeeTimeId.set(null);
      this.selectedCourseId.set(null);

      this.http.get<TournamentRound[]>(`${environment.apiUrl}/api/tournament-rounds?tournamentId=${tournamentId}`)
        .subscribe({
          next: (data) => {
            this.rounds.set(data);
          },
          error: (err) => {}
        });
    }
  }

  onRoundChange(roundId: number | null) {
    this.selectedRoundId.set(roundId);
    if (roundId) {
      this.teeTimes.set([]);
      this.holes.set([]);
      this.playerScores.set([]);
      this.teamScores.set([]);
      this.roundTeams.set([]);
      this.selectedTeeTimeId.set(null);

      // Find the selected round and extract courseId
      const selectedRound = this.rounds().find(r => r.id === roundId);
      if (selectedRound) {
        const courseId = selectedRound.course?.id || selectedRound.courseId;
        this.selectedCourseId.set(courseId || null);
      }

      this.http.get<RoundTeeTime[]>(`${environment.apiUrl}/api/round-tee-times?roundId=${roundId}`)
        .subscribe({
          next: (data) => {
            this.teeTimes.set(data);
          },
          error: (err) => {}
        });
    }
  }

  onTeeTimeChange(teeTimeId: number | null) {
    this.selectedTeeTimeId.set(teeTimeId);
    if (teeTimeId) {
      const selectedTeeTime = this.teeTimes().find(t => t.id === teeTimeId);
      if (selectedTeeTime) {
        const courseId = this.selectedCourseId();
        if (courseId) {
          // Load holes FIRST, then load scorecards once holes are loaded
          this.loadHolesForCourseAndThenScorecards(courseId, teeTimeId);
        } else {
          this.holes.set([]);
          this.loadScorecards(teeTimeId);
        }

        // Load teams for inter-group scoring
        this.loadTeams(teeTimeId);
      }
    }
  }

  private loadHolesForCourseAndThenScorecards(courseId: number, teeTimeId: number) {
    this.http.get<Hole[]>(`${environment.apiUrl}/api/holes?courseId=${courseId}`)
      .subscribe({
        next: (data) => {
          this.holes.set(data);
          // NOW load scorecards after holes are set
          this.loadScorecards(teeTimeId);
        },
        error: (err) => {
          this.holes.set([]);
          this.loadScorecards(teeTimeId);
        }
      });
  }

  private loadHolesForCourse(courseId: number) {
    this.http.get<Hole[]>(`${environment.apiUrl}/api/holes?courseId=${courseId}`)
      .subscribe({
        next: (data) => {
          this.holes.set(data);
        },
        error: (err) => {}
      });
  }

  private loadScorecards(teeTimeId: number) {
    const allPlayerIds = this.playerIds();
    if (allPlayerIds.length === 0) {
      this.playerScores.set([]);
      return;
    }

    const tournamentId = this.selectedTournamentId();
    const selectedRound = this.rounds().find(r => r.id === this.selectedRoundId());
    const courseInfo = selectedRound?.course;

    // Load all player details in parallel using Promise.all
    Promise.all(
      allPlayerIds.map(playerId =>
        Promise.all([
          this.loadPlayerDetails(playerId),
          tournamentId ? this.loadPlayerHandicap(playerId, tournamentId) : Promise.resolve(0)
        ]).then(([player, handicap]) => ({
          playerId,
          firstName: player.firstName || '',
          lastName: player.lastName || '',
          handicap: handicap || 0
        }))
      )
    ).then(playerDetailsArray => {
      const playerDetailsMap = new Map(playerDetailsArray.map(p => [p.playerId, p]));
      this.loadScorecardsWithPlayerDetails(teeTimeId, playerDetailsMap, courseInfo);
    }).catch(err => {
      // Fallback: load with empty details
      const playerDetailsMap = new Map<number, any>();
      allPlayerIds.forEach(pid => {
        playerDetailsMap.set(pid, { playerId: pid, firstName: '', lastName: '', handicap: 0 });
      });
      this.loadScorecardsWithPlayerDetails(teeTimeId, playerDetailsMap, courseInfo);
    });
  }

  private loadPlayerDetails(playerId: number): Promise<any> {
    return new Promise((resolve, reject) => {
      this.http.get<any>(`${environment.apiUrl}/api/players/${playerId}`).subscribe({
        next: (data) => resolve(data),
        error: (err) => resolve({ firstName: '', lastName: '' }) // Fallback
      });
    });
  }

  private loadPlayerHandicap(playerId: number, tournamentId: number): Promise<number> {
    return new Promise((resolve, reject) => {
      this.http.get<any[]>(`${environment.apiUrl}/api/tournament-handicaps?playerId=${playerId}&tournamentId=${tournamentId}`).subscribe({
        next: (data) => {
          const handicap = data && data.length > 0 ? data[0].handicap : 0;
          resolve(handicap || 0);
        },
        error: (err) => resolve(0) // Fallback
      });
    });
  }

  private loadScorecardsWithPlayerDetails(teeTimeId: number, playerDetailsMap: Map<number, { firstName: string; lastName: string; handicap: number }>, courseInfo: any) {
    this.http.get<PlayerScorecard[]>(`${environment.apiUrl}/api/player-scorecards/by-tee-time/${teeTimeId}`)
      .subscribe({
        next: (scorecards) => {
          const playerScoresMap = new Map<number, PlayerScore>();
          
          // Calculate course par from holes
          let coursePar = 0;
          this.holes().forEach(hole => coursePar += hole.par);
          
          // Initialize all players from the tee_time with their details
          this.playerIds().forEach(playerId => {
            const playerDetails = playerDetailsMap.get(playerId) || { firstName: '', lastName: '', handicap: 0 };
            const courseHandicap = this.calculateCourseHandicap(playerDetails.handicap, courseInfo, coursePar);
            
            // Pre-initialize scores object for ALL holes with empty values
            const scores: any = {};
            this.holes().forEach(hole => {
              scores[hole.id] = {
                scorecardId: undefined,
                grossScore: null,
                netScore: undefined,
                gamePoints: undefined
              };
            });
            
            playerScoresMap.set(playerId, {
              playerId,
              firstName: playerDetails.firstName,
              lastName: playerDetails.lastName,
              handicap: playerDetails.handicap,
              courseHandicap,
              scores
            });
          });
          
          // Populate scores from scorecards
          scorecards.forEach(scorecard => {
            const playerId = scorecard.player.id;
            const playerScore = playerScoresMap.get(playerId);
            
            if (playerScore) {
              playerScore.scores[scorecard.hole.id] = {
                scorecardId: scorecard.id,
                grossScore: scorecard.grossScore || undefined,
                netScore: scorecard.netScore || undefined,
                gamePoints: scorecard.gamePoints || undefined
              };
            }
          });

          this.playerScores.set(Array.from(playerScoresMap.values()));
          
          // Recalculate team scores now that player scores are loaded
          this.updateTeamScores();
          
          // Then load any saved game points from database
          this.loadTeamHoleScores();
        },
        error: (err) => {}
      });
  }

  private calculateCourseHandicap(playerHandicap: number, courseInfo: any, coursePar: number): number {
    if (!courseInfo) {
      return playerHandicap || 0;
    }
    
    // Get course properties
    const slope = courseInfo.slope;
    const rating = courseInfo.rating;
    
    // Validate we have required data
    if (typeof slope !== 'number' || typeof rating !== 'number' || !coursePar) {
      return playerHandicap || 0;
    }
    
    // Formula: handicap × (slope / 113) + (rating - par)
    const courseHandicap = playerHandicap * (slope / 113) + (rating - coursePar);
    return Math.round(courseHandicap);
  }

  getGameType(): string {
    return this.selectedRound()?.game?.name || '';
  }

  getScoringType(): string {
    return this.selectedRound()?.scoringType?.scoringTypeName || '';
  }

  /**
   * Format game points for display
   * For Split Skins: convert from cents to decimals (e.g., 50 -> 0.50, 33 -> 0.33)
   * For Nines and Stableford: display as-is (integer points, can be negative)
   */
  formatGamePoints(points: number | undefined): string {
    if (points === undefined || points === null) {
      return '0';
    }
    
    const scoringType = this.getScoringType();
    if (scoringType === 'Split Skins') {
      // Convert from cents to decimals and show 2 decimal places
      return (points / 100).toFixed(2);
    }
    
    // For Nines, Stableford and other types, show as integer (can be negative for Stableford)
    return points.toString();
  }

  /**
   * Calculate game points for a specific player on a hole based on Nines rules
   * Nines: 3 players, 9 points per hole
   * - Standard: 1st=5, 2nd=3, 3rd=1
   * - Tie for 1st: each=4, 3rd=1
   * - All tied: each=3
   * - Tie for 2nd: 1st=5, 2nd (both)=2 each
   */
  private calculateGamePointsForHole(playerId: number, hole: Hole): number {
    const gameType = this.getGameType();
    if (gameType !== 'Nines') {
      return 0;
    }

    // Get all players' net scores for this hole
    const holeScores = this.playerScores()
      .map(ps => ({
        playerId: ps.playerId,
        grossScore: ps.scores[hole.id]?.grossScore,
        courseHandicap: ps.courseHandicap
      }))
      .filter(ps => ps.grossScore !== undefined && ps.grossScore !== null)
      .map(ps => ({
        playerId: ps.playerId,
        netScore: this.calculateNetScore(ps.grossScore!, ps.courseHandicap, hole)
      }))
      .filter(ps => ps.netScore !== undefined && ps.netScore !== null);

    // Need exactly 3 players for Nines
    if (holeScores.length !== 3) {
      return 0;
    }

    // Sort by net score (ascending = best score first)
    const sorted = [...holeScores].sort((a, b) => (a.netScore || 0) - (b.netScore || 0));

    // Find player's position and calculate points
    const playerScore = holeScores.find(hs => hs.playerId === playerId);
    if (!playerScore) {
      return 0;
    }

    const playerNetScore = playerScore.netScore || 0;

    // Count ties
    const firstPlaceScores = sorted.filter(s => s.netScore === sorted[0].netScore);
    const secondPlaceScores = sorted.filter(s => s.netScore === sorted[1].netScore);

    // All tied
    if (firstPlaceScores.length === 3) {
      return 3;
    }

    // Tie for first place (2 players)
    if (firstPlaceScores.length === 2) {
      if (playerNetScore === sorted[0].netScore) {
        return 4; // Tied for first
      }
      return 1; // Third place
    }

    // Tie for second place (1st unique, 2nd and 3rd tied)
    if (secondPlaceScores.length === 2) {
      if (playerNetScore === sorted[0].netScore) {
        return 5; // First place
      }
      return 2; // Tied for second
    }

    // Standard scoring: no ties
    if (playerNetScore === sorted[0].netScore) {
      return 5; // First place
    } else if (playerNetScore === sorted[1].netScore) {
      return 3; // Second place
    }
    return 1; // Third place
  }

  private updateGamePointsForAllHoles() {
    // Recalculate game points for all holes after any score changes
    const updatedPlayerScores = this.playerScores().map(playerScore => {
      const updatedScores = { ...playerScore.scores };
      this.holes().forEach(hole => {
        if (updatedScores[hole.id] && updatedScores[hole.id].grossScore) {
          const gamePoints = this.calculateGamePointsForHole(playerScore.playerId, hole);
          updatedScores[hole.id] = {
            ...updatedScores[hole.id],
            gamePoints
          };
        }
      });
      return { ...playerScore, scores: updatedScores };
    });
    this.playerScores.set(updatedPlayerScores);
  }

  calculateNetScore(grossScore: number | undefined | null, courseHandicap: number | undefined | null, hole: Hole | undefined | null): number | undefined {
    if (!grossScore || !courseHandicap || !hole) {
      return undefined;
    }
    
    // Calculate shots to subtract
    // If handicap is 18, subtract 1 from each hole
    // If handicap is 36, subtract 2 from each hole
    // If handicap is 11, subtract 1 from the 11 hardest holes
    const baseSubtraction = Math.floor(courseHandicap / 18);
    const remainingSubtraction = courseHandicap % 18;
    
    // Additional stroke if this hole is among the remaining hardest holes
    const additionalStroke = hole.handicap <= remainingSubtraction ? 1 : 0;
    
    const netScore = grossScore - baseSubtraction - additionalStroke;
    return Math.max(0, netScore);
  }

  calculateStablefordGamePoints(netScore: number | undefined, hole: Hole | undefined): number | undefined {
    if (netScore === undefined || !hole) {
      return undefined;
    }
    
    const par = hole.par || 4;
    const scoreDiff = netScore - par;
    
    if (scoreDiff <= -2) {
      // Eagle or better = 8 pts
      return 8;
    } else if (scoreDiff === -1) {
      // Birdie = 4 pts
      return 4;
    } else if (scoreDiff === 0) {
      // Par = 2 pts
      return 2;
    } else if (scoreDiff === 1) {
      // Bogey = 0 pts
      return 0;
    } else {
      // Double Bogey or worse = -2 pts
      return -2;
    }
  }

  getPlayerScore(playerId: number, holeId: number): number | undefined {
    const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
    return playerScore?.scores[holeId]?.grossScore;
  }

  updateScore(playerId: number, hole: Hole, event: Event) {
    const target = event.target as HTMLInputElement | HTMLSelectElement;
    const newScore = target.value.trim();

    // Allow empty to clear score
    if (newScore === '') {
      this.saveScore(playerId, hole, null);
      return;
    }

    const score = parseInt(newScore, 10);
    if (isNaN(score) || score < 0 || score > 13) {
      alert('Score must be between 0 and 13');
      target.value = this.getPlayerScore(playerId, hole.id)?.toString() || '';
      return;
    }

    this.saveScore(playerId, hole, score);
  }

  private saveScore(playerId: number, hole: Hole, score: number | null) {
    console.log(`Saving score for player ${playerId}, hole ${hole.id}: ${score}`);
    const teeTimeId = this.selectedTeeTimeId();
    if (!teeTimeId) {
      console.warn('No teeTimeId selected');
      return;
    }

    const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
    const holeScoreData = playerScore?.scores[hole.id];
    const scorecardId = holeScoreData?.scorecardId;

    if (scorecardId && score === null) {
      // Delete scorecard
      this.http.delete(`${environment.apiUrl}/api/player-scorecards/${scorecardId}`).subscribe({
        next: () => {
          // Update local state
          const updatedPlayerScores = this.playerScores().map(ps => {
            if (ps.playerId === playerId) {
              return {
                ...ps,
                scores: {
                  ...ps.scores,
                  [hole.id]: {}
                }
              };
            }
            return ps;
          });
          this.playerScores.set(updatedPlayerScores);
          this.updateTeamScores();
        },
        error: (err) => console.error('Error deleting scorecard:', err)
      });
    } else if (scorecardId) {
      // Update existing
      const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
      const courseHandicap = playerScore?.courseHandicap;
      const netScore = this.calculateNetScore(score, courseHandicap, hole);
      
      // For Stableford, calculate game points
      let gamePoints: number | undefined = undefined;
      if (this.getScoringType() === 'Stableford') {
        gamePoints = this.calculateStablefordGamePoints(netScore, hole);
      }
      
      // Update local state immediately with calculated net score and game points
      const updatedPlayerScoresImmediate = this.playerScores().map(ps => {
        if (ps.playerId === playerId) {
          return {
            ...ps,
            scores: {
              ...ps.scores,
              [hole.id]: { scorecardId: scorecardId, grossScore: score || undefined, netScore: netScore, gamePoints: gamePoints }
            }
          };
        }
        return ps;
      });
      this.playerScores.set(updatedPlayerScoresImmediate);
      this.updateTeamScores();
      
      const scorecard: PlayerScorecard = {
        id: scorecardId,
        roundTeeTime: this.selectedTeeTime()!,
        player: { id: playerId },
        hole: { id: hole.id },
        grossScore: score || undefined,
        netScore: netScore,
        courseHandicap: courseHandicap,
        gamePoints: gamePoints
      };
      this.http.put(`${environment.apiUrl}/api/player-scorecards/${scorecardId}`, scorecard).subscribe({
        next: (updated: any) => {
          const updatedPlayerScores = this.playerScores().map(ps => {
            if (ps.playerId === playerId) {
              return {
                ...ps,
                scores: {
                  ...ps.scores,
                  [hole.id]: { scorecardId: updated.id, grossScore: updated.grossScore, netScore: updated.netScore, gamePoints: updated.gamePoints }
                }
              };
            }
            return ps;
          });
          this.playerScores.set(updatedPlayerScores);
          this.updateTeamScores();
        },
        error: (err) => {
          console.error('Error updating scorecard:', err);
          alert('Failed to save score: ' + (err.error?.message || err.message || 'Unknown error'));
        }
      });
    } else if (score !== null) {
      // Create new
      const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
      const courseHandicap = playerScore?.courseHandicap;
      const netScore = this.calculateNetScore(score, courseHandicap, hole);
      
      // For Stableford, calculate game points immediately
      let gamePoints: number | undefined = undefined;
      if (this.getScoringType() === 'Stableford') {
        gamePoints = this.calculateStablefordGamePoints(netScore, hole);
      }
      
      // Update local state immediately with calculated net score and game points
      const updatedPlayerScoresImmediate = this.playerScores().map(ps => {
        if (ps.playerId === playerId) {
          return {
            ...ps,
            scores: {
              ...ps.scores,
              [hole.id]: { grossScore: score || undefined, netScore: netScore, gamePoints: gamePoints }
            }
          };
        }
        return ps;
      });
      this.playerScores.set(updatedPlayerScoresImmediate);
      this.updateTeamScores();
      
      const scorecard: PlayerScorecard = {
        roundTeeTime: this.selectedTeeTime()!,
        player: { id: playerId },
        hole: { id: hole.id },
        grossScore: score,
        netScore: netScore,
        courseHandicap: courseHandicap,
        gamePoints: gamePoints
      };
      this.http.post(`${environment.apiUrl}/api/player-scorecards`, scorecard).subscribe({
        next: (created: any) => {
          const updatedPlayerScores = this.playerScores().map(ps => {
            if (ps.playerId === playerId) {
              return {
                ...ps,
                scores: {
                  ...ps.scores,
                  [hole.id]: { scorecardId: created.id, grossScore: created.grossScore, netScore: created.netScore, gamePoints: created.gamePoints }
                }
              };
            }
            return ps;
          });
          this.updateTeamScores();
          this.playerScores.set(updatedPlayerScores);
        },
        error: (err) => {
          console.error('Error creating scorecard:', err);
          alert('Failed to save score: ' + (err.error?.message || err.message || 'Unknown error'));
        }
      });
    }
  }

  getPlayerTotal(playerId: number): number {
    const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
    if (!playerScore) return 0;
    return Object.values(playerScore.scores).reduce((total, score) => {
      return total + (score.grossScore || 0);
    }, 0);
  }

  getHoleTotal(hole: Hole): number {
    return this.playerScores().reduce((total, playerScore) => {
      return total + (playerScore.scores[hole.id]?.grossScore || 0);
    }, 0);
  }

  getPlayerNineTotal(playerId: number, startHole: number, endHole: number): number {
    const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
    if (!playerScore) return 0;
    
    return this.holes()
      .filter(h => h.holeNumber >= startHole && h.holeNumber <= endHole)
      .reduce((total, hole) => {
        return total + (playerScore.scores[hole.id]?.grossScore || 0);
      }, 0);
  }

  getPlayerNineNetTotal(playerId: number, startHole: number, endHole: number): number {
    const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
    if (!playerScore) return 0;
    
    return this.holes()
      .filter(h => h.holeNumber >= startHole && h.holeNumber <= endHole)
      .reduce((total, hole) => {
        // Use stored net score from database (already calculated with correct handicap)
        return total + (playerScore.scores[hole.id]?.netScore || 0);
      }, 0);
  }

  getPlayerNetTotal(playerId: number): number {
    const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
    if (!playerScore) return 0;
    return this.holes().reduce((total, hole) => {
      // Use stored net score from database (already calculated with correct handicap)
      return total + (playerScore.scores[hole.id]?.netScore || 0);
    }, 0);
  }

  getPlayerNineGamePoints(playerId: number, startHole: number, endHole: number): number {
    const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
    if (!playerScore) return 0;
    
    return this.holes()
      .filter(h => h.holeNumber >= startHole && h.holeNumber <= endHole)
      .reduce((total, hole) => {
        return total + (playerScore.scores[hole.id]?.gamePoints || 0);
      }, 0);
  }

  getPlayerTotalGamePoints(playerId: number): number {
    const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
    if (!playerScore) return 0;
    return this.holes().reduce((total, hole) => {
      return total + (playerScore.scores[hole.id]?.gamePoints || 0);
    }, 0);
  }

  getHolesTotal(holes: Hole[], property: 'par' | 'yards' | 'handicap'): number {
    return holes.reduce((total, hole) => {
      return total + hole[property];
    }, 0);
  }

  getHoleColumnBackgroundColor(playerId: number, holeId: number, hole: Hole, courseHandicap: number | undefined): string {
    const grossScore = this.getPlayerScore(playerId, holeId);
    if (!grossScore || !courseHandicap || !hole) return 'white';
    
    const netScore = this.calculateNetScore(grossScore, courseHandicap, hole);
    if (!netScore) return 'white';
    
    const difference = netScore - hole.par;
    
    if (difference === 0) {
      return 'white';
    } else if (difference < 0) {
      // Under par - green shades
      if (difference <= -3) {
        return '#4CAF50'; // Dark green for 3+ under
      } else if (difference === -2) {
        return '#81C784'; // Medium green for 2 under
      } else {
        return '#C8E6C9'; // Light green for 1 under
      }
    } else {
      // Over par - red shades
      if (difference >= 3) {
        return '#E57373'; // Dark red for 3+ over
      } else if (difference === 2) {
        return '#EF9A9A'; // Medium red for 2 over
      } else {
        return '#FFCDD2'; // Light red for 1 over
      }
    }
  }

  resetForm() {
    this.selectedTournamentId.set(null);
    this.selectedRoundId.set(null);
    this.selectedTeeTimeId.set(null);
    this.rounds.set([]);
    this.teeTimes.set([]);
    this.holes.set([]);
    this.playerScores.set([]);
    this.teamScores.set([]);
    this.roundTeams.set([]);
  }

  private loadTeams(teeTimeId: number) {
    this.http.get<RoundTeam[]>(`${environment.apiUrl}/api/team-game-points/tee-time/${teeTimeId}`)
      .subscribe({
        next: (teams) => {
          this.roundTeams.set(teams);
          
          // Build team scores structure with all fields
          const teamScoresList: TeamScore[] = [];
          teams.forEach(team => {
            const playerIds: number[] = [];
            if (team.player1Id) playerIds.push(team.player1Id);
            if (team.player2Id) playerIds.push(team.player2Id);
            if (team.player3Id) playerIds.push(team.player3Id);
            
            const scores: { [holeId: number]: { grossScore?: number; netScore?: number; gamePoints?: number } } = {};
            
            this.holes().forEach(hole => {
              scores[hole.id] = {};
            });

            teamScoresList.push({
              teamId: team.id,
              playerIds,
              scores
            });
          });

          this.teamScores.set(teamScoresList);
          
          // Calculate team scores from current player scores
          this.updateTeamScores();
          // Note: loadTeamHoleScores() is now called after player scores load in loadScorecardsWithPlayerDetails()
        },
        error: (err) => {
          this.roundTeams.set([]);
          this.teamScores.set([]);
        }
      });
  }

  private loadTeamHoleScores() {
    const roundTeamIds = this.roundTeams().map(rt => rt.id);
    if (roundTeamIds.length === 0) return;

    // Load team game points from database (only gamePoints, preserve calculated gross/net)
    roundTeamIds.forEach(roundTeamId => {
      this.http.get<any[]>(`${environment.apiUrl}/api/team-game-points/scores/${roundTeamId}`)
        .subscribe({
          next: (teamScores) => {
            // Update team scores with gamePoints from database, preserving calculated gross/net
            const updatedTeamScores = this.teamScores().map(teamScore => {
              if (teamScore.teamId === roundTeamId) {
                const updatedScores = { ...teamScore.scores };
                teamScores.forEach(ts => {
                  updatedScores[ts.hole.id] = {
                    ...updatedScores[ts.hole.id],  // Keep existing gross/net
                    gamePoints: ts.gamePoints      // Add game points from database
                  };
                });
                return { ...teamScore, scores: updatedScores };
              }
              return teamScore;
            });
            this.teamScores.set(updatedTeamScores);
          },
          error: (err) => {
            // Silently fail if no team scores exist yet
          }
        });
    });
  }

  getTeamBestNetScore(teamId: number, holeId: number): number | undefined {
    const teamScore = this.teamScores().find(ts => ts.teamId === teamId);
    return teamScore?.scores[holeId]?.netScore;
  }

  getTeamGrossScore(teamId: number, holeId: number): number | undefined {
    const teamScore = this.teamScores().find(ts => ts.teamId === teamId);
    return teamScore?.scores[holeId]?.grossScore;
  }

  getTeamGamePoints(teamId: number, holeId: number): number | undefined {
    const teamScore = this.teamScores().find(ts => ts.teamId === teamId);
    return teamScore?.scores[holeId]?.gamePoints;
  }

  // Check if team has any score (gross or net) for a hole
  hasTeamScoreForHole(teamId: number, holeId: number): boolean {
    const teamScore = this.teamScores().find(ts => ts.teamId === teamId);
    return !!(teamScore?.scores[holeId]?.grossScore || teamScore?.scores[holeId]?.netScore);
  }

  /**
   * Calculate team scores from player scores
   * For Nines: lowest (best) net score from all players on team
   * For 2-Man Aggregate: sum of all player net scores
   */
  private updateTeamScores() {
    const gameType = this.getGameType();
    const updatedTeamScores = this.teamScores().map(teamScore => {
      const updatedScores: { [holeId: number]: { grossScore?: number; netScore?: number; gamePoints?: number } } = {};
      
      for (const hole of this.holes()) {
        let totalGross = 0;
        let totalNet = 0;
        let bestNet = undefined;
        let hasAnyScore = false;
        
        // Collect scores for all players on this team
        for (const playerId of teamScore.playerIds) {
          const playerScore = this.playerScores().find(ps => ps.playerId === playerId);
          if (playerScore) {
            const holeScore = playerScore.scores[hole.id];
            if (holeScore?.grossScore) {
              totalGross += holeScore.grossScore;
              hasAnyScore = true;
            }
            if (holeScore?.netScore !== undefined) {
              totalNet += holeScore.netScore;
              // Track best (lowest) net score
              if (bestNet === undefined || holeScore.netScore < bestNet) {
                bestNet = holeScore.netScore;
              }
            }
          }
        }
        
        if (hasAnyScore) {
          // Determine team net score based on game type
          // For Best Ball games (Nines, 2-man Best Ball) use lowest net score
          // For Aggregate games use sum of net scores
          let teamNetScore = totalNet; // Default: Aggregate (sum)
          if (gameType === 'Nines' || gameType === '2-man Best Ball') {
            teamNetScore = bestNet !== undefined ? bestNet : totalNet;
          }
          
          // Calculate team game points for Stableford
          let teamGamePoints: number | undefined = undefined;
          if (this.getScoringType() === 'Stableford') {
            teamGamePoints = this.calculateStablefordGamePoints(teamNetScore, hole);
          }
          
          updatedScores[hole.id] = {
            grossScore: totalGross,
            netScore: teamNetScore,
            gamePoints: teamGamePoints
          };
        }
      }
      
      // For Stableford, save team game points to backend
      if (this.getScoringType() === 'Stableford') {
        for (const [holeIdStr, holeScore] of Object.entries(updatedScores)) {
          if (holeScore.gamePoints !== undefined) {
            const holeId = parseInt(holeIdStr, 10);
            this.saveTeamGamePoints(teamScore.teamId, holeId, holeScore.gamePoints);
          }
        }
      }
      
      return { ...teamScore, scores: updatedScores };
    });
    
    this.teamScores.set(updatedTeamScores);
  }

  private saveTeamGamePoints(teamId: number, holeId: number, gamePoints: number) {
    const request = { gamePoints: gamePoints };
    this.http.put(`${environment.apiUrl}/api/team-game-points/save-game-points/${teamId}/${holeId}`, request).subscribe({
      next: (response: any) => {
        console.log(`Saved team game points for team ${teamId}, hole ${holeId}: ${gamePoints}`);
      },
      error: (err) => {
        console.error(`Error saving team game points for team ${teamId}, hole ${holeId}:`, err);
      }
    });
  }

  getTeamTotalNetScore(teamId: number, startHole: number, endHole: number): number {
    let total = 0;
    for (const hole of this.holes()) {
      if (hole.holeNumber >= startHole && hole.holeNumber <= endHole) {
        const netScore = this.getTeamBestNetScore(teamId, hole.id);
        if (netScore !== undefined) {
          total += netScore;
        }
      }
    }
    return total;
  }

  getTeamTotalGrossScore(teamId: number, startHole: number, endHole: number): number {
    let total = 0;
    for (const hole of this.holes()) {
      if (hole.holeNumber >= startHole && hole.holeNumber <= endHole) {
        const grossScore = this.getTeamGrossScore(teamId, hole.id);
        if (grossScore !== undefined) {
          total += grossScore;
        }
      }
    }
    return total;
  }

  getTeamTotalGamePoints(teamId: number): number {
    let total = 0;
    for (const hole of this.holes()) {
      const points = this.getTeamGamePoints(teamId, hole.id);
      if (points !== undefined) {
        total += points;
      }
    }
    return total;
  }

  generateTeamGamePoints(skipConfirm: boolean = false) {
    const roundId = this.selectedRoundId();
    if (!roundId) {
      alert('Please select a round first');
      return;
    }

    const proceed = skipConfirm || confirm('Generate team game points for all teams in this round? All teams must have completed scores for all 18 holes.');
    
    if (proceed) {
      this.http.post(`${environment.apiUrl}/api/team-game-points/calculate-points/${roundId}`, {})
        .subscribe({
          next: (response: any) => {
            if (!skipConfirm) {
              alert('Team game points generated successfully!');
            }
            // Reload both player and team game points from database
            this.refreshGamePointsFromDatabase();
          },
          error: (err) => {
            const errorMessage = err.error?.message || err.error?.error || err.message || 'Unknown error';
            alert('Cannot generate game points:\n\n' + errorMessage);
            console.error('Error:', err);
          }
        });
    }
  }

  /**
   * Refresh both player and team game points from the database
   */
  private refreshGamePointsFromDatabase() {
    const teeTimeId = this.selectedTeeTimeId();
    if (!teeTimeId) return;

    // Reload player scorecards to get updated game points
    this.http.get<PlayerScorecard[]>(`${environment.apiUrl}/api/player-scorecards/by-tee-time/${teeTimeId}`)
      .subscribe({
        next: (scorecards) => {
          // Update player scores with refreshed game points
          const playerScoresMap = new Map<number, PlayerScore>();
          const existingScores = this.playerScores();
          
          // Preserve existing player scores structure
          existingScores.forEach(ps => {
            playerScoresMap.set(ps.playerId, { ...ps });
          });
          
          // Update with fresh scorecard data (especially game points)
          scorecards.forEach(scorecard => {
            const playerId = scorecard.player.id;
            const playerScore = playerScoresMap.get(playerId);
            
            if (playerScore) {
              playerScore.scores[scorecard.hole.id] = {
                scorecardId: scorecard.id,
                grossScore: scorecard.grossScore || undefined,
                netScore: scorecard.netScore || undefined,
                gamePoints: scorecard.gamePoints || undefined
              };
            }
          });
          
          this.playerScores.set(Array.from(playerScoresMap.values()));
          
          // Also reload team game points
          this.loadTeamHoleScores();
        },
        error: (err) => {
          console.error('Error refreshing game points:', err);
        }
      });
  }

  /**
   * Get the team ID that a player belongs to
   */
  getTeamForPlayer(playerId: number): number | null {
    const teams = this.roundTeams();
    for (const team of teams) {
      if (team.player1Id === playerId || team.player2Id === playerId || team.player3Id === playerId) {
        return team.id;
      }
    }
    return null;
  }

  /**
   * Determine if we should show the team row after this player
   * Team row shows after the last player of that team
   */
  shouldShowTeamRowAfterPlayer(playerId: number, playerIndex: number): boolean {
    const players = this.playerScores();
    const teamId = this.getTeamForPlayer(playerId);
    
    if (!teamId) {
      return false;
    }

    // Check if this is the last player in this team
    for (let i = playerIndex + 1; i < players.length; i++) {
      const nextPlayerId = players[i].playerId;
      if (this.getTeamForPlayer(nextPlayerId) === teamId) {
        // Found another player on same team after this one, so don't show row yet
        return false;
      }
    }

    // This is the last player on this team, show the team row
    return true;
  }
}

