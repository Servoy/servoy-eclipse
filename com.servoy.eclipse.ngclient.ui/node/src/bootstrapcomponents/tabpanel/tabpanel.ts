import { Component, Renderer2, Input, Output, EventEmitter, ViewChild, SimpleChanges, ElementRef,ContentChild, TemplateRef } from '@angular/core';
import { BaseCustomObject } from '../../sablo/spectypes.service';
import { WindowRefService } from '../../sablo/util/windowref.service';

import { ServoyBootstrapBaseComponent } from '../bts_basecomp';
import { NgbTabChangeEvent } from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'servoybootstrap-tabpanel',
  templateUrl: './tabpanel.html',
  styleUrls: ['./tabpanel.scss']
})
export class ServoyBootstrapTabpanel extends ServoyBootstrapBaseComponent {

  @Input() onChangeMethodID;
  @Input() onTabClickedMethodID;
  @Input() onTabCloseMethodID;
  
  @Input() visible;
  @Input() height;
  @Input() tabs: Array<Tab>
  @Input() showTabCloseIcon;
  @Input() closeIconStyleClass;
  
  @Input() tabIndex;
  @Output() tabIndexChange = new EventEmitter();
  
  // this is a hack so that this element is done none statically (because it is nested in a view that is later visible)
  @ViewChild('element') elementRef:ElementRef;
  
  @ContentChild( TemplateRef  , {static: true})
  templateRef: TemplateRef<any>;
  
  private selectedTab: Tab;
  
  private waitingForServerVisibility = {};
  private lastSelectedTab: Tab;
  
  constructor(renderer: Renderer2,private windowRefService: WindowRefService) {
      super(renderer);
   }

  ngOnChanges( changes: SimpleChanges ) {
      if ( changes["tabs"] ) {
          // quickly generate the id's for a the tab html id (and selecting it)
          for ( let i = 0; i < this.tabs.length; i++ ) {
              this.tabs[i]._id = this.servoyApi.getMarkupId() + "_tab_" + i;
          }
      }
      if ( changes["tabIndex"] ) {
          Promise.resolve( null ).then(() => { this.select( this.tabs[this.getRealTabIndex()] ) } );
      }
      super.ngOnChanges(changes);
  }
  
  getForm( tab: Tab ) {
      if ( !this.selectedTab ) {
          const tabIndex = this.getRealTabIndex();
          if ( tabIndex >= 0 ) this.select( this.tabs[tabIndex] );

          if ( !this.selectedTab && this.tabs.length ) {
              this.select( this.tabs[0] );
          }
      }
      if ( this.selectedTab && ( tab.containedForm == this.selectedTab.containedForm ) && ( tab.relationName == this.selectedTab.relationName ) ) {
            return tab.containedForm;
      }
      return null;
  }

  onTabChange( event: NgbTabChangeEvent ) {
      // do prevent it by default, so that hte server side can decide of the swich can happen.
      event.preventDefault();
  }
  
  tabClicked(tab: Tab,tabIndexClicked : number, event){
      if (event.target.classList.contains('bts-tabpanel-close-icon')) {
          if (this.onTabCloseMethodID)
          {
              const promise = this.onTabCloseMethodID( this.windowRefService.nativeWindow.event != null ? this.windowRefService.nativeWindow.event : null /* TODO $.Event("tabclicked") */ ,tabIndexClicked + 1);
              promise.then(( ok ) => {
                  if ( ok ) {
                      this.removeTabAt(tabIndexClicked+1)
                  }
              } )
          }
          else
          {
              this.removeTabAt(tabIndexClicked+1)  
          }    
      }
      else {
          if (tab.disabled === true) {
              return;
          }
          
          if (this.onTabClickedMethodID) {
              /*var dataTargetAttr = $(event.target).closest('[data-target]');
              var dataTarget = dataTargetAttr ? dataTargetAttr.attr('data-target') : null;*/
              const promise =  this.onTabClickedMethodID(this.windowRefService.nativeWindow.event != null ? this.windowRefService.nativeWindow.event : null /*$.Event("tabclicked")*/, tabIndexClicked + 1, null)
              promise.then(( ok ) => {
                  if ( ok ) {
                      this.select( this.tabs[tabIndexClicked] ); 
                  }
              } );
          } else {
              this.select( this.tabs[tabIndexClicked] ); 
          }
      }    
  }

  select( tab: Tab ) {
      if ( !this.visible ) return;
      if ( this.isValidTab( tab ) ) {
          if ( ( tab != undefined && this.selectedTab != undefined && tab.containedForm == this.selectedTab.containedForm && tab.relationName == this.selectedTab.relationName ) || ( tab == this.selectedTab ) ) return;
          if ( this.selectedTab ) {
              if ( this.selectedTab.containedForm && !this.waitingForServerVisibility[this.selectedTab.containedForm] ) {
                  const formInWait = this.selectedTab.containedForm;
                  this.waitingForServerVisibility[formInWait] = true;
                  const currentSelectedTab = this.selectedTab;
                  this.lastSelectedTab = tab;
                  const promise = this.servoyApi.hideForm( this.selectedTab.containedForm, null, null, tab.containedForm, tab.relationName );
                  promise.then(( ok ) => {
                      delete this.waitingForServerVisibility[formInWait];
                      if ( this.lastSelectedTab != tab ) {
                          // visibility changed again, just ignore this
                          // it could be that the server was sending the correct state in the mean time already at the same time 
                          // we try to hide it. just call show again to be sure.
                          if ( currentSelectedTab == this.selectedTab ) this.servoyApi.formWillShow( this.selectedTab.containedForm, this.selectedTab.relationName );
                          return;
                      }
                      if ( ok ) {
                          this.setFormVisible( tab );
                      }
                  } )
              }
          }
          else {
              this.setFormVisible( tab );
          }
      }
  }
  
