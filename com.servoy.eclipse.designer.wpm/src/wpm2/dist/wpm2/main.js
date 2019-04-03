(window["webpackJsonp"] = window["webpackJsonp"] || []).push([["main"],{

/***/ "./src/$$_lazy_route_resource lazy recursive":
/*!**********************************************************!*\
  !*** ./src/$$_lazy_route_resource lazy namespace object ***!
  \**********************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

function webpackEmptyAsyncContext(req) {
	// Here Promise.resolve().then() is used instead of new Promise() to prevent
	// uncaught exception popping up in devtools
	return Promise.resolve().then(function() {
		var e = new Error("Cannot find module '" + req + "'");
		e.code = 'MODULE_NOT_FOUND';
		throw e;
	});
}
webpackEmptyAsyncContext.keys = function() { return []; };
webpackEmptyAsyncContext.resolve = webpackEmptyAsyncContext;
module.exports = webpackEmptyAsyncContext;
webpackEmptyAsyncContext.id = "./src/$$_lazy_route_resource lazy recursive";

/***/ }),

/***/ "./src/environments/environment.ts":
/*!*****************************************!*\
  !*** ./src/environments/environment.ts ***!
  \*****************************************/
/*! exports provided: environment */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "environment", function() { return environment; });
// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.
var environment = {
    production: false
};
/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.


/***/ }),

/***/ "./src/main.ts":
/*!*********************!*\
  !*** ./src/main.ts ***!
  \*********************/
/*! no exports provided */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_platform_browser_dynamic__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/platform-browser-dynamic */ "./node_modules/@angular/platform-browser-dynamic/fesm5/platform-browser-dynamic.js");
/* harmony import */ var _wpm_wpm_module__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! ./wpm/wpm.module */ "./src/wpm/wpm.module.ts");
/* harmony import */ var _environments_environment__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ./environments/environment */ "./src/environments/environment.ts");




if (_environments_environment__WEBPACK_IMPORTED_MODULE_3__["environment"].production) {
    Object(_angular_core__WEBPACK_IMPORTED_MODULE_0__["enableProdMode"])();
}
Object(_angular_platform_browser_dynamic__WEBPACK_IMPORTED_MODULE_1__["platformBrowserDynamic"])().bootstrapModule(_wpm_wpm_module__WEBPACK_IMPORTED_MODULE_2__["WpmModule"])
    .catch(function (err) { return console.error(err); });


/***/ }),

/***/ "./src/wpm/content/content.component.css":
/*!***********************************************!*\
  !*** ./src/wpm/content/content.component.css ***!
  \***********************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = ""

/***/ }),

/***/ "./src/wpm/content/content.component.html":
/*!************************************************!*\
  !*** ./src/wpm/content/content.component.html ***!
  \************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<div class=\"content\">\n  <mat-tab-group>\n        <mat-tab *ngFor=\"let packageList of packageLists\" label=\"{{getPackageTabLabel(packageList)}}\">\n          <app-packages [packageList]=\"packageList\"></app-packages>\n        </mat-tab>\n  </mat-tab-group>\n</div>"

/***/ }),

/***/ "./src/wpm/content/content.component.ts":
/*!**********************************************!*\
  !*** ./src/wpm/content/content.component.ts ***!
  \**********************************************/
