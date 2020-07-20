import { Component, OnInit, Renderer2, ContentChild, TemplateRef, Input, ViewChild, ElementRef, SimpleChanges } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { ServoyBootstrapBaseComponent } from '../bts_basecomp';
import { SabloService } from '../../sablo/sablo.service';

@Component({
  selector: 'servoybootstrap-tablesspanel',
  templateUrl: './tablesspanel.html',
  styleUrls: ['./tablesspanel.scss']
})
export class ServoyBootstrapTablesspanel extends ServoyBootstrapBaseComponent {

  @Input() containedForm: any;
  @Input() relationName: any;
  @Input() waitForData: any;
  @Input() height: number;

  @ViewChild('element') elementRef:ElementRef;

  private realContainedForm: any;
  private formWillShowCalled: any;

  @ContentChild( TemplateRef  , {static: true})
  templateRef: TemplateRef<any>;

  constructor(renderer: Renderer2) {
    super(renderer);
  }

  ngOnChanges( changes: SimpleChanges ) {
    if (changes) {
      for ( const property of Object.keys(changes) ) {
          const change = changes[property];
          switch ( property ) {
              case 'containedForm': {
                if (change.currentValue !== change.previousValue)
                  if (change.previousValue) {
                    this.formWillShowCalled = change.currentValue;
                    this.servoyApi.hideForm(change.previousValue, null, null, change.currentValue, this.relationName, null)
                      .then(() => {
                        this.realContainedForm = this.containedForm;
                      })
                  } else if (change.currentValue) {
                    this.setRealContainedForm(change.currentValue, this.relationName);
                  }
              }
              case 'visible': {
                if (this.containedForm && change.currentValue !== change.previousValue) {
                  this.formWillShowCalled = this.realContainedForm = undefined;
                  if (change.currentValue) {
                    this.setRealContainedForm(this.containedForm, this.relationName);
                  } else {
                    this.servoyApi.hideForm(this.containedForm);
                  }
                }
              }
            } 
        }
        super.ngOnChanges(changes);
    }
}

  showEditorHint() {
    return !this.containedForm && this.elementRef.nativeElement.getAttribute("svy-id") !== null;
  }

  setRealContainedForm(formName: any, relationName: any) {
    if (this.visible) {
      if (this.formWillShowCalled != formName && formName) {
        this.formWillShowCalled = formName;
        if (this.waitForData) {
          Promise.resolve(this.servoyApi.formWillShow(formName, relationName)).then(() => {
            this.realContainedForm = formName;
          });
        } else {
          this.servoyApi.formWillShow(formName, relationName);
          this.realContainedForm = formName;
        }
      }
    } else {
      // panel is not visible; don't ask server to show child form as that would generate an exception on server
      this.realContainedForm = this.formWillShowCalled = undefined;
    }
  }

  getForm() {
  }

  getContainerStyle() {
  }
}
