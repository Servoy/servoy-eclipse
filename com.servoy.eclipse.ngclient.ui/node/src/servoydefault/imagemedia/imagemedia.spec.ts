import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {SabloModule} from '../../sablo/sablo.module'

import { ServoyDefaultImageMedia } from './imagemedia';

import {FormatFilterPipe,SvyFormat, FormattingService,TooltipDirective,TooltipService} from '../../ngclient/servoy_public'
import { UploadDirective } from "../../ngclient/utils/upload.directive";

describe("ServoyDefaultImageMedia", () => {
  let component: ServoyDefaultImageMedia;
  let fixture: ComponentFixture<ServoyDefaultImageMedia>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultImageMedia,TooltipDirective, UploadDirective],
      imports: [SabloModule],
      providers: [FormatFilterPipe,FormattingService,TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultImageMedia);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
