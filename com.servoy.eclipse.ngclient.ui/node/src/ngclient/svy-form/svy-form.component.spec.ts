import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FormComponent,AddAttributeDirective } from './svy-form.component';

import {FormService} from '../form.service';

import { ServoyDefaultComponentsModule } from '../../servoydefault/servoydefault.module';

describe('FormComponent', () => {
  let component: FormComponent;
  let fixture: ComponentFixture<FormComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FormComponent,AddAttributeDirective ],
      imports: [
                ServoyDefaultComponentsModule,
       ],
       providers:    [ {provide: FormService, useValue: new FormServiceStub() } ]
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
  });
});

class  FormServiceStub {
    public getFormCache(form:FormComponent) {
        return {
            absolute:true
        };
    }
}

