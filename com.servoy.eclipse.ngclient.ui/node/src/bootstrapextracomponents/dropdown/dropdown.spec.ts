import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ServoyService } from '../../ngclient/servoy.service';
import { ServoyBootstrapExtraDropdown } from './dropdown';
import { SvyUtilsService, LocaleService} from '../../ngclient/servoy_public';
import { SabloModule } from '../../sablo/sablo.module';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { ClientFunctionService } from '../../ngclient/services/clientfunction.service';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { FormService } from '../../ngclient/form.service';

describe('ServoyBootstrapExtraDropdown', () => {
  let component: ServoyBootstrapExtraDropdown;
  let fixture: ComponentFixture<ServoyBootstrapExtraDropdown>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraDropdown ],
      imports: [SabloModule, ServoyPublicModule],
      providers: [SvyUtilsService, ServoyService, FormService, ClientFunctionService, ViewportService, { provide: LocaleService, useValue: {getLocale: () => 'en' }}]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraDropdown);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