/*! exports provided: ContentComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "ContentComponent", function() { return ContentComponent; });
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _wpm_service__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ../wpm.service */ "./src/wpm/wpm.service.ts");
var __decorate = (undefined && undefined.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (undefined && undefined.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var ContentComponent = /** @class */ (function () {
    function ContentComponent() {
        this.packageLists = [
            {
                title: "NG Components",
                type: _wpm_service__WEBPACK_IMPORTED_MODULE_1__["PACKAGE_TYPE_WEB_COMPONENT"],
                updateCount: 0
            },
            {
                title: "NG Services",
                type: _wpm_service__WEBPACK_IMPORTED_MODULE_1__["PACKAGE_TYPE_WEB_SERVICE"],
                updateCount: 0
            },
            {
                title: "NG Layouts",
                type: _wpm_service__WEBPACK_IMPORTED_MODULE_1__["PACKAGE_TYPE_WEB_LAYOUT"],
                updateCount: 0
            },
            {
                title: "Servoy solutions",
                type: _wpm_service__WEBPACK_IMPORTED_MODULE_1__["PACKAGE_TYPE_SOLUTION"],
                updateCount: 0
            }
        ];
    }
    ContentComponent.prototype.ngOnInit = function () {
    };
    ContentComponent.prototype.getPackageTabLabel = function (packageList) {
        return packageList.updateCount > 0 ? packageList.title + " (" + packageList.updateCount + ")" : packageList.title;
    };
    ContentComponent = __decorate([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_0__["Component"])({
            selector: 'app-content',
            template: __webpack_require__(/*! ./content.component.html */ "./src/wpm/content/content.component.html"),
            styles: [__webpack_require__(/*! ./content.component.css */ "./src/wpm/content/content.component.css")]
        }),
        __metadata("design:paramtypes", [])
    ], ContentComponent);
    return ContentComponent;
}());



/***/ }),

/***/ "./src/wpm/header/header.component.css":
/*!*********************************************!*\
  !*** ./src/wpm/header/header.component.css ***!
  \*********************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = ".header-content {\n    padding-left: 10px;\n    padding-right: 10px;\n    background-color: #000;\n    color: #fff;\n    font-size: medium;\n    text-transform: none;\n    display: block;\n    overflow: auto;\n}\n\n.wpm-title {\n    margin-bottom: 4px;\n  }\n\n.wpm-subtitle {\n    margin-top: 0px;\n    margin-bottom: 20px;\n    font-size: 14px;\n}\n\n.warning {\n    color: red;\n}"

/***/ }),

/***/ "./src/wpm/header/header.component.html":
/*!**********************************************!*\
  !*** ./src/wpm/header/header.component.html ***!
  \**********************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<div class=\"header-content\">\n    <!--\n    <div style=\"float:right\" layout=\"row\">\n     <md-select ng-model=\"activeRepository\" class=\"repository\">\n        <md-option ng-value=\"repository\" ng-repeat=\"repository in repositories\">{{repository}}</md-option>\n         <md-tooltip md-direction=\"bottom\" md-autohide=\"true\">Select to change the repository</md-tooltip>\n      </md-select>\n       <md-button style=\"height:auto;\" class=\"md-icon-button fa fa-trash\"  ng-click=\"removeRepository()\" ng-if=\"showRemoveRepository\">\n        <md-tooltip md-direction=\"bottom\" md-autohide=\"true\">\n          Remove the repository url'.\n        </md-tooltip>\n      </md-button>\n    </div>\n    -->\n    <h1 class=\"wpm-title\">Servoy Package Manager</h1>\n    <p *ngIf=\"isNeedRefresh()\" class=\"warning\">There are new packages available! Reopen the Web Package Manager to load them.</p>\n    <p class=\"wpm-subtitle\">Manage Web Packages of the active solution {{ !!getActiveSolution() ? \"'\" + getActiveSolution() + \"'\" : \"\"}} and it's modules...</p>\n</div>"

/***/ }),

/***/ "./src/wpm/header/header.component.ts":
/*!********************************************!*\
  !*** ./src/wpm/header/header.component.ts ***!
  \********************************************/
/*! exports provided: HeaderComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "HeaderComponent", function() { return HeaderComponent; });
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _wpm_service__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ../wpm.service */ "./src/wpm/wpm.service.ts");
var __decorate = (undefined && undefined.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (undefined && undefined.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var HeaderComponent = /** @class */ (function () {
    function HeaderComponent(wpmService) {
        this.wpmService = wpmService;
    }
    HeaderComponent.prototype.ngOnInit = function () {
    };
    HeaderComponent.prototype.getActiveSolution = function () {
        return this.wpmService.getActiveSolution();
    };
    HeaderComponent.prototype.isNeedRefresh = function () {
        return this.wpmService.isNeedRefresh();
    };
    HeaderComponent = __decorate([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_0__["Component"])({
            selector: 'app-header',
            template: __webpack_require__(/*! ./header.component.html */ "./src/wpm/header/header.component.html"),
            styles: [__webpack_require__(/*! ./header.component.css */ "./src/wpm/header/header.component.css")]
        }),
        __metadata("design:paramtypes", [_wpm_service__WEBPACK_IMPORTED_MODULE_1__["WpmService"]])
    ], HeaderComponent);
    return HeaderComponent;
}());



/***/ }),

