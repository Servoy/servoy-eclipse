import { Component, ChangeDetectorRef, Renderer2, SimpleChanges, ChangeDetectionStrategy, Inject } from '@angular/core';
import { ServoyDefaultBaseField } from '../basefield';
import {  FormattingService, PropertyUtils, ServoyPublicService } from '@servoy/public';
import { DOCUMENT } from '@angular/common';
import tinymce, { RawEditorOptions } from 'tinymce';

@Component({
    selector: 'servoydefault-htmlarea',
    templateUrl: './htmlarea.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultHtmlarea extends ServoyDefaultBaseField<HTMLDivElement> {

    tinyValue: any;
    tinyConfig: RawEditorOptions = {
        suffix: '.min',
        height: '100%',
        menubar: false,
        statusbar: false,
        readonly: false,
        promotion: false,
        toolbar: 'fontselect fontsizeselect | bold italic underline | superscript subscript | undo redo |alignleft aligncenter alignright alignjustify | styleselect | outdent indent bullist numlist'
    };

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService, @Inject(DOCUMENT) doc: Document, protected servoyService: ServoyPublicService) {
        super(renderer, cdRef, formattingService, doc);
    }

    focus() {
        if (this.onFocusGainedMethodID) {
            if (this.mustExecuteOnFocus !== false) {
                this.onFocusGainedMethodID(new CustomEvent('focus'));
            }
            this.mustExecuteOnFocus = true;
        }
    }

    blur() {
        this.dataProviderID = '<html><body>' + this.tinyValue + '</body></html>'
        this.pushUpdate();
        if (this.onFocusLostMethodID) this.onFocusLostMethodID(new CustomEvent('blur'));
    }

    click() {
        if (this.onActionMethodID) this.onActionMethodID(new CustomEvent('click'));
    }

    ngOnInit(){
        super.ngOnInit(); 
        
        this.tinyConfig['language'] = this.servoyService.getLocale();

        this.tinyConfig['base_url'] = this.doc.head.getElementsByTagName('base')[0].href + 'tinymce';

        // app level configuration
        let defaultConfiguration = this.servoyService.getUIProperty('config');
        if (defaultConfiguration) {
            if (typeof defaultConfiguration === 'string') {
                try {
                    defaultConfiguration = JSON.parse(defaultConfiguration);
                } catch (e) {
                    console.error(e);
                }
            }
            for (const key in defaultConfiguration) {
                if (defaultConfiguration.hasOwnProperty(key)) {
                    this.tinyConfig[key] =  defaultConfiguration[key];
                }
            }
        }

        // element level configuration
        let configuration = this.servoyApi.getClientProperty('config');
        if (configuration) {
            if (typeof configuration === 'string') {
                try {
                    configuration = JSON.parse(configuration);
                } catch (e) {
                    console.error(e);
                }
            }
            for (const key in configuration) {
                if (configuration.hasOwnProperty(key)) {
                    this.tinyConfig[key] = configuration[key];
                }
            }
        }

    }
    
    svyOnInit() {
        super.svyOnInit();
        this.tinyValue = this.dataProviderID;
    }

    svyOnChanges(changes: SimpleChanges) {
        if (changes) {
            for (const property of Object.keys(changes)) {
                const change = changes[property];
                switch (property) {
                    case 'styleClass':
                        if (change.previousValue) {
                            const array = change.previousValue.trim().split(' ');
                            array.filter((element: string) => element !== '').forEach((element: string) => this.renderer.removeClass(this.getNativeElement(), element));
                        }
                        if (change.currentValue) {
                            const array = change.currentValue.trim().split(' ');
                            array.filter((element: string) => element !== '').forEach((element: string) => this.renderer.addClass(this.getNativeElement(), element));
                        }
                        break;
                    case 'scrollbars':
                        if (change.currentValue) {
                            const element = this.getNativeChild().textArea;
                            PropertyUtils.setScrollbars(element, this.renderer, change.currentValue);
                        }
                        break;
                    case 'editable':
                    case 'readOnly':
                    case 'enabled':
                        let editable = this.editable && !this.readOnly && this.enabled;
                        if (tinymce.activeEditor) {
                            if (editable) {
                                if (!change.firstChange) {
                                    tinymce.activeEditor.mode.set("design");
                                }
                            }
                            else {
                                tinymce.activeEditor.mode.set("readonly");
                            }

                        }
                        break;
                    case 'dataProviderID':
                        this.tinyValue = this.dataProviderID;
                        break;
                }
            }
        }
        super.svyOnChanges(changes);
    }

    getEditor() {
        return tinymce.get(this.servoyApi.getMarkupId() + '_editor');
    }

    requestFocus(mustExecuteOnFocusGainedMethod: boolean) {
        this.mustExecuteOnFocus = mustExecuteOnFocusGainedMethod;
        this.getEditor().focus();
    }

    public selectAll() {
        let ed = this.getEditor();
        ed.selection.select(ed.getBody(), true);
    }

    public getSelectedText(): string {
        return this.getEditor().selection.getContent();
    }

    public getAsPlainText() {
        return this.getEditor().getContent().replace(/<[^>]*>/g, '');
    }

    public getScrollX(): number {
        return this.getEditor().getWin().scrollX;
    }

    public getScrollY(): number {
        return this.getEditor().getWin().scrollY;
    }

    public replaceSelectedText(text: string) {
        this.getEditor().selection.setContent(text);
        var edContent = this.getEditor().getContent();
        this.dataProviderID = '<html><body>' + edContent + '</body></html>'
        this.pushUpdate();
    }

    public setScroll(x: number, y: number) {
        this.getEditor().getWin().scrollTo(x, y);
    }
}
