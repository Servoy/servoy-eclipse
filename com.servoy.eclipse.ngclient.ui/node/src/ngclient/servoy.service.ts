import { Injectable } from '@angular/core';

import { AllServiceService } from './allservices.service';
import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { ConverterService } from '../sablo/converter.service'
import { WindowRefService } from '../sablo/util/windowref.service';

import { SessionStorageService } from 'angular-web-storage';

import { DateConverter } from './converters/date_converter'
import { JSONObjectConverter } from './converters/json_object_converter'
import { JSONArrayConverter } from './converters/json_array_converter'

import { IterableDiffers, IterableDiffer } from '@angular/core';

import { SpecTypesService } from '../sablo/spectypes.service'


@Injectable()
export class ServoyService {
    private solutionSettings: SolutionSettings = new SolutionSettings();
    private uiProperties: UIProperties;

    private findModeShortCutAdded = false;

    constructor( private websocketService: WebsocketService,
            private sabloService: SabloService,
            private windowRefService: WindowRefService,
            converterService: ConverterService,
            sessionStorageService: SessionStorageService,
            specTypesService: SpecTypesService,
            iterableDiffers: IterableDiffers ) {

        this.uiProperties = new UIProperties( sessionStorageService )
        const dateConverter = new DateConverter();
        converterService.registerCustomPropertyHandler( "svy_date", dateConverter );
        converterService.registerCustomPropertyHandler( "Date", dateConverter );
        converterService.registerCustomPropertyHandler( "JSON_obj", new JSONObjectConverter( converterService, specTypesService ) );
        converterService.registerCustomPropertyHandler( "JSON_arr", new JSONArrayConverter( converterService, specTypesService, iterableDiffers ) );
    }

    public connect() {
        // maybe do this with defer ($q)
        var solName = this.websocketService.getURLParameter( 's' );
        if ( !solName ) this.solutionSettings.solutionName = /.*\/([\$\w]+)\/.*/.exec( this.websocketService.getPathname() )[1];
        else this.solutionSettings.solutionName = solName;
        this.solutionSettings.windowName = this.sabloService.getWindowId();
        var recordingPrefix;
        if ( this.windowRefService.nativeWindow.location.search.indexOf( "svy_record=true" ) > -1 ) {
            recordingPrefix = "/recording/websocket";

        }
        var wsSession = this.sabloService.connect( '/solution/' + this.solutionSettings.solutionName, { solution: this.solutionSettings.solutionName, clienttype: 2 }, recordingPrefix )
        // TODO find mode and anchors handling (anchors should be handles completely at the server side, css positioning should go over the line)
        wsSession.onMessageObject(( msg, conversionInfo ) => {

            if ( msg.sessionid && recordingPrefix ) {
                var btn = <HTMLAnchorElement>this.windowRefService.nativeWindow.document.createElement( "A" );        // Create a <button> element
                btn.href = "solutions/" + msg.sessionid + ".recording";
                btn.target = "_blank";
                btn.style.position = "absolute";
                btn.style.right = "0px";
                btn.style.bottom = "0px";
                var t = this.windowRefService.nativeWindow.document.createTextNode( "Download" );
                btn.appendChild( t );                                // Append the text to <button>
                this.windowRefService.nativeWindow.document.body.appendChild( btn );
            }
            if ( msg.windowid ) {
                this.solutionSettings.windowName = msg.windowid;
            }
        } );

        wsSession.onopen(( evt ) => {
            // update the main app window with the right size
            wsSession.callService( "$windowService", "resize", { size: { width: this.windowRefService.nativeWindow.innerWidth, height: this.windowRefService.nativeWindow.innerHeight } }, true );
        } );
    }

    public getSolutionSettings(): SolutionSettings {
        return this.solutionSettings;
    }

    public getUIProperties(): UIProperties {
        return this.uiProperties;
    }

    private setFindMode( beanData ) {
        if ( beanData['findmode'] ) {
            if ( this.windowRefService.nativeWindow.shortcut.all_shortcuts['ENTER'] === undefined ) {
                this.findModeShortCutAdded = true;

                this.windowRefService.nativeWindow.shortcut.add( 'ENTER', this.performFind );
            }
        }
        else if ( beanData['findmode'] == false && this.findModeShortCutAdded ) {
            this.findModeShortCutAdded = false;
            this.windowRefService.nativeWindow.shortcut.remove( 'ENTER' );
        }
    }

    private performFind( event ) {
        // TODO this was:  angular.element( event.srcElement ? event.srcElement : event.target );
        var element = event.srcElement ? event.srcElement : event.target
        // TODO this whole looking of ng-model and servoy api from the attribute...;
        //        if ( element && element.attr( 'ng-model' ) ) {
        //            var dataproviderString = element.attr( 'ng-model' );
        //            var index = dataproviderString.indexOf( '.' );
        //            if ( index > 0 ) {
        //                var modelString = dataproviderString.substring( 0, index );
        //                var propertyname = dataproviderString.substring( index + 1 );
        //                var svyServoyApi = $utils.findAttribute( element, element.scope(), "svy-servoyApi" );
        //                if ( svyServoyApi && svyServoyApi.apply ) {
        //                    svyServoyApi.apply( propertyname );
        //                }
        //            }
        //        }
        //
        //        this.sabloService.callService( "formService", "performFind", { 'formname': formname, 'clear': true, 'reduce': true, 'showDialogOnNoResults': true }, true );
    }
}

class UIProperties {
    private uiProperties;

    constructor( private sessionStorageService: SessionStorageService ) {
    }

    private getUiProperties() {
        if ( !this.uiProperties ) {
            var json = this.sessionStorageService.get( "uiProperties" );
            if ( json ) {
                this.uiProperties = JSON.parse( json );
            } else {
                this.uiProperties = {};
            }
        }
        return this.uiProperties;
    }

    public getUIProperty( key ) {
        var value = this.getUiProperties()[key];
        if ( value === undefined ) {
            value = null;
        }
        return value;
    }
    public setUIProperty( key, value ) {
        var uiProps = this.getUiProperties();
        if ( value == null ) delete uiProps[key];
        else uiProps[key] = value;
        this.sessionStorageService.set( "uiProperties", JSON.stringify( uiProps ) )
    }
}

class SolutionSettings {
    public solutionName: string;
    public windowName: string;
    public enableAnchoring: boolean = true;
    public ltrOrientation: boolean = true;
    public solutionTitle = "";
    public mainForm: FormSettings;
    public navigatorForm: FormSettings;
    public styleSheetPaths = [];
}

class AnchorConstants {
    public static readonly NORTH = 1;
    public static readonly EAST = 2;
    public static readonly SOUTH = 4;
    public static readonly WEST = 8;
}

class FormSettings {
    public name: String;
    public size: { width: number, height: number };
}