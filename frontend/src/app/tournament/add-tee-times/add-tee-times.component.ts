import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

interface Tournament {
  id: number;
  year: number;
  location: string;
}

interface Round {
  id: number;
  tournament: Tournament;
  day: string;
  game: { id: number; name: string };
  course?: { id: number; name: string };
  scoringType?: { id: number; scoringTypeName: string };
  teeTime?: RoundTeeTime;
  inter_group?: boolean;
  split_skins?: boolean;
}

interface RoundTeeTime {
  id: number;
  teeTime: string; // LocalDateTime as ISO string
  tournamentRound: Round;
  player1Id?: number;
  player2Id?: number;
  player3Id?: number;
  player4Id?: number;
  player1Handicap?: number;
  player2Handicap?: number;
  player3Handicap?: number;
  player4Handicap?: number;
}

interface Golfer {
  id: number;
  firstName?: string;
  lastName?: string;
  name: string;
}

interface PlayerSelectionInfo {
  id: number;
  name: string;
  partTime: boolean;
}

@Component({
  selector: 'app-add-tee-times',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-tee-times.component.html',
  styleUrl: './add-tee-times.component.scss'
})
export class AddTeeTimesComponent {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  tournaments = signal<Tournament[]>([]);
  selectedTournamentId = signal<number | null>(null);
  rounds = signal<Round[]>([]);
  selectedRoundId = signal<number | null>(null);
  teeTimes = signal<RoundTeeTime[]>([]);
  golfers = signal<Golfer[]>([]);
  showAddTeeTime = signal(false);
  editingTeeTimeId = signal<number | null>(null);
  tournamentHandicaps = signal<Map<number, number>>(new Map()); // Map of playerId -> handicap
  randomizedTeams = signal<number[][]>([]);
  showRandomizationPanel = signal(false);
  showPlayerSelectionPanel = signal(false);
  isLoadingRandomization = signal(false);
  isLoadingPlayerSelection = signal(false);
  playerList = signal<PlayerSelectionInfo[]>([]);
  selectedPlayers = signal<Set<number>>(new Set());
  addTeeTimeForm: FormGroup;
  editTeeTimeForm: FormGroup;

  constructor() {
    this.addTeeTimeForm = this.fb.group({
      teeTime: ['', Validators.required],
      golfer1: [''],
      golfer2: [''],
      golfer3: [''],
      golfer4: [''],
      handicap1: [''],
      handicap2: [''],
      handicap3: [''],
      handicap4: ['']
    });
    this.editTeeTimeForm = this.fb.group({
      teeTime: ['', Validators.required],
      golfer1: [''],
      golfer2: [''],
      golfer3: [''],
      golfer4: [''],
      handicap1: [''],
      handicap2: [''],
      handicap3: [''],
      handicap4: ['']
    });
    this.loadTournaments();
  }

  loadTournaments() {
    this.http.get<Tournament[]>('http://localhost:8080/api/tournaments').subscribe({
      next: (data) => this.tournaments.set(data),
      error: () => alert('Error loading tournaments')
    });
  }

  loadPlayersForTournament(tournamentId: number) {
    this.http.get<Golfer[]>(`http://localhost:8080/api/tournaments/${tournamentId}/players`).subscribe({
      next: (data) => {
        this.golfers.set(data);
        this.loadTournamentHandicaps(tournamentId);
      },
      error: () => {
        console.error('Error loading players for tournament');
        this.golfers.set([]);
      }
    });
  }

  loadTournamentHandicaps(tournamentId: number) {
    this.http.get<any[]>(`http://localhost:8080/api/tournament-handicaps?tournamentId=${tournamentId}`).subscribe({
      next: (data) => {
        const handicapMap = new Map<number, number>();
        data.forEach(h => {
          handicapMap.set(h.player?.id, h.handicap);
        });
        this.tournamentHandicaps.set(handicapMap);
      },
      error: () => {
        console.error('Error loading tournament handicaps');
        this.tournamentHandicaps.set(new Map());
      }
    });
  }

  onTournamentChange(tournamentId: number) {
    this.selectedTournamentId.set(tournamentId);
    this.selectedRoundId.set(null);
    this.rounds.set([]);
    this.teeTimes.set([]);
    this.showAddTeeTime.set(false);
    
    if (tournamentId) {
      this.loadPlayersForTournament(tournamentId);
      this.loadRoundsForTournament(tournamentId);
    }
  }

