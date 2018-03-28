import { Injectable } from '@angular/core';

import {ServoyService} from '../servoy.service'

import { LocalStorageService,SessionStorageService } from 'angular-web-storage';
import {WindowRefService} from '../../sablo/util/windowref.service';

import {SabloService} from '../../sablo/sablo.service';

import * as numeral from 'numeral';

import { NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { DefaultLoginWindowComponent } from './default-login-window/default-login-window.component'
import { FileUploadWindowComponent } from './file-upload-window/file-upload-window.component'

@Injectable()
export class ApplicationService {
    private userProperties;
    
    constructor(private servoyService:ServoyService, 
                            private localStorageService:LocalStorageService,
                            private sessionStorageService:SessionStorageService,
                            private windowRefService:WindowRefService,
                            private sabloService:SabloService,
                            private modalService:NgbModal) {
    }

    public setStyleSheets(paths) {
        this.servoyService.getSolutionSettings().styleSheetPaths = paths;
    }
    
    public getUserProperty(key) {
        return this.getUserProperties()[key];
    }
    
    public setUserProperty(key,value) {
        var userProps = this.getUserProperties();
        if (value == null) delete userProps[key];
        else userProps[key] = value;
        this.localStorageService.set("userProperties", JSON.stringify(userProps))
    }
    
    public getUIProperty(key) {
        return this.servoyService.getUIProperties().getUIProperty(key);
    }
    
    public setUIProperty(key,value) {
        this.servoyService.getUIProperties().setUIProperty(key, value);
    }
    
    public getUserPropertyNames() {
        return Object.getOwnPropertyNames(this.getUserProperties());
    }
    
    public showMessage(message) {
        this.windowRefService.nativeWindow.alert(message);
    }
    
    public showUrl(url,target,targetOptions,timeout){
        if(!target) target ='_blank';
        if(!timeout) timeout = 0;            
        setTimeout(() => {
            if(url.indexOf('resources/dynamic') === 0 && target === '_self') {
                var ifrm = document.getElementById('srv_downloadframe');
                if (ifrm) {
                    ifrm.setAttribute("src", url);
                }
                else {
                    ifrm = document.createElement("IFRAME");
                    ifrm.setAttribute("src", url);
                    ifrm.setAttribute('id', 'srv_downloadframe');
                    ifrm.setAttribute('name', 'srv_downloadframe');
                    ifrm.style.width = 0 + "px";
                    ifrm.style.height = 0 + "px";
                    this.windowRefService.nativeWindow.document.body.appendChild(ifrm);
                }
            }
            else {
                this.windowRefService.nativeWindow.open(url,target,targetOptions);
            }
        }, timeout*1000)
    }
    
    public setStatusText(text){
        this.windowRefService.nativeWindow.status = text;      
    }
    
    public getScreenSize() {
        if (this.windowRefService.nativeWindow.screen) {
            return{width: this.windowRefService.nativeWindow.screen.width, height: this.windowRefService.nativeWindow.screen.height, orientation:this.windowRefService.nativeWindow.orientation};
        }
        return null;
    }
    
    public getUserAgentAndPlatform() {
        return {userAgent:this.windowRefService.nativeWindow.navigator.userAgent,platform:this.windowRefService.nativeWindow.navigator.platform};
    }
    
    public getClientBrowserInformation() {
        var locale = this.sabloService.getLocale();
        this.setAngularLocale(locale.language);
        var userAgent = this.getUserAgentAndPlatform();
        return {
            serverURL: this.getServerURL(),
            userAgent : userAgent.userAgent,
            platform : userAgent.platform,
            locale : locale.full,
            remote_ipaddress : this.windowRefService.nativeWindow.servoy_remoteaddr,
            remote_host : this.windowRefService.nativeWindow.servoy_remotehost,
            utcOffset : (new Date(new Date().getFullYear(), 0, 1, 0, 0, 0, 0).getTimezoneOffset() / -60),utcDstOffset:(new Date(new Date().getFullYear(), 6, 1, 0, 0, 0, 0).getTimezoneOffset() / -60)
        };
    }
    
    public setAngularLocale(language){
        // TODO how does locale work in angular 2+
//        var fileref= this.windowRefService.nativeWindow.document.createElement('script');
//        fileref.setAttribute("type","text/javascript");
//        fileref.setAttribute("src", "js/angular_i18n/angular-locale_"+language+".js");
//        fileref.onload = function () {
//            var localInjector = angular.injector(['ngLocale']),
//            externalLocale = localInjector.get('$locale');
//            angular.forEach(externalLocale, function(value, key) {
//                $locale[key] = externalLocale[key];
//            });
//        }
//        this.windowRefService.nativeWindow.document.getElementsByTagName("head")[0].appendChild(fileref);
    }
    
    public setLocale(language, country, initializing) {
        try{
       // TODO angular $translate and our i18n service
//            $translate.refresh();
//            $svyI18NService.flush();
            this.setAngularLocale(language);
            numeral.localeData((language + '-' + country).toLowerCase());
            numeral.locale((language + '-' + country).toLowerCase());
            if (!initializing) this.sessionStorageService.set("locale", (language + '-' + country).toLowerCase());
        } catch(e) {
            try {
                numeral.localeData(language + '-' + country);
                numeral.locale(language + '-' + country);
                if (!initializing) this.sessionStorageService.set("locale", language + '-' + country);
            } catch(e2) {
                try {
                    //try it with just the language part
                    numeral.localeData(language);
                    numeral.locale(language);
                    if (!initializing) this.sessionStorageService.set("locale", language);
                } catch(e3) {
                    try {
                        //try it with just the language part but lowercase
                        numeral.localeData(language.toLowerCase());
                        numeral.locale(language.toLowerCase());
                        if (!initializing) this.sessionStorageService.set("locale", language);
                    } catch(e4) {
                        try {
                            //try to duplicate the language in case it's only defined like that
                            numeral.localeData(language.toLowerCase() + "-" + language.toLowerCase()); // nl-nl for example is defined but browser only says 'nl' (this won't work for all languages for example "en-en" I don't think even exists)
                            numeral.locale(language.toLowerCase() + "-" + language.toLowerCase()); 
                            if (!initializing) this.sessionStorageService.set("locale", language);
                        } catch(e5) {
                            // we can't find a suitable locale defined in locales.js; get the needed things from server (Java knows more locales)
                            // and create the locate info from that
                            var promise = this.sabloService.callService("i18nService", "generateLocaleForNumeralJS", country ? {'language' : language, 'country' : country} : {'language' : language}, false);
                            // TODO should we always do this (get stuff from server side java) instead of trying first to rely on numeral.js and locales.js provided langs?
                            var numeralLanguage = language + (country ? '-' + country : "");
                            promise.then(function(numeralLocaleInfo) {
//                                if ($log.debugEnabled) $log.debug("Locale '" + numeralLanguage + "' not found in client js lib, but it was constructed based on server Java locale-specific information: " + JSON.stringify(numeralLocaleInfo));
                                numeralLocaleInfo.ordinal = function (number) {
                                    return ".";
                                };
                                //numeral.language(numeralLanguage, numeralLocaleInfo);
                                numeral.locale(numeralLanguage);
                                if (!initializing) {
                                    this.sessionStorageService.set("locale", numeralLanguage);
                                    this.sabloService.setLocale({ language : language, country : country , full: language + "-" + country});
                                }
                            }, function(reason) {
//                                $log.warn("Cannot properly handle locale '" + numeralLanguage + "'. It is not available in js libs and it could not be loaded from server...");
                            });
                        }
                    }
                }
            }
        }
        var lang = this.sessionStorageService.get("locale");
        if (lang) {
            var array = lang.split('-');
            this.sabloService.setLocale({ language : array[0], country : (array[1] ?  array[1] : country), full: (array[1] ?  lang :  (array[0] +"-"+country))});
        }
    }
    
    public showInfoPanel(url,w,h,t,closeText)
    {
        var infoPanel=document.createElement("div");
        infoPanel.innerHTML="<iframe marginheight=0 marginwidth=0 scrolling=no frameborder=0 src='"+url+"' width='100%' height='"+(h-25)+"'></iframe><br><a href='#' onClick='javascript:document.getElementById(\"infoPanel\").style.display=\"none\";return false;'>"+closeText+"</a>";
        infoPanel.style.zIndex="2147483647";  
        infoPanel.id="infoPanel"; 
        var width = window.innerWidth || document.body.offsetWidth; 
        infoPanel.style.position = "absolute"; 
        infoPanel.style.left = ((width - w) - 30) + "px";  
        infoPanel.style.top = "10px"; 
        infoPanel.style.height= h+"px"; 
        infoPanel.style.width= w+"px";
        document.body.appendChild(infoPanel);
        setTimeout('document.getElementById(\"infoPanel\").style.display=\"none\"',t);
    }
    
    public showDefaultLogin() {
        if(this.localStorageService.get('servoy_username') && this.localStorageService.get('servoy_password')) {
            var promise = this.sabloService.callService("applicationServerService", "login", {'username' : this.localStorageService.get('servoy_username'), 'password' : this.localStorageService.get('servoy_password'), 'encrypted': true}, false);
            promise.then((ok) =>{
                if(!ok) {
                    this.localStorageService.remove('servoy_username');
                    this.localStorageService.remove('servoy_password');
                    this.showDefaultLoginWindow();
                }
            })              
        } else {
            this.showDefaultLoginWindow();
        }       
    }
    
    public showFileOpenDialog(title, multiselect, acceptFilter) {
        const modalRef = this.modalService.open(FileUploadWindowComponent, { backdrop: 'static' });
        modalRef.componentInstance.url = "/resources/upload/" + this.sabloService.getSessionId();
        modalRef.componentInstance.title = title;
        modalRef.componentInstance.multiselect = multiselect;
        modalRef.componentInstance.filter = acceptFilter;
    }
    public getSolutionName() {
        return this.servoyService.getSolutionSettings().solutionName;
    }
    
    public trustAsHtml(beanModel) {
        
        if (beanModel && beanModel.clientProperty && beanModel.clientProperty.trustDataAsHtml)
        {
            return beanModel.clientProperty.trustDataAsHtml;
        }
        return this.servoyService.getUIProperties().getUIProperty("trustDataAsHtml");
    }
    
    private  showDefaultLoginWindow() {   
        this.modalService.open(DefaultLoginWindowComponent, { backdrop: 'static' });
    }
    
    private   getUserProperties() {
        if (!this.userProperties) {
            var json = this.localStorageService.get("userProperties");
            if (json) {
               this.userProperties = JSON.parse(json);
            } else {
                this.userProperties = {};
            }
        }
        return this.userProperties;
    }
    
    private  getServerURL() {
        // current remote address including the context (includes leading /)
        var context = this.windowRefService.nativeWindow.document.getElementsByTagName ("base")[0].getAttribute("href");
        return this.windowRefService.nativeWindow.location.protocol + '//'+ this.windowRefService.nativeWindow.location.host + context;
    }

}