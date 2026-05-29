import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HandicapCalculatorComponent } from './handicap-calculator.component';

describe('HandicapCalculatorComponent', () => {
  let component: HandicapCalculatorComponent;
  let fixture: ComponentFixture<HandicapCalculatorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HandicapCalculatorComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HandicapCalculatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