  getRealTabIndex(): number {
      if ( this.tabIndex ) {
          if ( isNaN( this.tabIndex ) ) {
              if (!this.tabs) return -1;
              for ( let i = 0; i < this.tabs.length; i++ ) {
                  if (this.tabs[i].name == this.tabIndex) {
                      this.tabIndex = i +1
                      this.tabIndexChange.emit(i);
                      return i;
                  }
              }
              return -1;
          }
          return this.tabIndex - 1;
      }
      if ( this.tabs && this.tabs.length > 0 ) return 0;
      return -1;
  }
  
  isValidTab( tab: Tab ) {
      if ( this.tabs ) {
          for ( var i = 0; i < this.tabs.length; i++ ) {
              if ( this.tabs[i] === tab ) {
                  return true;
              }
          }
      }
      return false;
  }
  
  setFormVisible( tab: Tab ) {
      if ( tab.containedForm ) this.servoyApi.formWillShow( tab.containedForm, tab.relationName );
      var oldSelected = this.selectedTab;
      this.selectedTab = tab;
      this.tabIndex = this.getTabIndex( this.selectedTab );
      this.tabIndexChange.emit( this.tabIndex );
      if ( oldSelected && oldSelected != tab && this.onChangeMethodID ) {
          setTimeout(() => {
              this.onChangeMethodID( this.getTabIndex( oldSelected ), this.windowRefService.nativeWindow.event != null ? this.windowRefService.nativeWindow.event : null /* TODO $.Event("change") */ );
          }, 0, false );
      }
  }
  
  private getTabIndex( tab: Tab ) {
      if ( tab ) {
          for ( var i = 0; i < this.tabs.length; i++ ) {
              if ( this.tabs[i] == tab ) {
                  return i + 1;
              }
          }
      }
      return -1;
  }
  
  getSelectedTabId() {
      if ( this.selectedTab ) return this.selectedTab._id;
      const tabIndex = this.getRealTabIndex();
      if (tabIndex > 0) {
          return this.tabs[tabIndex]._id;
      }
      else if (this.tabs && this.tabs.length > 0) return this.tabs[0]._id;
  }
  
  removeTabAt(removeIndex:number) {
      // copied from the serverside code
      if (removeIndex > 0 && removeIndex <= this.tabs.length) {
          let oldTabIndex = this.tabIndex;
          let formToHide;
          let formToShow;
          if (this.tabIndex === removeIndex) {
              formToHide = this.tabs[removeIndex - 1];

              var nextIndex = this.getFirstEnabledTabIndexNotAtIndex(this.tabIndex)
              // if the tabIndex after removal will remain the same after removal, shall force showForm
              if ((nextIndex > -1 && nextIndex === this.tabIndex + 1) && this.tabs.length > 1) {
                  // get the tab at second position
                  formToShow = this.tabs[nextIndex - 1];
              }
          }

          // remove the tab
          // create a new tabObject, so angular-ui is properly refreshed.
          var newTabs = [];
          for (var i = 0; i < this.tabs.length; i++) {
              if (i == removeIndex - 1) continue;
              newTabs.push(this.tabs[i]);
          }
          this.tabs = newTabs;
          

          // update the tabIndex
          if (this.tabIndex >= removeIndex) {
              if (this.tabIndex === removeIndex) {
                  var newTabIndex = this.getFirstEnabledTabIndex();
                  if (newTabIndex > - 1) {
                      this.tabIndex = newTabIndex;
                  } else {
                      // deselect all tabs setting tabIndex to 0
                      this.tabIndex = 0;
                      newTabIndex = 0;
                  }
              } else {
                  this.tabIndex--;
              }
          }

          // hide the form
          if (formToHide) {
              // hide the current form
              if (formToHide.containedForm && !formToShow) {
                  // TODO what if doesn't hide ?
                  this.servoyApi.hideForm(formToHide.containedForm);
              }

              // show the next form if the tabIndex was 1 and has not changed
              if (formToShow && formToShow.containedForm) {
                  // This will happen only when the first tab is the visible tab and i am closing the first tab.
                  // The previous tab already call the onHide.. here i force the onShow of the "next" tab.. since the $scope.model.tabIndex doesn't change
                  // Using ng-repeat="tab in model.tabs track by $index" to make angularui aware of the change.
                  
                  this.servoyApi.formWillShow(formToShow.containedForm, formToShow.relationName);
                  if (this.onChangeMethodID) {
                      setTimeout(() => {
                          this.onChangeMethodID( 1 , this.windowRefService.nativeWindow.event != null ? this.windowRefService.nativeWindow.event : null /* TODO $.Event("change") */ );
                      }, 0, false );
                  } 
              }
          }
      }
  }
  
  getFirstEnabledTabIndexNotAtIndex (skipIndex : number) {
      for (var i = 0; this.tabs && i < this.tabs.length; i++) {
          var tab = this.tabs[i];
          if (tab.disabled !== true && (skipIndex !== i +1 )) {
              return i + 1;
          }
      }
      return -1;
  }
  
  getFirstEnabledTabIndex() {
      for (var i = 0; this.tabs && i < this.tabs.length; i++) {
          var tab = this.tabs[i];
          if (tab.disabled !== true) {
              return i + 1;
          }
      }
      return -1;
  }
}

export class Tab extends BaseCustomObject {
    _id: string;
    name: string;
    containedForm: string;
    text: string;
    relationName: string;
    disabled: boolean;
    imageMediaID: string;
    hideCloseIcon: boolean;
    iconStyleClass:string;
}
