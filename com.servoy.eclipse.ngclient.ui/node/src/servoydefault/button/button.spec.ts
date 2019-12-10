import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultButton } from './button';

import { SabloModule } from '../../sablo/sablo.module'
import { TooltipService} from '../../ngclient/servoy_public'
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'

describe('SvyButton', () => {
  let component: ServoyDefaultButton;
  let fixture: ComponentFixture<ServoyDefaultButton>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultButton],
      providers: [ TooltipService],
      imports:[
               SabloModule, ServoyPublicModule],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultButton);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