  loadRoundsForTournament(tournamentId: number) {
    this.http.get<Round[]>(`http://localhost:8080/api/tournament-rounds?tournamentId=${tournamentId}`).subscribe({
      next: (data) => this.rounds.set(data),
      error: () => alert('Error loading rounds')
    });
  }

  onRoundChange(roundId: number) {
    this.selectedRoundId.set(roundId);
    this.teeTimes.set([]);
    this.showAddTeeTime.set(false);
    this.addTeeTimeForm.reset();
    
    if (roundId) {
      this.loadTeeTimesForRound(roundId);
    }
  }

  loadTeeTimesForRound(roundId: number) {
    this.http.get<RoundTeeTime[]>(`http://localhost:8080/api/round-tee-times?roundId=${roundId}`).subscribe({
      next: (data) => this.teeTimes.set(data),
      error: () => alert('Error loading tee times')
    });
  }

  toggleAddTeeTime() {
    this.showAddTeeTime.set(!this.showAddTeeTime());
    if (!this.showAddTeeTime()) {
      this.addTeeTimeForm.reset();
    }
  }

  onAddTeeTimeSubmit() {
    if (this.addTeeTimeForm.valid && this.selectedRoundId()) {
      const formValue = this.addTeeTimeForm.value;
      const round = this.rounds().find(r => r.id === this.selectedRoundId());
      
      if (!round) {
        alert('Round not found');
        return;
      }

      // Combine round day with time to create full LocalDateTime
      // Format as "YYYY-MM-DDTHH:MM:SS" without timezone info
      const [hours, minutes] = formValue.teeTime.split(':');
      const day = round.day.split('T')[0]; // Extract just the date part
      const localDateTime = `${day}T${hours}:${minutes}:00`;

      const teeTimeData = {
        teeTime: localDateTime,
        tournamentRoundId: this.selectedRoundId(),
        player1Id: formValue.golfer1 ? parseInt(formValue.golfer1, 10) : null,
        player2Id: formValue.golfer2 ? parseInt(formValue.golfer2, 10) : null,
        player3Id: formValue.golfer3 ? parseInt(formValue.golfer3, 10) : null,
        player4Id: formValue.golfer4 ? parseInt(formValue.golfer4, 10) : null,
        player1Handicap: formValue.handicap1 ? parseFloat(formValue.handicap1) : null,
        player2Handicap: formValue.handicap2 ? parseFloat(formValue.handicap2) : null,
        player3Handicap: formValue.handicap3 ? parseFloat(formValue.handicap3) : null,
        player4Handicap: formValue.handicap4 ? parseFloat(formValue.handicap4) : null
      };

      this.http.post('http://localhost:8080/api/round-tee-times', teeTimeData).subscribe({
        next: () => {
          this.addTeeTimeForm.reset();
          this.showAddTeeTime.set(false);
          if (this.selectedRoundId()) {
            this.loadTeeTimesForRound(this.selectedRoundId()!);
          }
        },
        error: (err) => {
          console.error('Error adding tee time:', err);
          alert('Error adding tee time: ' + err.error?.message || err.statusText);
        }
      });
    }
  }

  deleteTeeTime(teeTimeId: number) {
    if (confirm('Are you sure you want to delete this tee time?')) {
      this.http.delete(`http://localhost:8080/api/round-tee-times/${teeTimeId}`).subscribe({
        next: () => {
          if (this.selectedRoundId()) {
            this.loadTeeTimesForRound(this.selectedRoundId()!);
          }
        },
        error: () => alert('Error deleting tee time')
      });
    }
  }

  getGolferName(golferId: number | undefined): string {
    if (!golferId) return '-';
    const golfer = this.golfers().find(g => g.id === golferId);
    return golfer?.name || '-';
  }

  getGolferAtPosition(teeTime: RoundTeeTime, position: number): string {
    let playerId: number | undefined;
    
    switch (position) {
      case 1:
        playerId = teeTime.player1Id;
        break;
      case 2:
        playerId = teeTime.player2Id;
        break;
      case 3:
        playerId = teeTime.player3Id;
        break;
      case 4:
        playerId = teeTime.player4Id;
        break;
    }

    if (!playerId) return '-';
    const golfer = this.golfers().find(g => g.id === playerId);
    return golfer?.name || '-';
  }

