import { Component, ChangeDetectorRef, Renderer2, ContentChild, TemplateRef, Input, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';
import { FormService } from '../../ngclient/form.service';

@Component({
    selector: 'servoycore-formcontainer',
    templateUrl: './formcontainer.html',
    styleUrls: ['./formcontainer.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyCoreFormContainer extends ServoyBaseComponent<HTMLDivElement> {

    @Input() containedForm: any;
    @Input() relationName: any;
    @Input() waitForData: any;
    @Input() height: number;
    @Input() tabSeq: number;
    @Input() toolTipText: string;

    @ContentChild(TemplateRef, { static: true })
    templateRef: TemplateRef<any>;

    private realContainedForm: any;
    private formWillShowCalled: any;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, private formService: FormService) {
        super(renderer, cdRef);
    }

    svyOnChanges(changes: SimpleChanges) {
        if (changes) {
            for (const property of Object.keys(changes)) {
                const change = changes[property];
                switch (property) {
                    case 'containedForm': {
                        if (change.currentValue !== change.previousValue)
                            if (change.previousValue) {
                                this.formWillShowCalled = change.currentValue;
                                this.servoyApi.hideForm(change.previousValue, null, null, change.currentValue, this.relationName, null)
                                    .then(() => {
                                        this.realContainedForm = this.containedForm;
                                        this.cdRef.detectChanges();
                                    });
                            } else if (change.currentValue) {
                                this.setRealContainedForm(change.currentValue, this.relationName);
                            }
                        break;
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
                        break;
                    }
                }
            }
            super.svyOnChanges(changes);
        }
    }

    setRealContainedForm(formName: any, relationName: any) {
        if (this.formWillShowCalled !== formName && formName) {
            this.formWillShowCalled = formName;
            if (this.waitForData) {
                this.servoyApi.formWillShow(formName, relationName).then(() => {
                    this.realContainedForm = formName;
                    this.cdRef.detectChanges();
                });
            } else {
                this.servoyApi.formWillShow(formName, relationName).then(() => this.cdRef.detectChanges());
                this.realContainedForm = formName;
            }
        }
    }

    getForm() {
        return this.realContainedForm;
    }

    getContainerStyle() {
        const style = { position: 'relative' };
        let minHeight = 0;
        if (this.height) {
            minHeight = this.height;
        } else if (this.containedForm) {
            // for absolute form default height is design height, for responsive form default height is 0
            const formCache = this.formService.getFormCacheByName(this.containedForm);
            if (formCache && formCache.absolute) {
                minHeight = formCache.size.height;
            }
        }
        if (minHeight > 0) {
            style['minHeight'] = minHeight + 'px';
        }
        return style;
    }
}