/***/ "./src/wpm/main.component.css":
/*!************************************!*\
  !*** ./src/wpm/main.component.css ***!
  \************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = ""

/***/ }),

/***/ "./src/wpm/main.component.html":
/*!*************************************!*\
  !*** ./src/wpm/main.component.html ***!
  \*************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<app-header class=\"wpm-header\"></app-header>\n<app-content class=\"wpm-content\"></app-content>\n\n"

/***/ }),

/***/ "./src/wpm/main.component.ts":
/*!***********************************!*\
  !*** ./src/wpm/main.component.ts ***!
  \***********************************/
/*! exports provided: MainComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "MainComponent", function() { return MainComponent; });
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
var __decorate = (undefined && undefined.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var MainComponent = /** @class */ (function () {
    function MainComponent() {
    }
    MainComponent = __decorate([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_0__["Component"])({
            selector: 'app-wpm',
            template: __webpack_require__(/*! ./main.component.html */ "./src/wpm/main.component.html"),
            styles: [__webpack_require__(/*! ./main.component.css */ "./src/wpm/main.component.css")]
        })
    ], MainComponent);
    return MainComponent;
}());



/***/ }),

/***/ "./src/wpm/packages/packages.component.css":
/*!*************************************************!*\
  !*** ./src/wpm/packages/packages.component.css ***!
  \*************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "th > div {\n    white-space: nowrap;\n\tfont-size: medium;\n    font-weight: bold;\n    background-color: rgb(248,248,248);\n    color: rgba(0,0,0,0.55);\n    padding: 16px;\n}\n\ntr.wpm-package-description > td {\n    padding-left: 60px;\n    padding-right: 0px;\n    padding-bottom: 0px;\n    padding-top: 0px;\n  }\n\ntr.wpm-package-description > td > mat-card-content {\n    display: block;\n    padding: 15px;\n    overflow: hidden;\n    background-color: rgb(240,240,240);\n}\n\n.table-package {\n    width: 100%;\n}\n\n.table-package th,\n.table-package td {\n    padding-right: 2px;\n    padding-left: 2px;\n}\n\n.table-package .md-avatar {\n    margin-top: 8px;\n    margin-bottom: 8px;\n    margin-right: 16px;\n    border-radius: 50%;\n    box-sizing: content-box;\n    width: 40px;\n    height: 40px;\n}\n\n.table-package md-select {\n    display: inline-block;\n}\n\n.table-package td > div {\n    display: flex;\n    justify-content: flex-start;\n    align-items: center;\n    min-height: 48px;\n    margin-left: 5px;\n    margin-right: 5px;\n    height: auto;\n}\n\n.table-package td > div.align-center {\n    justify-content: center;\n}\n\n.table-package td > div.align-right {\n    justify-content: flex-end;\n}\n\ntd.column-fitwidth {\n    width: 1%;\n    white-space: nowrap;\n}\n\n.table-package .release-action {\n    font-size: 24px;\n    float: right;\n}\n\n.table-package {\n    border-spacing: 0px;\n    padding: 2px;\n}\n\n.table-package th,\n.table-package td {\n    padding-right: 4px;\n    padding-left: 4px;\n}\n\n.wpm-package-selected {\n    background-color: rgb(210,210,210);\n}\n\n.wpm-clickable {\n    cursor: pointer;\n  }\n\n.wpm-clickable:focus {\n    outline: none;\n}"

/***/ }),

