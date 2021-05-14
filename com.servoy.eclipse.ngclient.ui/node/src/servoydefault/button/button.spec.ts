import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyDefaultButton } from './button';

import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { TooltipService, ComponentContributor, ServoyApi} from '@servoy/public';
import { ServoyPublicModule } from '@servoy/public';

import { runOnPushChangeDetection } from '../testingutils';

describe('SvyButton', () => {
  let component: ServoyDefaultButton;
  let fixture: ComponentFixture<ServoyDefaultButton>;

  const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'trustAsHtml','registerComponent','unRegisterComponent']);

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultButton],
      providers: [ TooltipService, ComponentContributor],
      imports: [
               ServoyTestingModule, ServoyPublicModule],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultButton);
    component = fixture.componentInstance;
    component.servoyApi =  servoyApi;
    component.toolTipText = 'Hi';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it( 'should have called servoyApi.getMarkupId', () => {
    runOnPushChangeDetection(fixture);
    expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it( 'should render html', async () => {
    servoyApi.trustAsHtml.and.returnValue( true );
    component.dataProviderID = '<div class="myclass" onclick="javascript:test()">hallo</div>';
    runOnPushChangeDetection(fixture);
    expect( component.child.nativeElement.children[1].innerHTML ).toBe(component.dataProviderID);
  });
});