  getAvailableGolfers(currentFieldValue?: number | string): Golfer[] {
    const selectedGolferIds = new Set<number>();
    
    // Convert currentFieldValue to number
    const currentNumValue = currentFieldValue ? (typeof currentFieldValue === 'string' ? parseInt(currentFieldValue, 10) : currentFieldValue) : null;
    
    // Collect all player IDs already selected in other tee times
    this.teeTimes().forEach(teeTime => {
      if (teeTime.player1Id) selectedGolferIds.add(teeTime.player1Id);
      if (teeTime.player2Id) selectedGolferIds.add(teeTime.player2Id);
      if (teeTime.player3Id) selectedGolferIds.add(teeTime.player3Id);
      if (teeTime.player4Id) selectedGolferIds.add(teeTime.player4Id);
    });
    
    // Collect golfers selected in OTHER fields of this form row
    const golfer1 = this.addTeeTimeForm.get('golfer1')?.value;
    const golfer2 = this.addTeeTimeForm.get('golfer2')?.value;
    const golfer3 = this.addTeeTimeForm.get('golfer3')?.value;
    const golfer4 = this.addTeeTimeForm.get('golfer4')?.value;
    
    [golfer1, golfer2, golfer3, golfer4].forEach((id: any) => {
      if (id) {
        const numId = typeof id === 'string' ? parseInt(id, 10) : id;
        if (numId && numId !== currentNumValue) {
          selectedGolferIds.add(numId);
        }
      }
    });
    
    // Filter golfers not in selectedGolferIds, or the current field's value
    return this.golfers().filter(g => !selectedGolferIds.has(g.id) || g.id === currentNumValue);
  }

  toggleEditTeeTime(teeTime: RoundTeeTime) {
    if (this.editingTeeTimeId() === teeTime.id) {
      this.editingTeeTimeId.set(null);
    } else {
      this.editingTeeTimeId.set(teeTime.id);
      // Populate the edit form with current values
      this.editTeeTimeForm.patchValue({
        teeTime: teeTime.teeTime ? new Date(teeTime.teeTime).toTimeString().slice(0, 5) : '',
        golfer1: teeTime.player1Id || '',
        golfer2: teeTime.player2Id || '',
        golfer3: teeTime.player3Id || '',
        golfer4: teeTime.player4Id || '',
        handicap1: teeTime.player1Handicap || '',
        handicap2: teeTime.player2Handicap || '',
        handicap3: teeTime.player3Handicap || '',
        handicap4: teeTime.player4Handicap || ''
      });
    }
  }

  saveEditedTeeTime(teeTime: RoundTeeTime) {
    if (!this.editTeeTimeForm.valid) {
      alert('Please fill in required fields');
      return;
    }

    const formValue = this.editTeeTimeForm.value;
    const round = this.rounds().find(r => r.id === this.selectedRoundId());
    
    if (!round) {
      alert('Round not found');
      return;
    }

    // Combine round day with time to create full LocalDateTime
    // Format as "YYYY-MM-DDTHH:MM:SS" without timezone info
    const [hours, minutes] = formValue.teeTime.split(':');
    const day = round.day.split('T')[0]; // Extract just the date part
    const localDateTime = `${day}T${hours}:${minutes}:00`;

    const updatedTeeTime = {
      id: teeTime.id,
      teeTime: localDateTime,
      tournamentRoundId: this.selectedRoundId(),
      player1Id: formValue.golfer1 ? parseInt(formValue.golfer1, 10) : null,
      player2Id: formValue.golfer2 ? parseInt(formValue.golfer2, 10) : null,
      player3Id: formValue.golfer3 ? parseInt(formValue.golfer3, 10) : null,
      player4Id: formValue.golfer4 ? parseInt(formValue.golfer4, 10) : null,
      player1Handicap: formValue.handicap1 ? parseFloat(formValue.handicap1) : null,
      player2Handicap: formValue.handicap2 ? parseFloat(formValue.handicap2) : null,
      player3Handicap: formValue.handicap3 ? parseFloat(formValue.handicap3) : null,
      player4Handicap: formValue.handicap4 ? parseFloat(formValue.handicap4) : null
    };

    this.http.put(`http://localhost:8080/api/round-tee-times/${teeTime.id}`, updatedTeeTime).subscribe({
      next: () => {
        this.editingTeeTimeId.set(null);
        if (this.selectedRoundId()) {
          this.loadTeeTimesForRound(this.selectedRoundId()!);
        }
      },
      error: (err) => {
        console.error('Error saving tee time:', err);
        alert('Error saving tee time: ' + err.error?.message || err.statusText);
      }
    });
  }

