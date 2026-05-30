import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { environment } from '../../../environments/environment';

interface Player {
  id: number;
  firstName: string;
  lastName: string;
}

interface Tournament {
  id: number;
  year: number;
  location: string;
}

interface TournamentPlayer {
  id: number;
  player: Player;
  handicap: number;
  partTime: boolean;
}

@Component({
  selector: 'app-add-players',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './add-players.component.html',
  styleUrl: './add-players.component.scss'
})
export class AddPlayersComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);

  players = signal<Player[]>([]);
  tournaments = signal<Tournament[]>([]);
  tournamentPlayers = signal<TournamentPlayer[]>([]);

  selectedTournamentId = signal<number | null>(null);
  showAddPlayerForm = signal(false);
  editingPlayerId = signal<number | null>(null);

  newPlayerForm: FormGroup = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    handicap: [0, Validators.required],
    partTime: [false]
  });

  selectedPlayerId = signal<number | null>(null);
  addExistingForm: FormGroup = this.fb.group({
    playerId: ['', Validators.required],
    handicap: [0, Validators.required],
    partTime: [false]
  });

  editPlayerForm: FormGroup = this.fb.group({
    handicap: [0, Validators.required],
    partTime: [false]
  });

  constructor() {
    this.loadPlayers();
    this.loadTournaments();
  }

  loadPlayers() {
    this.http.get<Player[]>(`${environment.apiUrl}/api/players`).subscribe({
      next: (data) => this.players.set(data),
      error: () => alert('Error loading players')
    });
  }

  loadTournaments() {
    this.http.get<Tournament[]>(`${environment.apiUrl}/api/tournaments`).subscribe({
      next: (data) => this.tournaments.set(data),
      error: () => alert('Error loading tournaments')
    });
  }

  loadTournamentPlayers() {
    if (this.selectedTournamentId()) {
      this.http.get<TournamentPlayer[]>(`${environment.apiUrl}/api/tournament-handicaps?tournamentId=${this.selectedTournamentId()}`).subscribe({
        next: (data) => this.tournamentPlayers.set(data),
        error: (error) => {
          console.error('Error loading tournament players:', error);
          this.tournamentPlayers.set([]);
        }
      });
    }
  }

  getAvailablePlayers() {
    const tournamentPlayerIds = this.tournamentPlayers().map(tp => tp.player.id);
    return this.players().filter(p => !tournamentPlayerIds.includes(p.id));
  }

  createPlayer() {
    if (this.newPlayerForm.valid && this.selectedTournamentId()) {
      const playerData = this.newPlayerForm.value;
      this.http.post(`${environment.apiUrl}/api/players`, {
        firstName: playerData.firstName,
        lastName: playerData.lastName
      }).subscribe({
        next: (player: any) => {
          this.addPlayerToTournament(player.id, playerData.handicap, playerData.partTime);
          this.newPlayerForm.reset();
          this.loadPlayers();
          this.loadTournamentPlayers();
        },
        error: () => alert('Error creating player')
      });
    }
  }

  addExistingPlayer() {
    if (this.addExistingForm.valid && this.selectedTournamentId()) {
      const formValue = this.addExistingForm.value;
      this.addPlayerToTournament(formValue.playerId, formValue.handicap, formValue.partTime);
    }
  }

  onTournamentChange(event: Event) {
    const target = event.target as HTMLSelectElement;
    this.selectedTournamentId.set(+target.value);
    this.selectedPlayerId.set(null);
    this.addExistingForm.reset();
    this.loadTournamentPlayers();
  }

  toggleAddPlayerForm() {
    this.showAddPlayerForm.update(value => !value);
    if (!this.showAddPlayerForm()) {
      this.newPlayerForm.reset();
    }
  }

  deletePlayerFromTournament(tournamentPlayerId: number) {
    if (confirm('Are you sure you want to remove this player from the tournament?')) {
      this.http.delete(`${environment.apiUrl}/api/tournament-handicaps/${tournamentPlayerId}`).subscribe({
        next: () => {
          alert('Player removed from tournament');
          this.loadTournamentPlayers();
        },
        error: () => alert('Error removing player from tournament')
      });
    }
  }

  toggleEditPlayer(tournamentPlayer: TournamentPlayer) {
    if (this.editingPlayerId() === tournamentPlayer.id) {
      this.editingPlayerId.set(null);
    } else {
      this.editingPlayerId.set(tournamentPlayer.id);
      this.editPlayerForm.patchValue({
        handicap: tournamentPlayer.handicap,
        partTime: tournamentPlayer.partTime
      });
    }
  }

  saveEditedPlayer(tournamentPlayer: TournamentPlayer) {
    if (this.editPlayerForm.valid) {
      const formValue = this.editPlayerForm.value;
      this.http.put(`${environment.apiUrl}/api/tournament-handicaps/${tournamentPlayer.id}`, {
        handicap: formValue.handicap,
        partTime: formValue.partTime
      }).subscribe({
        next: () => {
          alert('Player updated');
          this.editingPlayerId.set(null);
          this.loadTournamentPlayers();
        },
        error: () => alert('Error updating player')
      });
    }
  }

  cancelEditPlayer() {
    this.editingPlayerId.set(null);
    this.editPlayerForm.reset();
  }

  private addPlayerToTournament(playerId: number, handicap: number, partTime: boolean) {
    this.http.post(`${environment.apiUrl}/api/tournament-handicaps`, {
      player: { id: playerId },
      tournament: { id: this.selectedTournamentId() },
      handicap,
      partTime
    }).subscribe({
      next: () => {
        alert('Player added to tournament');
        this.addExistingForm.reset();
        this.loadTournamentPlayers();
      },
      error: () => alert('Error adding player to tournament')
    });
  }
}