/***/ "./src/wpm/packages/packages.component.html":
/*!**************************************************!*\
  !*** ./src/wpm/packages/packages.component.html ***!
  \**************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<table style=\"width:100%\" class=\"table-package\">\n    <tr>\n      <th><div>Package</div></th>\n      <th colspan=\"2\"><div class=\"subheader\">Released version</div></th>  \n      <th><div>Added</div></th>\n      <th><div>In solution/module</div></th>  \n      <th><div>References</div></th>\n    </tr>\n    <ng-template ngFor let-package [ngForOf]=\"packages\">\n      <tr [ngClass]=\"isPackageSelected(package) ? 'wpm-package-selected' : ''\">\n        <td>\n          <div class=\"wpm-clickable\" matTooltip=\"{{ getDescriptionTooltip(package) }}\" matTooltipDisabled=\"{{ !package.description }}\" (click)=\"togglePackageSelection($event, package)\">\n            <img  [src]=\"package.icon\" class=\"md-avatar\" />\n            <span>{{ package.displayName }}</span>\n            <mat-icon *ngIf=\"package.description\">{{ isSelectedAndWithDescriptionExpanded(package) ? \"arrow_drop_up\" : \"arrow_drop_down\" }}</mat-icon>\n          </div>\n        </td>\n        <td class=\"column-fitwidth\">\n          <!-- Release Version-->\n          <div class=\"align-center\">\n            <div class= \"select\"> \n              <mat-form-field>\n                <mat-select [(value)]=\"package.selected\" [placeholder]=\"getSelectedRelease(package)\" matTooltip=\"{{ getReleaseTooltip(package) }}\">\n                  <mat-option *ngFor=\" let release of package.releases\" [value]=\"release.version\">{{ release.version }}</mat-option>\n                </mat-select>\n              </mat-form-field>\n            </div>\n          </div>\n        </td>\n        <td class=\"column-fitwidth\">\n          <!-- Add/Upgrade button -->\n          <div class=\"align-center btn-group\">\n            <button mat-icon-button *ngIf=\"installAvailable(package)\" [disabled]=\"isInstallingOrRemoving(package)\" (click)=\"install(package)\">\n              <mat-icon matTooltip=\"{{ getInstallTooltip(package) }}\">{{ package.installed ? \"update\" : \"add_circle_outline\" }}</mat-icon>\n            </button>\n          </div>\n        </td>\n        <td class=\"column-fitwidth\">\n          <!-- Added -->\n          <div class=\"align-center\"> \n            <p matTooltip=\"{{ getNotWPAAddedTooltip() }}\" matTooltipDisabled=\"{{ !package.installed || package.installedIsWPA }}\">{{ package.installed }}</p>\n            <button mat-icon-button *ngIf=\"canBeRemoved(package)\" [disabled]=\"isInstallingOrRemoving(package)\" (click)=\"uninstall(package)\">\n              <mat-icon matTooltip=\"{{ getRemoveTooltip(package) }}\">delete_outline</mat-icon>\n            </button>\n          </div>\n        </td>\n        <td class=\"column-fitwidth\">\n          <!-- Active Solution -->\n          <div class=\"align-center\">\n            <mat-select [(value)]=\"package.activeSolution\" [disabled]=\"!installEnabled(package)\" matTooltip=\"{{ getSolutionTooltip(package) }}\">\n              <mat-option *ngFor=\"let solution of getSolutions()\" [value]=\"solution\">{{ solution }}</mat-option>\n            </mat-select>\n          </div>\n        </td>\n        <td class=\"column-fitwidth\">\n          <!-- References -->\n          <div class=\"align-right\">  \n            <button mat-raised-button *ngIf=\"package.sampleUrl\" matTooltip=\"View demo\" (click)=\"showUrl(package.sampleUrl)\">Demo</button>\n            <button mat-icon-button *ngIf=\"package.wikiUrl\" matTooltip=\"Documentation\" (click)=\"showUrl(package.wikiUrl)\"><mat-icon>library_books</mat-icon></button>\n            <button mat-icon-button *ngIf=\"package.sourceUrl\" matTooltip=\"Source code\" (click)=\"showUrl(package.sourceUrl)\"><mat-icon>code</mat-icon></button>\n          </div>\n        </td> \n      </tr>\n      <tr *ngIf=\"isInstallingOrRemoving(package)\"> \n          <!-- Loading indicator -->\n          <td colspan=\"6\">\n            <mat-progress-bar mode=\"indeterminate\"></mat-progress-bar>\n          </td>\n      </tr>\n      <tr [hidden]=\"!isSelectedAndWithDescriptionExpanded(package)\" class=\"wpm-package-description\">\n            <!-- Description panel --> \n            <td colspan=\"6\">\n              <mat-card-content *ngIf=\"isSelectedAndWithDescriptionExpanded(package)\" [innerHTML]=\"getPackageDescription(package)\"></mat-card-content>\n            </td>\n      </tr>\n    </ng-template>\n  </table>"

/***/ }),