  cancelEditTeeTime() {
    this.editingTeeTimeId.set(null);
    this.editTeeTimeForm.reset();
  }

  getAvailableGolfersForEdit(currentFieldValue?: number | string): Golfer[] {
    const selectedGolferIds = new Set<number>();
    
    // Convert currentFieldValue to number
    const currentNumValue = currentFieldValue ? (typeof currentFieldValue === 'string' ? parseInt(currentFieldValue, 10) : currentFieldValue) : null;
    
    // Collect all player IDs already selected in other tee times (excluding the one being edited)
    this.teeTimes().forEach(teeTime => {
      if (teeTime.id !== this.editingTeeTimeId()) {
        if (teeTime.player1Id) selectedGolferIds.add(teeTime.player1Id);
        if (teeTime.player2Id) selectedGolferIds.add(teeTime.player2Id);
        if (teeTime.player3Id) selectedGolferIds.add(teeTime.player3Id);
        if (teeTime.player4Id) selectedGolferIds.add(teeTime.player4Id);
      }
    });
    
    // Collect golfers selected in OTHER fields of the edit form row
    const golfer1 = this.editTeeTimeForm.get('golfer1')?.value;
    const golfer2 = this.editTeeTimeForm.get('golfer2')?.value;
    const golfer3 = this.editTeeTimeForm.get('golfer3')?.value;
    const golfer4 = this.editTeeTimeForm.get('golfer4')?.value;
    
    [golfer1, golfer2, golfer3, golfer4].forEach((id: any) => {
      if (id) {
        const numId = typeof id === 'string' ? parseInt(id, 10) : id;
        if (numId && numId !== currentNumValue) {
          selectedGolferIds.add(numId);
        }
      }
    });
    
    // Filter golfers not in selectedGolferIds, or the current field's value
    return this.golfers().filter(g => !selectedGolferIds.has(g.id) || g.id === currentNumValue);
  }

  getTournamentHandicap(playerId: number | string | null): number | null {
    if (!playerId) return null;
    const numId = typeof playerId === 'string' ? parseInt(playerId, 10) : playerId;
    return this.tournamentHandicaps().get(numId) || null;
  }

  onGolferSelected(position: number, golferId: number | string | null) {
    const handicapFieldName = `handicap${position}`;
    if (golferId) {
      const numId = typeof golferId === 'string' ? parseInt(golferId, 10) : golferId;
      const handicap = this.getTournamentHandicap(numId);
      if (handicap !== null) {
        this.addTeeTimeForm.get(handicapFieldName)?.setValue(handicap.toString());
      }
    }
  }

  onGolferSelectedEdit(position: number, golferId: number | string | null) {
    const handicapFieldName = `handicap${position}`;
    if (golferId) {
      const numId = typeof golferId === 'string' ? parseInt(golferId, 10) : golferId;
      const handicap = this.getTournamentHandicap(numId);
      if (handicap !== null) {
        this.editTeeTimeForm.get(handicapFieldName)?.setValue(handicap.toString());
      }
    }
  }

  randomizeTeams() {
    if (!this.selectedRoundId()) {
      alert('Please select a round first');
      return;
    }

    this.isLoadingPlayerSelection.set(true);
    this.http.get<PlayerSelectionInfo[]>(`http://localhost:8080/api/round-tee-times/player-selection/${this.selectedRoundId()}`).subscribe({
      next: (players) => {
        this.playerList.set(players);
        
        // Pre-select full-time players
        const selected = new Set<number>();
        players.forEach(p => {
          if (!p.partTime) {
            selected.add(p.id);
          }
        });
        this.selectedPlayers.set(selected);
        
        this.showPlayerSelectionPanel.set(true);
        this.isLoadingPlayerSelection.set(false);
      },
      error: (err) => {
        console.error('Error loading players:', err);
        alert('Error loading players: ' + err.error?.message || err.statusText);
        this.isLoadingPlayerSelection.set(false);
      }
    });
  }

