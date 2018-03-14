import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DefaultLoginWindowComponent } from './default-login-window.component';

describe('DefaultLoginWindowComponent', () => {
  let component: DefaultLoginWindowComponent;
  let fixture: ComponentFixture<DefaultLoginWindowComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DefaultLoginWindowComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DefaultLoginWindowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
