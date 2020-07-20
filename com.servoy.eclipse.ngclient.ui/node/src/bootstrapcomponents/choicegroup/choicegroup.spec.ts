import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapChoicegroup } from './choicegroup';

describe('ChoicegroupComponent', () => {
  let component: ServoyBootstrapChoicegroup;
  let fixture: ComponentFixture<ServoyBootstrapChoicegroup>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapChoicegroup ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapChoicegroup);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
