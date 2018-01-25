import { Directive, Input} from '@angular/core';
import { TestBed, async } from '@angular/core/testing';
import { AppComponent } from './app.component';
import {AllServiceService} from './allservices.service';

describe('AppComponent', () => {
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        AppComponent,MockFormComponent
      ],
      providers:    [ {provide: AllServiceService, useValue: new AllServicesMock() } ]
    }).compileComponents();
  }));
  it('should create the app', async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }));
});

@Directive({
    selector: 'svy-form'
  })
  class MockFormComponent{
    @Input('name')
    public name: string;
  }

class AllServicesMock {
    
}
