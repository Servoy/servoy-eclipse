import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapTextbox } from './textbox';
import { FormattingService, I18NProvider, LocaleService, TooltipService } from '../../ngclient/servoy_public';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { SabloModule } from '../../sablo/sablo.module';
import { FormsModule } from '@angular/forms';

describe('TextboxComponent', () => {
  let component: ServoyBootstrapTextbox;
  let fixture: ComponentFixture<ServoyBootstrapTextbox>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTextbox ],
      imports: [SabloModule, ServoyPublicModule, FormsModule],
      providers: [I18NProvider, FormattingService, TooltipService, LocaleService ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTextbox);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
