import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FormComponent,AddAttributeDirective } from './form_component.component';

import {FormService} from '../form.service';
import {ServoyService} from '../servoy.service';
import {SabloService} from '../../sablo/sablo.service';

import { ServoyDefaultComponentsModule } from '../../servoydefault/servoydefault.module';

describe('FormComponent', () => {
  let component: FormComponent;
  let fixture: ComponentFixture<FormComponent>;
  let sabloService;
  let formService;
  let servoyService;

  beforeEach(async(() => {
      sabloService = jasmine.createSpyObj("SabloService", ["callService"]);
      formService = jasmine.createSpyObj("FormService", {getFormCache:{absolute:true,getComponent:()=>null}, destroy:()=>null});
      servoyService = jasmine.createSpyObj("ServoyService", ["connect"]);
    TestBed.configureTestingModule({
      declarations: [ FormComponent,AddAttributeDirective ],
      imports: [
                ServoyDefaultComponentsModule,
       ],
       providers:    [ {provide: FormService, useValue:  formService },
                               {provide: SabloService, useValue:  sabloService },
                               {provide: ServoyService, useValue:  servoyService }
                             ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(formService.getFormCache).toHaveBeenCalled();
  });
});
