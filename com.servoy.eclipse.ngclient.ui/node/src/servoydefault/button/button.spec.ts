import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultButton } from './button';

import { SabloModule } from '../../sablo/sablo.module'
import { TooltipService, ComponentContributor} from '../../ngclient/servoy_public'
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'

describe('SvyButton', () => {
  let component: ServoyDefaultButton;
  let fixture: ComponentFixture<ServoyDefaultButton>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultButton],
      providers: [ TooltipService, ComponentContributor],
      imports:[
               SabloModule, ServoyPublicModule],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultButton);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    component.toolTipText = 'Hi';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it( 'should have called servoyApi.getMarkupId', () => {
    expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it( 'should render html', () => {
    component.servoyApi.trustAsHtml.and.returnValue( true )
    component.dataProviderID = '<div class="myclass" onclick="javascript:test()">hallo</div>'
    fixture.detectChanges();
    expect( component.child.nativeElement.children[1].innerHTML ).toBe(component.dataProviderID);
  });
});