/***/ "./src/wpm/packages/packages.component.ts":
/*!************************************************!*\
  !*** ./src/wpm/packages/packages.component.ts ***!
  \************************************************/
/*! exports provided: PackagesComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "PackagesComponent", function() { return PackagesComponent; });
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _wpm_service__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ../wpm.service */ "./src/wpm/wpm.service.ts");
var __decorate = (undefined && undefined.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (undefined && undefined.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var PackagesComponent = /** @class */ (function () {
    function PackagesComponent(wpmService) {
        this.wpmService = wpmService;
    }
    PackagesComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.wpmService.getPackages().subscribe(function (p) {
            if (p.packageType == _this.packageList.type) {
                _this.packages = p.packages;
                _this.packageList.updateCount = _this.getUpgradeCount(_this.packages);
            }
        });
    };
    PackagesComponent.prototype.install = function (p) {
        this.wpmService.install(p);
    };
    PackagesComponent.prototype.uninstall = function (p) {
        this.wpmService.uninstall(p);
    };
    PackagesComponent.prototype.showUrl = function (url) {
        this.wpmService.showUrl(url);
    };
    PackagesComponent.prototype.getSelectedRelease = function (p) {
        if (!p.selected) {
            p.selected = p.releases[0].version;
        }
        return p.selected == p.releases[0].version ? "Latest" : "";
    };
    PackagesComponent.prototype.installAvailable = function (p) {
        return !p.installed || (p.installedIsWPA && !this.isLatestRelease(p) && p.selected > p.installed);
    };
    PackagesComponent.prototype.canBeRemoved = function (p) {
        return p.installed && p.installedIsWPA && (p.packageType != 'Solution');
    };
    PackagesComponent.prototype.isLatestRelease = function (p) {
        return p.installed == p.releases[0].version;
    };
    PackagesComponent.prototype.isInstallingOrRemoving = function (p) {
        return p.installing || p.removing;
    };
    PackagesComponent.prototype.installEnabled = function (p) {
        return !this.isInstallingOrRemoving(p) && this.installAvailable(p);
    };
    PackagesComponent.prototype.isPackageSelected = function (p) {
        return this.selectedPackage == p;
    };
    PackagesComponent.prototype.isSelectedAndWithDescriptionExpanded = function (p) {
        return this.isPackageSelected(p) && this.descriptionExpanded;
    };
    PackagesComponent.prototype.togglePackageSelection = function (event, p) {
        if (this.isPackageSelected(p)) {
            this.descriptionExpanded = !this.descriptionExpanded;
            if (this.descriptionExpanded)
                this.descriptionExpanded = !!p.description; // allow expand only if it has a description
        }
        else {
            this.selectedPackage = p;
            this.descriptionExpanded = !!p.description;
        }
        event.stopPropagation();
    };
    PackagesComponent.prototype.getPackageDescription = function (p) {
        // if (p.description) {
        //   return $sce.trustAsHtml(p.description);
        // }
        // return "";
        return p.description;
    };
    PackagesComponent.prototype.getSolutions = function () {
        return this.wpmService.getSolutions();
    };
    PackagesComponent.prototype.getUpgradeCount = function (packages) {
        var count = 0;
        try {
            for (var i = 0; i < packages.length; i++) {
                var pckg = packages[i];
                if (pckg.installed && pckg.installed < pckg.releases[0].version) {
                    count++;
                }
            }
        }
        catch (e) { }
        return count;
    };
    PackagesComponent.prototype.getReleaseTooltip = function (p) {
        if (p.installed) {
            return p.installedIsWPA ? "Version to upgrade to..." : "Released versions";
        }
        else {
            return "Version to add to the active solution or modules...";
        }
    };
    PackagesComponent.prototype.getInstallTooltip = function (p) {
        if (p.installed) {
            return p.installing ? "Upgrading the web package..." : "Upgrade the web package to the selected release version.";
        }
        else if (p.installing) {
            return "Adding the web package...";
        }
        else {
            return "Add the web package to solution '" + p.activeSolution + "'.";
        }
    };
    PackagesComponent.prototype.getRemoveTooltip = function (p) {
        return "Remove the web package from solution " + p.activeSolution;
    };
    PackagesComponent.prototype.getDescriptionTooltip = function (p) {
        return "Click to read more about package " + p.displayName;
    };
    PackagesComponent.prototype.getNotWPAAddedTooltip = function () {
        return "Package not added using Web Package Manager";
    };
    PackagesComponent.prototype.getSolutionTooltip = function (p) {
        if (p.installed) {
            return p.installing ? "Solution that will contain this upgrading package..." : "The solution that already contains/references this package.";
        }
        else if (p.installing) {
            return "Solution that will contain this package...";
        }
        else {
            return "Solution that this package will be added to if you press the 'Add' button.";
        }
    };
    __decorate([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_0__["Input"])(),
        __metadata("design:type", Object)
    ], PackagesComponent.prototype, "packageList", void 0);
    PackagesComponent = __decorate([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_0__["Component"])({
            selector: 'app-packages',
            template: __webpack_require__(/*! ./packages.component.html */ "./src/wpm/packages/packages.component.html"),
            styles: [__webpack_require__(/*! ./packages.component.css */ "./src/wpm/packages/packages.component.css")]
        }),
        __metadata("design:paramtypes", [_wpm_service__WEBPACK_IMPORTED_MODULE_1__["WpmService"]])
    ], PackagesComponent);
    return PackagesComponent;
}());



