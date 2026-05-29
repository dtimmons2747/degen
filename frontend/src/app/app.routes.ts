import { Routes } from '@angular/router';
import { CreateTournamentComponent } from './admin/create-tournament/create-tournament.component';
import { AddPlayersComponent } from './admin/add-players/add-players.component';
import { AddCourseComponent } from './admin/add-course/add-course.component';
import { HandicapCalculatorComponent } from './admin/handicap-calculator/handicap-calculator.component';
import { AddTeeTimesComponent } from './tournament/add-tee-times/add-tee-times.component';
import { EnterScorecardComponent } from './tournament/enter-scorecard/enter-scorecard.component';
import { LeaderboardComponent } from './tournament/leaderboard/leaderboard.component';

export const routes: Routes = [
  { path: 'admin/create-tournament', component: CreateTournamentComponent },
  { path: 'admin/add-players', component: AddPlayersComponent },
  { path: 'admin/add-course', component: AddCourseComponent },
  { path: 'admin/handicap-calculator', component: HandicapCalculatorComponent },
  { path: 'tournament/add-tee-times', component: AddTeeTimesComponent },
  { path: 'tournament/enter-scorecard', component: EnterScorecardComponent },
  { path: 'tournament/leaderboard', component: LeaderboardComponent },
];
