import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface HandicapCalculatorDto {
  playerId: number;
  playerName: string;
  handicap: number | null;
  roundsPlayed: number;
  holesPlayed: number;
  eligible: boolean;
  roundDifferentials: RoundDifferentialDto[];
}

interface RoundDifferentialDto {
  roundTeeTimeId: number;
  roundDate: string;
  courseName: string;
  courseRating: number;
  slopeRating: number;
  grossScore: number;
  scoreDifferential: number;
  holesPlayed: number;
  isUsed: boolean;
}

interface Player {
  id: number;
  firstName: string;
  lastName: string;
}

@Component({
  selector: 'app-handicap-calculator',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './handicap-calculator.component.html',
  styleUrl: './handicap-calculator.component.scss'
})
export class HandicapCalculatorComponent {
  private http = inject(HttpClient);

  players = signal<Player[]>([]);
  selectedPlayerId = signal<number | null>(null);
  handicapResult = signal<HandicapCalculatorDto | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  constructor() {
    this.loadPlayers();
  }

  loadPlayers() {
    this.http.get<Player[]>(`${environment.apiUrl}/api/players`).subscribe({
      next: (data) => this.players.set(data),
      error: () => {
        this.error.set('Error loading players');
      }
    });
  }

  onPlayerChange(event: Event) {
    const target = event.target as HTMLSelectElement;
    this.selectedPlayerId.set(+target.value || null);
    this.handicapResult.set(null);
    this.error.set(null);
  }

  calculateHandicap() {
    const playerId = this.selectedPlayerId();
    if (!playerId) {
      this.error.set('Please select a player');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.handicapResult.set(null);

    this.http.get<HandicapCalculatorDto>(`${environment.apiUrl}/api/handicap/player/${playerId}`).subscribe({
      next: (data) => {
        this.handicapResult.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Error calculating handicap: ' + err.message);
        this.loading.set(false);
      }
    });
  }

  getSelectedPlayerName(): string {
    if (!this.selectedPlayerId()) return '';
    const player = this.players().find(p => p.id === this.selectedPlayerId());
    return player ? `${player.firstName} ${player.lastName}` : '';
  }
}
