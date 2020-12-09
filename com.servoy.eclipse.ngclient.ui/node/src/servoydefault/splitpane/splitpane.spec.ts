import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyDefaultSplitpane } from './splitpane';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';

describe('ServoyDefaultSplitpane', () => {
  let component: ServoyDefaultSplitpane;
  let fixture: ComponentFixture<ServoyDefaultSplitpane>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultSplitpane ],
 	  imports: [ ServoyPublicModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultSplitpane);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