/***/ }),

/***/ "./src/wpm/websocket.service.ts":
/*!**************************************!*\
  !*** ./src/wpm/websocket.service.ts ***!
  \**************************************/
/*! exports provided: WebsocketService */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "WebsocketService", function() { return WebsocketService; });
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var rxjs__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! rxjs */ "./node_modules/rxjs/_esm5/index.js");
var __decorate = (undefined && undefined.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (undefined && undefined.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var WebsocketService = /** @class */ (function () {
    function WebsocketService() {
    }
    WebsocketService.prototype.connect = function (url) {
        if (!this.connection) {
            this.connection = this.create(url);
        }
        return this.connection;
    };
    WebsocketService.prototype.create = function (url) {
        var ws = new WebSocket(url);
        var openObservable = rxjs__WEBPACK_IMPORTED_MODULE_1__["Observable"].create(function (obs) {
            ws.onopen = obs.next.bind(obs);
            ws.onerror = obs.error.bind(obs);
            ws.onclose = obs.complete.bind(obs);
            return ws.close.bind(ws);
        });
        var messageObservable = rxjs__WEBPACK_IMPORTED_MODULE_1__["Observable"].create(function (obs) {
            ws.onmessage = obs.next.bind(obs);
            ws.onerror = obs.error.bind(obs);
            ws.onclose = obs.complete.bind(obs);
            return ws.close.bind(ws);
        });
        var messageObserver = {
            next: function (data) {
                if (ws.readyState === WebSocket.OPEN) {
                    ws.send(JSON.stringify(data));
                }
            }
        };
        return { open: openObservable, messages: rxjs__WEBPACK_IMPORTED_MODULE_1__["Subject"].create(messageObserver, messageObservable) };
    };
    WebsocketService = __decorate([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_0__["Injectable"])(),
        __metadata("design:paramtypes", [])
    ], WebsocketService);
    return WebsocketService;
}());



/***/ }),

/***/ "./src/wpm/wpm.module.ts":
/*!*******************************!*\
  !*** ./src/wpm/wpm.module.ts ***!
  \*******************************/
