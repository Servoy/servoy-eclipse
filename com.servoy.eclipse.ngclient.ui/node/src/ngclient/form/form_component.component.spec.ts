import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FormComponent,AddAttributeDirective } from './svy-form.component';

import {FormService} from '../form.service';
import {SabloService} from '../../sablo/sablo.service';

import { ServoyDefaultComponentsModule } from '../../servoydefault/servoydefault.module';

describe('FormComponent', () => {
  let component: FormComponent;
  let fixture: ComponentFixture<FormComponent>;
  let sabloService;
  let formService;

  beforeEach(async(() => {
      sabloService = jasmine.createSpyObj("SabloService", ["callService"]);
      formService = jasmine.createSpyObj("FormService", {getFormCache:{absolute:true}});
    TestBed.configureTestingModule({
      declarations: [ FormComponent,AddAttributeDirective ],
      imports: [
                ServoyDefaultComponentsModule,
       ],
       providers:    [ {provide: FormService, useValue:  formService },
                               {provide: SabloService, useValue:  sabloService }
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
