import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { ServoyExtraMultiFileUpload } from './multifileupload';
import { SvyUtilsService, LocaleService} from '../../ngclient/servoy_public';
import { FormService } from '../../ngclient/form.service';
import { ServoyService } from '../../ngclient/servoy.service';
import { ViewportService } from '../../ngclient/services/viewport.service';

describe('ServoyExtraMultiFileUpload', () => {
  let component: ServoyExtraMultiFileUpload;
  let fixture: ComponentFixture<ServoyExtraMultiFileUpload>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyExtraMultiFileUpload ],
      imports: [SabloModule, ServoyPublicModule],
      providers: [SvyUtilsService, ViewportService, FormService, ServoyService, { provide: LocaleService, useValue: {getLocale: () => 'en' }}]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyExtraMultiFileUpload);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent', 'getFormname']);
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
