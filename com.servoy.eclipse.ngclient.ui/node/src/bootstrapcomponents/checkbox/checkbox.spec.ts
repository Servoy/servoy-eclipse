import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapCheckbox } from './checkbox';

describe('CheckboxComponent', () => {
  let component: ServoyBootstrapCheckbox;
  let fixture: ComponentFixture<ServoyBootstrapCheckbox>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapCheckbox ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapCheckbox);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
