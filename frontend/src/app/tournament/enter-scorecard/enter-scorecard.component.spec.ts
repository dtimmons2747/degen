import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnterScorecardComponent } from './enter-scorecard.component';

describe('EnterScorecardComponent', () => {
  let component: EnterScorecardComponent;
  let fixture: ComponentFixture<EnterScorecardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ EnterScorecardComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EnterScorecardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
