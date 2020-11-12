import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapChoicegroup } from './choicegroup';

describe('ChoicegroupComponent', () => {
  let component: ServoyBootstrapChoicegroup;
  let fixture: ComponentFixture<ServoyBootstrapChoicegroup>;

  beforeEach(waitForAsync(() => {
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
