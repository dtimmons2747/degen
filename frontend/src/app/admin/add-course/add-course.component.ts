import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { environment } from '../../../environments/environment';

interface HoleInput {
  holeNumber: number;
  par: number;
  yards: number;
  handicap: number;
}

@Component({
  selector: 'app-add-course',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './add-course.component.html',
  styleUrl: './add-course.component.scss'
})
export class AddCourseComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);

  courseForm: FormGroup = this.fb.group({
    courseName: ['', Validators.required],
    tees: ['', Validators.required],
    slope: ['', [Validators.required, Validators.min(0)]],
    rating: ['', [Validators.required, Validators.min(0)]]
  });

  holes = signal<HoleInput[]>(this.initializeHoles());
  isSubmitting = signal(false);

  initializeHoles(): HoleInput[] {
    const holeArray: HoleInput[] = [];
    for (let i = 1; i <= 18; i++) {
      holeArray.push({
        holeNumber: i,
        par: 4,
        yards: 0,
        handicap: 0
      });
    }
    return holeArray;
  }

  updateHole(holeNumber: number, field: string, value: any) {
    const holesArray = [...this.holes()];
    const hole = holesArray.find(h => h.holeNumber === holeNumber);
    if (hole) {
      (hole as any)[field] = value;
      this.holes.set(holesArray);
    }
  }

  onSubmit() {
    if (!this.courseForm.valid) {
      alert('Please fill in all course information');
      return;
    }

    this.isSubmitting.set(true);

    const formValue = this.courseForm.value;
    const courseData = {
      name: formValue.courseName,
      tees: formValue.tees,
      slope: formValue.slope,
      rating: formValue.rating
    };

    // Create course first
    this.http.post<any>(`${environment.apiUrl}/api/courses`, courseData).subscribe({
      next: (createdCourse) => {
        // Create all holes for this course
        const holePromises = this.holes().map(hole =>
          this.http.post(`${environment.apiUrl}/api/holes`, {
            holeNumber: hole.holeNumber,
            par: hole.par,
            yards: hole.yards,
            handicap: hole.handicap,
            courseId: createdCourse.id
          }).toPromise()
        );

        Promise.all(holePromises).then(
          () => {
            alert('Course created successfully with all hole information!');
            this.courseForm.reset();
            this.holes.set(this.initializeHoles());
            this.isSubmitting.set(false);
          },
          () => {
            alert('Course created but there was an error adding some holes.');
            this.isSubmitting.set(false);
          }
        );
      },
      error: () => {
        alert('Error creating course');
        this.isSubmitting.set(false);
      }
    });
  }

  getTotalYards(): number {
    return this.holes().reduce((total, hole) => total + hole.yards, 0);
  }

  getTotalYardsOut(): number {
    return this.holes()
      .slice(0, 9)
      .reduce((total, hole) => total + hole.yards, 0);
  }

  getTotalYardsIn(): number {
    return this.holes()
      .slice(9, 18)
      .reduce((total, hole) => total + hole.yards, 0);
  }

  getTotalPar(): number {
    return this.holes().reduce((total, hole) => total + hole.par, 0);
  }

  getTotalParOut(): number {
    return this.holes()
      .slice(0, 9)
      .reduce((total, hole) => total + hole.par, 0);
  }

  getTotalParIn(): number {
    return this.holes()
      .slice(9, 18)
      .reduce((total, hole) => total + hole.par, 0);
  }
}
