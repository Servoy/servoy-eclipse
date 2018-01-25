import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SvyButton } from './button';

describe('SvyButton', () => {
  let component: SvyButton;
  let fixture: ComponentFixture<SvyButton>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SvyButton ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SvyButton);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
