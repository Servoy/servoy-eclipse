import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapImageMedia } from './imagemedia';

describe('ImagemediaComponent', () => {
  let component: ServoyBootstrapImageMedia;
  let fixture: ComponentFixture<ServoyBootstrapImageMedia>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapImageMedia ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapImageMedia);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
