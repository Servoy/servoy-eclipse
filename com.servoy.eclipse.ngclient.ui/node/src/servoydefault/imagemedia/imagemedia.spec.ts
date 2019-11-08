import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {SabloModule} from '../../sablo/sablo.module'

import { ServoyDefaultImageMedia } from './imagemedia';

import {FormatFilterPipe,SvyFormat, FormattingService} from '../../ngclient/servoy_public'

describe("ServoyDefaultImageMedia", () => {
  let component: ServoyDefaultImageMedia;
  let fixture: ComponentFixture<ServoyDefaultImageMedia>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultImageMedia],
      imports: [SabloModule],
      providers: [FormatFilterPipe,FormattingService]
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
