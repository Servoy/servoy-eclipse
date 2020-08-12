import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapTypeahead } from './typeahead';

describe('TypeaheadComponent', () => {
  let component: ServoyBootstrapTypeahead;
  let fixture: ComponentFixture<ServoyBootstrapTypeahead>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTypeahead ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTypeahead);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
