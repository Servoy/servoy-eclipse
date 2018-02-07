import { Injectable } from '@angular/core';

import { AllServiceService } from './allservices.service';
import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { WindowRefService } from '../sablo/util/windowref.service';

import { SessionStorageService } from 'angular-web-storage';


@Injectable()
export class ServoyService {
    private solutionSettings: SolutionSettings = new SolutionSettings();
    private uiProperties:UIProperties;

    private findModeShortCutAdded = false;

    constructor( private websocketService: WebsocketService, 
                            private sabloService: SabloService, 
                            private windowRefService: WindowRefService,
                            sessionStorageService:SessionStorageService) {
        this.uiProperties = new UIProperties(sessionStorageService)
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
        var wsSession = this.sabloService.connect( '/solution/' + this.solutionSettings.solutionName, { solution: this.solutionSettings.solutionName }, recordingPrefix )
        wsSession.onMessageObject(( msg, conversionInfo ) => {
            // data got back from the server
            for ( var formname in msg.forms ) {
                // current model
                var formState = this.sabloService.getFormStateEvenIfNotYetResolved( formname );
                if ( typeof ( formState ) == 'undefined' ) {
                    // if the form is not yet on the client ,wait for it and then apply it
                    this.sabloService.getFormState( formname ).then( this.getFormMessageHandler( formname, msg, conversionInfo ),
                        ( err ) => {
                            //                      $log.error("Error getting form state (svy) when trying to handle msg. from server: " + err); 
                        } );
                }
                else {
                    this.getFormMessageHandler( formname, msg, conversionInfo )( formState );
                }
            }



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

    private getFormMessageHandler( formname, msg, conversionInfo ) {
        return ( formState ) => {
            var formModel = formState.model;
            var layout = formState.layout;
            var newFormData = msg.forms[formname];

            for ( var beanname in newFormData ) {
                // copy over the changes, skip for form properties (beanname empty)
                var beanData = newFormData[beanname];
                if ( beanname != '' ) {
                    var beanModel = formModel[beanname];
                    if ( beanModel != undefined && ( beanData.size != undefined || beanData.location != undefined ) ) {
                        //size or location were changed at runtime, we need to update components with anchors
                        beanData.anchors = beanModel.anchors;
                    }
                    this.applyBeanLayout( beanModel, layout[beanname], beanData, formState.properties.designSize, false )
                }
                else if ( beanData['findmode'] !== undefined ) {
                    this.setFindMode( beanData );
                }

            }
        }
    }

    private applyBeanLayout( beanModel, beanLayout, beanData, containerSize, isApplyBeanData ) {

        if ( !beanLayout ) return;
        var runtimeChanges = !isApplyBeanData && ( beanData.size != undefined || beanData.location != undefined );
        //beanData.anchors means anchors changed or must be initialized
        if ( ( beanData.anchors || runtimeChanges ) && containerSize && this.solutionSettings.enableAnchoring ) {
            var anchoredTop = ( beanModel.anchors & AnchorConstants.NORTH ) != 0; // north
            var anchoredRight = ( beanModel.anchors & AnchorConstants.EAST ) != 0; // east
            var anchoredBottom = ( beanModel.anchors & AnchorConstants.SOUTH ) != 0; // south
            var anchoredLeft = ( beanModel.anchors & AnchorConstants.WEST ) != 0; //west

            if ( !anchoredLeft && !anchoredRight ) anchoredLeft = true;
            if ( !anchoredTop && !anchoredBottom ) anchoredTop = true;

            if ( anchoredTop || runtimeChanges ) {
                if ( beanLayout.top == undefined || runtimeChanges && beanModel.location != undefined ) beanLayout.top = beanModel.location.y + 'px';
            }
            else delete beanLayout.top;

            if ( anchoredBottom ) {
                if ( beanLayout.bottom == undefined ) {
                    beanLayout.bottom = ( beanModel.partHeight ? beanModel.partHeight : containerSize.height ) - beanModel.location.y - beanModel.size.height;
                    if ( beanModel.offsetY ) {
                        beanLayout.bottom = beanLayout.bottom - beanModel.offsetY;
                    }
                    beanLayout.bottom = beanLayout.bottom + "px";
                }
            }
            else delete beanLayout.bottom;

            if ( !anchoredTop || !anchoredBottom ) beanLayout.height = beanModel.size.height + 'px';
            else delete beanLayout.height;

            if ( anchoredLeft || runtimeChanges ) {
                if ( this.solutionSettings.ltrOrientation ) {
                    if ( beanLayout.left == undefined || runtimeChanges && beanModel.location != undefined ) {
                        beanLayout.left = beanModel.location.x + 'px';
                    }
                }
                else {
                    if ( beanLayout.right == undefined || runtimeChanges && beanModel.location != undefined ) {
                        beanLayout.right = beanModel.location.x + 'px';
                    }
                }
            }
            else if ( this.solutionSettings.ltrOrientation ) {
                delete beanLayout.left;
            }
            else {
                delete beanLayout.right;
            }

            if ( anchoredRight ) {
                if ( this.solutionSettings.ltrOrientation ) {
                    if ( beanLayout.right == undefined ) beanLayout.right = ( containerSize.width - beanModel.location.x - beanModel.size.width ) + "px";
                }
                else {
                    if ( beanLayout.left == undefined ) beanLayout.left = ( containerSize.width - beanModel.location.x - beanModel.size.width ) + "px";
                }
            }
            else if ( this.solutionSettings.ltrOrientation ) {
                delete beanLayout.right;
            }
            else {
                delete beanLayout.left;
            }

            if ( !anchoredLeft || !anchoredRight ) beanLayout.width = beanModel.size.width + 'px';
            else delete beanLayout.width;
        }

        //we set the following properties iff the bean doesn't have anchors
        var isAnchoredTopLeftBeanModel = !beanModel.anchors || ( beanModel.anchors == ( AnchorConstants.NORTH + AnchorConstants.WEST ) );
        if ( isAnchoredTopLeftBeanModel || !this.solutionSettings.enableAnchoring ) {
            if ( beanModel.location ) {
                if ( this.solutionSettings.ltrOrientation ) {
                    beanLayout.left = beanModel.location.x + 'px';
                }
                else {
                    beanLayout.right = beanModel.location.x + 'px';
                }
                beanLayout.top = beanModel.location.y + 'px';
            }

            if ( beanModel.size ) {
                beanLayout.width = beanModel.size.width + 'px';
                beanLayout.height = beanModel.size.height + 'px';
            }
        }

        // TODO: visibility must be based on properties of type visible, not on property name
        if ( beanModel.visible != undefined ) {
            if ( beanModel.visible == false ) {
                beanLayout.display = 'none';
            }
            else {
                delete beanLayout.display;
            }
        }
    }
}

class UIProperties {
    private uiProperties;
    
    constructor(private sessionStorageService:SessionStorageService) {
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