/*! exports provided: WpmModule */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "WpmModule", function() { return WpmModule; });
/* harmony import */ var _angular_platform_browser__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @angular/platform-browser */ "./node_modules/@angular/platform-browser/fesm5/platform-browser.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _main_component__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! ./main.component */ "./src/wpm/main.component.ts");
/* harmony import */ var _angular_platform_browser_animations__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! @angular/platform-browser/animations */ "./node_modules/@angular/platform-browser/fesm5/animations.js");
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var _header_header_component__WEBPACK_IMPORTED_MODULE_5__ = __webpack_require__(/*! ./header/header.component */ "./src/wpm/header/header.component.ts");
/* harmony import */ var _content_content_component__WEBPACK_IMPORTED_MODULE_6__ = __webpack_require__(/*! ./content/content.component */ "./src/wpm/content/content.component.ts");
/* harmony import */ var _packages_packages_component__WEBPACK_IMPORTED_MODULE_7__ = __webpack_require__(/*! ./packages/packages.component */ "./src/wpm/packages/packages.component.ts");
/* harmony import */ var _websocket_service__WEBPACK_IMPORTED_MODULE_8__ = __webpack_require__(/*! ./websocket.service */ "./src/wpm/websocket.service.ts");
/* harmony import */ var _wpm_service__WEBPACK_IMPORTED_MODULE_9__ = __webpack_require__(/*! ./wpm.service */ "./src/wpm/wpm.service.ts");
var __decorate = (undefined && undefined.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};










var WpmModule = /** @class */ (function () {
    function WpmModule() {
    }
    WpmModule = __decorate([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["NgModule"])({
            declarations: [
                _main_component__WEBPACK_IMPORTED_MODULE_2__["MainComponent"],
                _header_header_component__WEBPACK_IMPORTED_MODULE_5__["HeaderComponent"],
                _content_content_component__WEBPACK_IMPORTED_MODULE_6__["ContentComponent"],
                _packages_packages_component__WEBPACK_IMPORTED_MODULE_7__["PackagesComponent"]
            ],
            imports: [
                _angular_platform_browser__WEBPACK_IMPORTED_MODULE_0__["BrowserModule"],
                _angular_platform_browser_animations__WEBPACK_IMPORTED_MODULE_3__["BrowserAnimationsModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatButtonModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatTabsModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatSelectModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatOptionModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatIconModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatTooltipModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatCardModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_4__["MatProgressBarModule"]
            ],
            providers: [_websocket_service__WEBPACK_IMPORTED_MODULE_8__["WebsocketService"], _wpm_service__WEBPACK_IMPORTED_MODULE_9__["WpmService"]],
            bootstrap: [_main_component__WEBPACK_IMPORTED_MODULE_2__["MainComponent"]]
        })
    ], WpmModule);
    return WpmModule;
}());



/***/ }),

/***/ "./src/wpm/wpm.service.ts":
/*!********************************!*\
  !*** ./src/wpm/wpm.service.ts ***!
  \********************************/
