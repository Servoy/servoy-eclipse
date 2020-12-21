import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA, SimpleChange } from '@angular/core';

import { FormComponent, AddAttributeDirective } from './form_component.component';

import {FormService} from '../form.service';
import {ServoyService} from '../servoy.service';
import {SabloService} from '../../sablo/sablo.service';

import { ErrorBean } from '../../servoycore/error-bean/error-bean';
import { ServoyDefaultComponentsModule } from '../../servoydefault/servoydefault.module';
import {ServoyCoreSlider} from '../../servoycore/slider/slider';

import { ServoyPublicModule } from '../servoy_public.module';


describe('FormComponent', () => {
  let component: FormComponent;
  let fixture: ComponentFixture<FormComponent>;
  let sabloService;
  let formService;
  let servoyService;

  beforeEach(waitForAsync(() => {
      sabloService = jasmine.createSpyObj('SabloService', ['callService']);
      formService = jasmine.createSpyObj('FormService', {
                        getFormCache: {
                                  absolute: true,
                                  getComponent: () => ({model: {}})
                                },
                        destroy: () => null
                      });
      servoyService = jasmine.createSpyObj('ServoyService', ['connect']);
    TestBed.configureTestingModule({
      declarations: [ FormComponent, AddAttributeDirective, ServoyCoreSlider, ErrorBean ],
      imports: [
                ServoyDefaultComponentsModule, ServoyPublicModule
       ],
       providers:    [ {provide: FormService, useValue:  formService },
                               {provide: SabloService, useValue:  sabloService },
                               {provide: ServoyService, useValue:  servoyService }
                             ],
       schemas: [
              CUSTOM_ELEMENTS_SCHEMA
       ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FormComponent);
    component = fixture.componentInstance;
    component.ngOnChanges({name:new SimpleChange(null,'test',true)});
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(formService.getFormCache).toHaveBeenCalled();
  });
});