  togglePlayerSelection(playerId: number) {
    const selected = new Set(this.selectedPlayers());
    if (selected.has(playerId)) {
      selected.delete(playerId);
    } else {
      selected.add(playerId);
    }
    this.selectedPlayers.set(selected);
  }

  isPlayerSelected(playerId: number): boolean {
    return this.selectedPlayers().has(playerId);
  }

  startRandomization() {
    const selectedIds = Array.from(this.selectedPlayers());
    if (selectedIds.length === 0) {
      alert('Please select at least one player');
      return;
    }

    this.isLoadingRandomization.set(true);
    this.http.post<any[]>(
      `http://localhost:8080/api/round-tee-times/randomize-selection/${this.selectedRoundId()}`,
      selectedIds
    ).subscribe({
      next: (suggestions) => {
        // Transform TeamSuggestion objects to number[][] format
        const teams = suggestions.map(s => s.playerIds);
        this.randomizedTeams.set(teams);
        this.showRandomizationPanel.set(true);
        this.showPlayerSelectionPanel.set(false);
        this.isLoadingRandomization.set(false);
      },
      error: (err) => {
        console.error('Error randomizing teams:', err);
        let errorMsg = 'Error randomizing teams';
        if (err.error?.error) {
          errorMsg += ': ' + err.error.error;
        } else if (err.error?.message) {
          errorMsg += ': ' + err.error.message;
        } else if (err.statusText) {
          errorMsg += ': ' + err.statusText;
        }
        alert(errorMsg);
        this.isLoadingRandomization.set(false);
      }
    });
  }

  cancelPlayerSelection() {
    this.showPlayerSelectionPanel.set(false);
    this.playerList.set([]);
    this.selectedPlayers.set(new Set());
  }

  getGolferNameById(golferId: number | null): string {
    if (!golferId) return '-';
    const golfer = this.golfers().find(g => g.id === golferId);
    return golfer?.name || '-';
  }

  applyRandomizedTeams() {
    // Clear existing tee times
    const teeTimesToDelete = this.teeTimes().map(tt => tt.id);
    let deleteCount = 0;

    const deleteAll = () => {
      if (deleteCount >= teeTimesToDelete.length) {
        this.createTeesFromRandomized();
      } else {
        this.http.delete(`http://localhost:8080/api/round-tee-times/${teeTimesToDelete[deleteCount]}`).subscribe({
          next: () => {
            deleteCount++;
            deleteAll();
          },
          error: () => {
            deleteCount++;
            deleteAll();
          }
        });
      }
    };

    if (teeTimesToDelete.length > 0) {
      deleteAll();
    } else {
      this.createTeesFromRandomized();
    }
  }

  private createTeesFromRandomized() {
    const round = this.rounds().find(r => r.id === this.selectedRoundId());
    if (!round) {
      alert('Round not found');
      return;
    }

    // Create new tee times from randomized teams
    let createCount = 0;
    const teams = this.randomizedTeams();
    const baseTime = new Date(round.day);

    const createNextTeeTime = () => {
      if (createCount >= teams.length) {
        // All done
        this.showRandomizationPanel.set(false);
        this.randomizedTeams.set([]);
        if (this.selectedRoundId()) {
          this.loadTeeTimesForRound(this.selectedRoundId()!);
        }
        return;
      }

      const team = teams[createCount];
      const teeTimeData = {
        teeTime: baseTime.toISOString().slice(0, 16) + ':00', // YYYY-MM-DDTHH:MM:SS
        tournamentRoundId: this.selectedRoundId(),
        player1Id: team[0] || null,
        player2Id: team[1] || null,
        player3Id: team[2] || null,
        player4Id: team[3] || null,
        player1Handicap: null,
        player2Handicap: null,
        player3Handicap: null,
        player4Handicap: null
      };

      this.http.post('http://localhost:8080/api/round-tee-times', teeTimeData).subscribe({
        next: () => {
          createCount++;
          createNextTeeTime();
        },
        error: (err) => {
          console.error('Error creating tee time:', err);
          createCount++;
          createNextTeeTime();
        }
      });
    };

    createNextTeeTime();
  }

  cancelRandomization() {
    this.showRandomizationPanel.set(false);
    this.randomizedTeams.set([]);
  }
}