/*! exports provided: PACKAGE_TYPE_WEB_COMPONENT, PACKAGE_TYPE_WEB_SERVICE, PACKAGE_TYPE_WEB_LAYOUT, PACKAGE_TYPE_SOLUTION, WpmService */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "PACKAGE_TYPE_WEB_COMPONENT", function() { return PACKAGE_TYPE_WEB_COMPONENT; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "PACKAGE_TYPE_WEB_SERVICE", function() { return PACKAGE_TYPE_WEB_SERVICE; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "PACKAGE_TYPE_WEB_LAYOUT", function() { return PACKAGE_TYPE_WEB_LAYOUT; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "PACKAGE_TYPE_SOLUTION", function() { return PACKAGE_TYPE_SOLUTION; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "WpmService", function() { return WpmService; });
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _websocket_service__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ./websocket.service */ "./src/wpm/websocket.service.ts");
/* harmony import */ var rxjs_operators__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! rxjs/operators */ "./node_modules/rxjs/_esm5/operators/index.js");
/* harmony import */ var rxjs__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! rxjs */ "./node_modules/rxjs/_esm5/index.js");
var __decorate = (undefined && undefined.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (undefined && undefined.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};




var PACKAGE_TYPE_WEB_COMPONENT = "Web-Component";
var PACKAGE_TYPE_WEB_SERVICE = "Web-Service";
var PACKAGE_TYPE_WEB_LAYOUT = "Web-Layout";
var PACKAGE_TYPE_SOLUTION = "Solution";
var WpmService = /** @class */ (function () {
    function WpmService(wsService) {
        var _this = this;
        this.needRefresh = false;
        this.refreshRemotePackages = function () {
            this.needRefresh = true;
        };
        var loc = window.location;
        //let uri = "ws://"+loc.host+"/wpm/angular2/websocket";
        var uri = "ws://localhost:8080/wpm/angular2/websocket";
        var webSocketConnection = wsService.connect(uri);
        webSocketConnection.open.subscribe(function () {
            _this.onConnectionOpen();
        });
        this.messages = webSocketConnection.messages.pipe(Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_2__["map"])(function (response) {
            var data = JSON.parse(response.data);
            return {
                method: data.method,
                data: data.result
            };
        }));
        this.messages.subscribe(function (m) {
            _this[m.method](m.data);
        });
        this.packagesObservable = rxjs__WEBPACK_IMPORTED_MODULE_3__["Observable"].create(function (obs) {
            _this.packagesObserver = obs;
        }).pipe(Object(rxjs_operators__WEBPACK_IMPORTED_MODULE_2__["share"])());
    }
    /**
     * Callback for when connection is established
     */
    WpmService.prototype.onConnectionOpen = function () {
        var requestAllInstalledPackagesCommand = { method: "requestAllInstalledPackages" };
        // get solution name
        var solutionName = decodeURIComponent((new RegExp('[?|&]solution=' + '([^&;]+?)(&|#|;|$)').exec(window.location.search) || [, ""])[1].replace(/\+/g, '%20')) || null;
        if (solutionName) {
            requestAllInstalledPackagesCommand.solution = solutionName;
        }
        this.messages.next(requestAllInstalledPackagesCommand);
        this.callRemoteMethod("getSolutionList");
        this.callRemoteMethod("getRepositories");
    };
    /**
     * Call remote method
     *
     * @param method name of the method
     */
    WpmService.prototype.callRemoteMethod = function (method) {
        var command = { method: method };
        this.messages.next(command);
    };
    WpmService.prototype.getPackages = function () {
        return this.packagesObservable;
    };
    WpmService.prototype.install = function (p) {
        p.installing = true;
        var command = { method: "install", package: p };
        this.messages.next(command);
    };
    WpmService.prototype.uninstall = function (p) {
        p.removing = true;
        var command = { method: "remove", package: p };
        this.messages.next(command);
    };
    WpmService.prototype.showUrl = function (url) {
        var command = { method: "showurl", url: url };
        this.messages.next(command);
    };
    WpmService.prototype.getSolutions = function () {
        // TODO: this should be a promise
        return this.solutions;
    };
    WpmService.prototype.getActiveSolution = function () {
        if (this.solutions && this.solutions.length) {
            return this.solutions[this.solutions.length - 1];
        }
        return "";
    };
    WpmService.prototype.isNeedRefresh = function () {
        return this.needRefresh;
    };
    /**
     * Remote method responses
     */
    WpmService.prototype.requestAllInstalledPackages = function (packagesArray) {
        var _this = this;
        var typeOfPackages = new Map();
        for (var i = 0; i < packagesArray.length; i++) {
            if (!typeOfPackages.has(packagesArray[i].packageType)) {
                typeOfPackages.set(packagesArray[i].packageType, []);
            }
            var packages = typeOfPackages.get(packagesArray[i].packageType);
            packages.push(packagesArray[i]);
        }
        typeOfPackages.forEach(function (pks, typ) {
            _this.packagesObserver.next({ packageType: typ, packages: pks });
        });
    };
    WpmService.prototype.getSolutionList = function (solutionsArray) {
        this.solutions = solutionsArray;
    };
    WpmService.prototype.getRepositories = function (repositoriesArray) {
        this.repositories = repositoriesArray;
    };
    WpmService = __decorate([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_0__["Injectable"])(),
        __metadata("design:paramtypes", [_websocket_service__WEBPACK_IMPORTED_MODULE_1__["WebsocketService"]])
    ], WpmService);
    return WpmService;
}());



/***/ }),

/***/ 0:
/*!***************************!*\
  !*** multi ./src/main.ts ***!
  \***************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__(/*! /home/gabi/github_master/servoy-eclipse/com.servoy.eclipse.designer.wpm/src/wpm2/src/main.ts */"./src/main.ts");


/***/ })

},[[0,"runtime","vendor"]]]);
//# sourceMappingURL=main.js.map