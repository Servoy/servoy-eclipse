import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyExtraSplitpane } from './splitpane';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';

describe('ServoyExtraSplitpane', () => {
  let component: ServoyExtraSplitpane;
  let fixture: ComponentFixture<ServoyExtraSplitpane>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyExtraSplitpane ],
 	  imports: [ ServoyPublicModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyExtraSplitpane);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
