import { Component, inject, signal, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

interface Tournament {
  id: number;
  year: number;
  location: string;
}

interface Round {
  id: number;
  tournament: Tournament;
  day: string;
  game: Game;
  course?: Course;
  scoringType?: ScoringType;
  teeTime?: RoundTeeTime;
  inter_group?: boolean;
  split_skins?: boolean;
  vs_group?: boolean;
}

interface RoundTeeTime {
  id: number;
  teeTime: string; // LocalDateTime as ISO string
  tournamentRound: Round;
  // Add any additional fields from backend entity if present in DB
  // Example: status, notes, createdAt, updatedAt, etc.
  // Uncomment and adjust as needed:
  // status?: string;
  // notes?: string;
  // createdAt?: string;
  // updatedAt?: string;
}

interface Game {
  id: number;
  name: string;
}

interface Course {
  id: number;
  name: string;
}

interface ScoringType {
  id: number;
  scoringTypeName: string;
}

@Component({
  selector: 'app-create-tournament',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './create-tournament.component.html',
  styleUrl: './create-tournament.component.scss'
})
export class CreateTournamentComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  tournaments = signal<Tournament[]>([]);
  games = signal<Game[]>([]);
  courses = signal<Course[]>([]);
  scoringTypes = signal<ScoringType[]>([]);
  rounds = signal<Map<number, Round[]>>(new Map());
  expandedTournament = signal<number | null>(null);
  showAddRounds = signal<number | null>(null);
  selectedCourseForRound = signal<{ roundId: number; courseId: number } | null>(null);
  editingRound = signal<number | null>(null);

  tournamentForm: FormGroup = this.fb.group({
    year: ['', Validators.required],
    location: ['', Validators.required]
  });

  addRoundForm: FormGroup = this.fb.group({
    date: ['', Validators.required],
    game: ['', Validators.required],
    course: ['', Validators.required],
    scoringType: ['', Validators.required],
    vsGroup: [false]
  });

  constructor() {
    this.loadTournaments();
    this.loadGames();
    this.loadCourses();
    this.loadScoringTypes();
    
    // Listen to game changes in the add round form
    this.addRoundForm.get('game')?.valueChanges.subscribe(gameName => {
      // Find the game by name and check if its ID is 3
      const selectedGame = this.games().find(g => g.name === gameName);
      if (selectedGame && selectedGame.id === 3) {
        // Set scoring type to 4 when game 3 is selected
        this.addRoundForm.patchValue({ scoringType: 4 });
      }
    });
  }

  loadTournaments() {
    this.http.get<Tournament[]>('http://localhost:8080/api/tournaments').subscribe({
      next: (data) => {
        const sorted = data.sort((a, b) => a.year - b.year);
        this.tournaments.set(sorted);
      },
      error: () => alert('Error loading tournaments')
    });
  }

  loadGames() {
    this.http.get<Game[]>('http://localhost:8080/api/games').subscribe({
      next: (data) => {
        console.log('Games loaded:', data);
        console.log('Games length:', data.length);
        if (data.length > 0) {
          console.log('First game:', data[0]);
          console.log('First game name:', data[0].name);
        }
        this.games.set(data);
      },
      error: (error) => {
        console.error('Error loading games:', error);
        this.games.set([]);
      }
    });
  }

  loadCourses() {
    this.http.get<Course[]>('http://localhost:8080/api/courses').subscribe({
      next: (data) => this.courses.set(data),
      error: () => alert('Error loading courses')
    });
  }

  loadScoringTypes() {
    this.http.get<ScoringType[]>('http://localhost:8080/api/scoring-types').subscribe({
      next: (data) => {
        console.log('Scoring types loaded:', data);
        this.scoringTypes.set(data);
      },
      error: (error) => {
        console.error('Error loading scoring types:', error);
        alert('Error loading scoring types');
      }
    });
  }

  loadRoundsForTournament(tournamentId: number) {
    if (!this.rounds().has(tournamentId)) {
      this.http.get<Round[]>(`http://localhost:8080/api/tournament-rounds?tournamentId=${tournamentId}`).subscribe({
        next: (data) => {
          const roundsMap = new Map(this.rounds());
          roundsMap.set(tournamentId, data);
          this.rounds.set(roundsMap);
        },
        error: () => {
          const roundsMap = new Map(this.rounds());
          roundsMap.set(tournamentId, []);
          this.rounds.set(roundsMap);
        }
      });
    }
  }

  toggleTournamentExpand(tournamentId: number) {
    if (this.expandedTournament() === tournamentId) {
      this.expandedTournament.set(null);
    } else {
      this.expandedTournament.set(tournamentId);
      this.loadRoundsForTournament(tournamentId);
    }
  }

  toggleAddRoundsForm(tournamentId: number) {
    if (this.showAddRounds() === tournamentId) {
      this.showAddRounds.set(null);
      this.addRoundForm.reset();
    } else {
      this.showAddRounds.set(tournamentId);
    }
  }

  onAddTournamentSubmit() {
    if (this.tournamentForm.valid) {
      this.http.post('http://localhost:8080/api/tournaments', this.tournamentForm.value)
        .subscribe({
          next: (newTournament: any) => {
            alert('Tournament created successfully');
            this.tournamentForm.reset();
            this.loadTournaments();
            this.showAddRounds.set(newTournament.id);
          },
          error: () => alert('Error creating tournament')
        });
    }
  }

  onAddRoundSubmit(tournamentId: number) {
    if (this.addRoundForm.valid) {
      const formValue = this.addRoundForm.value;
      const selectedGame = this.games().find(g => g.name === formValue.game);
      const selectedCourse = this.courses().find(c => c.id === parseInt(formValue.course));
      const selectedScoringType = this.scoringTypes().find(st => st.id === parseInt(formValue.scoringType));
      const roundData = {
        day: formValue.date,
        game: selectedGame ? { id: selectedGame.id } : null,
        tournament: { id: tournamentId },
        course: selectedCourse ? { id: selectedCourse.id } : null,
        scoringType: selectedScoringType ? { id: selectedScoringType.id } : null,
        vs_group: formValue.vsGroup || false
      };
      this.http.post('http://localhost:8080/api/tournament-rounds', roundData)
        .subscribe({
          next: () => {
            alert('Round added successfully');
            this.addRoundForm.reset();
            const roundsMap = new Map(this.rounds());
            roundsMap.delete(tournamentId);
            this.rounds.set(roundsMap);
            this.loadRoundsForTournament(tournamentId);
          },
          error: () => alert('Error adding round')
        });
    }
  }

  deleteRound(roundId: number, tournamentId: number) {
    if (confirm('Are you sure you want to delete this round?')) {
      this.http.delete(`http://localhost:8080/api/tournament-rounds/${roundId}`).subscribe({
        next: () => {
          const roundsMap = new Map(this.rounds());
          roundsMap.delete(tournamentId);
          this.rounds.set(roundsMap);
          this.loadRoundsForTournament(tournamentId);
        },
        error: () => alert('Error deleting round')
      });
    }
  }

  selectCourseForRound(roundId: number, courseId: number) {
    if (!courseId) {
      this.selectedCourseForRound.set(null);
      return;
    }
    this.selectedCourseForRound.set({ roundId, courseId });
  }

  updateRoundCourse(tournamentId: number) {
    const selection = this.selectedCourseForRound();
    if (!selection) {
      alert('Please select a course');
      return;
    }

    const { roundId, courseId } = selection;
    const selectedCourse = this.courses().find(c => c.id === courseId);
    if (!selectedCourse) {
      alert('Course not found');
      return;
    }

    // Update the tournament round with the course_id
    const roundUpdateData = {
      course: { id: courseId }
    };

    this.http.patch(`http://localhost:8080/api/tournament-rounds/${roundId}`, roundUpdateData).subscribe({
      next: () => {
        this.selectedCourseForRound.set(null);
        const roundsMap = new Map(this.rounds());
        roundsMap.delete(tournamentId);
        this.rounds.set(roundsMap);
        this.loadRoundsForTournament(tournamentId);
      },
      error: () => alert('Error updating round with course')
    });
  }

  addCourseToRound(roundId: number, tournamentId: number, courseId: number) {
    this.selectCourseForRound(roundId, courseId);
  }

  updateRoundData(
    roundId: number,
    tournamentId: number,
    gameId: number,
    courseId: number,
    scoringTypeId: number,
    splitSkins: boolean,
    vsGroup: boolean = false
  ) {
    const selectedGame = this.games().find(g => g.id === gameId);
    const selectedCourse = this.courses().find(c => c.id === courseId);
    const selectedScoringType = this.scoringTypes().find(st => st.id === scoringTypeId);

    const roundUpdateData = {
      game: selectedGame ? { id: selectedGame.id } : null,
      course: selectedCourse ? { id: selectedCourse.id } : null,
      scoringType: selectedScoringType ? { id: selectedScoringType.id } : null,
      inter_group: true,
      split_skins: splitSkins,
      vs_group: vsGroup
    };

    this.http.patch(`http://localhost:8080/api/tournament-rounds/${roundId}`, roundUpdateData).subscribe({
      next: () => {
        this.editingRound.set(null);
        const roundsMap = new Map(this.rounds());
        roundsMap.delete(tournamentId);
        this.rounds.set(roundsMap);
        this.loadRoundsForTournament(tournamentId);
      },
      error: () => alert('Error updating round')
    });
  }

  editTournament(tournament: Tournament) {
    alert('Edit functionality coming soon');
  }

  getJuneFirst(year: number): string {
    const date = new Date(year, 5, 1); // Month is 0-indexed, so 5 = June
    return date.toISOString().split('T')[0];
  }

  getJuneThirtieth(year: number): string {
    const date = new Date(year, 5, 30); // Month is 0-indexed, so 5 = June
    return date.toISOString().split('T')[0];
  }

  shouldDisableScoringType(gameId: number | string): boolean {
    const id = typeof gameId === 'string' ? parseInt(gameId, 10) : gameId;
    return id === 3; // Game ID 3 disables scoring type
  }

  onGameChange(round: Round, event: any): void {
    // If game 3 is selected, set scoring type to 4 (Nines)
    const selectedGameId = +event.target.value;
    if (selectedGameId === 3) {
      round.scoringType = this.scoringTypes().find(st => st.id === 4);
    }
    // Force change detection when game changes to re-evaluate scoring type disable binding
    this.cdr.detectChanges();
  }
}
