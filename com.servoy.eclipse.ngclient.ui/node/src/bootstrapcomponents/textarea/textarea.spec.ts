import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapTextarea } from './textarea';

describe('TextareaComponent', () => {
  let component: ServoyBootstrapTextarea;
  let fixture: ComponentFixture<ServoyBootstrapTextarea>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTextarea ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTextarea);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
