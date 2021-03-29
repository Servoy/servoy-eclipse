import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, HostListener, Input, OnDestroy, Renderer2, SimpleChanges, ViewChild } from "@angular/core";
import { ServoyBaseComponent } from "../../ngclient/basecomponent";
import { IActionMapping, ITreeOptions, TreeComponent, TreeNode} from '@circlon/angular-tree-component'
import { LoggerService, LoggerFactory } from "../../sablo/logger.service";
import { ApplicationService } from "../../ngclient/services/application.service";
import { ServoyService } from "../../ngclient/servoy.service";
import { ITreeNode } from "@circlon/angular-tree-component/lib/defs/api";
import { LocalStorageService } from "../../sablo/webstorage/localstorage.service";
import { Foundset, FoundsetChangeEvent } from "../../ngclient/converters/foundset_converter";

interface FoundsetListener {
  listener: () => void,
  foundsetId: number
}

@Component({
    selector: 'servoyextra-dbtreeview', 
    templateUrl: './dbtreeview.html', 
    changeDetection: ChangeDetectionStrategy.OnPush
})

export class ServoyExtraDbtreeview extends ServoyBaseComponent<HTMLDivElement> implements OnDestroy { 

    @Input() levelVisibilityChange;
    @Input() foundsets: any[]; 
    @Input() relatedFoundsets: any[];
    @Input() roots;
    @Input() autoRefresh;
    @Input() bindings;
    @Input() cssPosition;
    @Input() enabled;
    @Input() levelVisibility;
    @Input() location;
    @Input() responsiveHeight;
    @Input() selection: Array<any>
    @Input() size; 
    @Input() visible: boolean;
    @Input() onReady: (e: Event, data?: any) => void;

    log: LoggerService;
    folderImgPath = "../../assets/images/folder.png"
    fileImgPath = "../../assets/images/file.png"
    useCheckboxes = false;
    expandedNodes: any = [];
    displayNodes = [];
    removeListenerFunctionArray: Array<FoundsetListener> = [];

    @ViewChild('element', { static: true }) elementRef: ElementRef;
    @ViewChild('tree', { static: true }) tree: TreeComponent;
  
    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef,logFactory: LoggerFactory,
       private applicationService: ApplicationService,
       private servoyService: ServoyService,
       private localStorage: LocalStorageService) {
      super(renderer, cdRef);
      this.log = logFactory.getLogger('ServoyExtraDbtreeview');
    }

    @HostListener('window:beforeunload', ['$event'])
    onBeforeUnloadHander() {
      // TODO: find a way to store the inner children (i.d. the array of children)
      // this.localStorage.set('dbtreeview', JSON.stringify(this.displayNodes));
    }

    actionMapping: IActionMapping = {
      mouse: {
        click: (tree, node) => { 
          this.check(node, !node.data.checked);
          if(node.data && node.data.callbackinfo) {
            const doubleClick = node.data.callbackinfo;
            this.servoyService.executeInlineScript(doubleClick.formname, doubleClick.script, [doubleClick.callbackinfoParamValue]);
          }
        },
        dblClick: (tree, node) => {
          if(node.data && node.data.methodToCallOnDoubleClick) {
            const doubleClick = node.data.methodToCallOnDoubleClick;
            this.servoyService.executeInlineScript(doubleClick.formname, doubleClick.script, [doubleClick.methodToCallOnDoubleClickParamValue]);
          }
        }, 
        contextMenu: (tree, node) => {
          if(node.data && node.data.methodToCallOnRightClick) {
            const doubleClick = node.data.methodToCallOnRightClick;
            this.servoyService.executeInlineScript(doubleClick.formname, doubleClick.script, [doubleClick.methodToCallOnRightClickParamValue]);
          }
        }
      }
    };

    options: ITreeOptions = {
      getChildren: this.getChildren.bind(this),
      actionMapping: this.actionMapping
    }

    svyOnInit() {
        super.svyOnInit();
        if (this.localStorage.has('dbtreeview')) {
          this.displayNodes = JSON.parse(this.localStorage.get('dbtreeview'));
        }
        if (!this.displayNodes || this.displayNodes.length === 0) { this.initTree() } 

        if (this.foundsets) {
          this.foundsets.forEach(foundsetInfo => {
            this.addFoundsetListener(foundsetInfo.foundset);
          });
        }
    }

    svyOnChanges(changes: SimpleChanges) {
      if (changes) {
        for (const property of Object.keys(changes)) {
            const change = changes[property];
            switch (property) {
                case 'responsiveHeight':
                    if(this.elementRef && this.servoyApi.isInAbsoluteLayout() && change.currentValue ) {
                      this.renderer.setStyle(this.elementRef, 'height', change.currentValue + 'px');
                    }
                    break;
                case 'foundsets': {
                  if (change.currentValue) {
                    this.initTree();
                  }
                  this.addOrRemoveFoundsetListeners(change);
                  break;
                }
                case 'relatedFoundsets': {
                  this.addOrRemoveFoundsetListeners(change);
                  break;
                }
                case 'selection': {
                  this.selectNode(change.currentValue);
                  break;
                }
                case 'levelVisibility': {
                  if (change.currentValue && this.tree) {
                    this.expandChildNodes(this.tree.treeModel.virtualRoot, change.currentValue.level, change.currentValue.state);
                    break;
                  }
                }
                case 'enabled': {
                  if(change.previousValue !== change.currentValue && this.elementRef) {
                    if(change.currentValue) {
                      this.renderer.removeClass(this.elementRef.nativeElement, "dbtreeview-disabled");
                    } else {
                      this.renderer.addClass(this.elementRef.nativeElement, "dbtreeview-disabled");
                    }
                  }
                  break;
                }
              }
            }
          }
    }

    ngOnDestroy() {
      if (this.removeListenerFunctionArray != null) {
        this.removeListenerFunctionArray.forEach(removeListenerFunction => {
          removeListenerFunction.listener();
        })
        this.removeListenerFunctionArray = null;
      }
      if (this.localStorage.has('dbtreeview')) {
        this.localStorage.remove('dbtreeview');
      }
      super.ngOnDestroy();
    }

    private initTree(): void {
      let children = [];
      this.displayNodes = [];
      if (this.foundsets) {
        this.foundsets.forEach((elem) => {
          elem.foundset.viewPort.rows.forEach((row, index) => {
            let child = this.buildChild(row, elem, index, 1, null);
            children.push(child);
          });
      }, this);
      this.displayNodes = children;
      this.cdRef.detectChanges();
      }
    }

    async getChildren(node: TreeNode) {
      let children = [];
      for (let index = 0; index < this.relatedFoundsets.length; index++) {
          if (this.relatedFoundsets[index].foundsetInfoParentID === node.data.foundsetInfoID 
            && this.relatedFoundsets[index].indexOfTheParentRecord === node.data.indexOfTheParentRecord) {

            // load the next round of related foundsets
            await this.servoyApi.callServerSideApi('loadRelatedFoundset', [index]);

            const rows = this.relatedFoundsets[index].foundset.viewPort.rows;
            for(let rowIndex = 0; rowIndex < rows.length; rowIndex++ ) {
              children.push(this.buildChild(rows[rowIndex], this.relatedFoundsets[index], rowIndex, node.data.level + 1, node));
            }
            break;
         }
      }
      return children;
    }

    onLoadNodeChildren(event: any) {
      event.node.children.forEach((child: ITreeNode) => {
        if(child.data.active) {
          child.setIsActive(true);
        }
      });
    }

    onNodeExpanded(event: any) {
      event.node.data["expanded"] = event.isExpanded;
    }

    onTreeLoad(event) {
      this.expandNodes(this.displayNodes);
      if (this.onReady) {
        this.onReady(event);
      }
    }

    expandNodes(nodes) {
      nodes.forEach(node => {
        if (node.expanded) {
          this.tree.treeModel.getNodeById(node.id).setIsExpanded(true);
        }
        if (node.hasChildren && node.children) this.expandNodes(node.children);
      });
    }

    check(node, checked) {
      this.updateChildNodeCheckbox(node, checked);
      this.updateParentNodeCheckbox(node.realParent);

      // if(data.node.data.checkboxvaluedataprovider) {
      //   var v = data.node.selected
      //   if("number" == data.node.data.checkboxvaluedataprovidertype) {
      //     v = v ? 1 : 0;
      //   } else if ("string" == data.node.data.checkboxvaluedataprovidertype) {
      //     v = v ? 'true' : 'false'
      //   }
      //   foundset_manager.updateFoundSetRow(
      //       data.node.key.substring(0, data.node.key.lastIndexOf('_')),
      //       data.node.data._svyRowId,
      //       data.node.data.checkboxvaluedataprovider,
      //       v);
      // }

      if(node.data && node.data.methodToCallOnCheckBoxChange) {
        const checkboxChange = node.data.methodToCallOnCheckBoxChange;
        this.servoyService.executeInlineScript(checkboxChange.formname, checkboxChange.script, [checkboxChange.methodToCallOnCheckBoxChangeParamValue]);
      }
    }

    updateChildNodeCheckbox(node, checked) {
      node.data.checked = checked;
      if (node.children) {
        node.children.forEach((child) => this.updateChildNodeCheckbox(child, checked));
      }
    }

    updateParentNodeCheckbox(node) {
      if (!node) {
        return;
      }
      let allChildrenChecked = true;
      let noChildChecked = true;
  
      for (const child of node.children) {
        if (!child.data.checked || child.data.indeterminate) {
          allChildrenChecked = false;
        }
        if (child.data.checked) {
          noChildChecked = false;
        }
      }
  
      if (allChildrenChecked) {
        node.data.checked = true;
        node.data.indeterminate = false;
      } else if (noChildChecked) {
        node.data.checked = false;
        node.data.indeterminate = false;
      } else {
        node.data.checked = true;
        node.data.indeterminate = true;
      }
      this.updateParentNodeCheckbox(node.parent);
    }

    private buildChild(row: any, foundsetInfo: any, index: number, level: number, parent: TreeNode) {
      let child : {parent?: TreeNode, id?: string, name?: string, hasChildren?: boolean, image?: string,
      checkbox?: boolean, checked?: boolean, active?: boolean, children?: Array<any>, tooltip?: string};

      child = { id: foundsetInfo.foundsetInfoID + '_' + row[foundsetInfo.foundsetPK],
       name: row.text, hasChildren: this.hasChildren(foundsetInfo, index) };

       // save info about parent 
      if (child.hasChildren) {
        child["indexOfTheParentRecord"] = index;
        child["foundsetInfoID"] = foundsetInfo.foundsetInfoID;
      }

      child["parent"] = parent;
      child["level"] = level;

      const binding = this.getBinding(foundsetInfo.datasourceID);
      if (binding.imageurldataprovider) {
        child["image"] = this.getIconURL(foundsetInfo.foundset.viewPort.rows[index][binding.imageurldataprovider]);
      } else if (child.hasChildren) {
        child["image"] = this.folderImgPath;
      } else {
        child["image"] = this.fileImgPath;
      }

      if (binding.tooltiptextdataprovider) {
        child["tooltip"] = foundsetInfo.foundset.viewPort.rows[index][binding.tooltiptextdataprovider];
      }

      if (binding.checkboxvaluedataprovider) {
        child["checkbox"] = Boolean(foundsetInfo.foundset.viewPort.rows[index][binding.hascheckboxdataprovider]);
      }
      else if (binding.hasCheckboxValue) {
        child["checkbox"] = binding.hasCheckboxValue.indexOf("" + foundsetInfo.foundset.viewPort.rows[index][foundsetInfo.foundsetPK]) != -1;
      }
      else {
        child["checkbox"] = Boolean(binding.initialCheckboxValues);
      }

      if (child["checkbox"]) {
        if(parent && parent.data.checked) {
          child["checked"] = true;
        }
        if (binding.checkboxvaluedataprovider) {
          child["checked"] = Boolean(foundsetInfo.foundset.viewPort.rows[index][binding.checkboxvaluedataprovider]);
        }
        else if (binding.initialCheckboxValues) {
          child["checked"] = binding.initialCheckboxValues.indexOf("" + foundsetInfo.foundset.viewPort.rows[index][foundsetInfo.foundsetPK]) != -1;
        }
      }

      const isLevelVisible = this.levelVisibility && this.levelVisibility.state && (this.levelVisibility.level == level);
      var isNodeExpanded = (level <= this.expandedNodes.length) && (this.expandedNodes[level - 1].toString() == this.getPKFromNodeID(child.id));

      if (isLevelVisible || isNodeExpanded) {
        child["expanded"] = true;
      }

      if(this.isNodeSelected(child, this.selection)) {
        child["active"] = true;
      }

      if(binding.nRelationInfos && binding.nRelationInfos.length > 0) {
        child["image"] = this.folderImgPath;
        child["children"] = new Array();
        for(let j = 0; j < binding.nRelationInfos.length; j++) {
          let relationItem: {name: string, checkbox: boolean, image: string, hasChildren: boolean, id: string} = {
               name: binding.nRelationInfos[j].label,
               checkbox: true,
               hasChildren: true,
               image: this.folderImgPath,
               id: row.node_id
             };

          child.children.push(relationItem);
        }

      }

      if(binding.callbackinfo || binding.methodToCallOnCheckBoxChange || binding.methodToCallOnDoubleClick || binding.methodToCallOnRightClick)
          {
            if(binding.callbackinfo) {
              child["callbackinfo"] = binding.callbackinfo.f;
              child["callbackinfoParamValue"] = foundsetInfo.foundset.viewPort.rows[index][binding.callbackinfo.param];
            }
            if(binding.methodToCallOnCheckBoxChange) {
              child["methodToCallOnCheckBoxChange"] = binding.methodToCallOnCheckBoxChange.f;
              child["methodToCallOnCheckBoxChangeParamValue"] = foundsetInfo.foundset.viewPort.rows[index][binding.methodToCallOnCheckBoxChange.param];
            }
            if(binding.methodToCallOnDoubleClick) {
              child["methodToCallOnDoubleClick"] = binding.methodToCallOnDoubleClick.f;
              child["methodToCallOnDoubleClickParamValue"] = foundsetInfo.foundset.viewPort.rows[index][binding.methodToCallOnDoubleClick.param];
            }    				    				
            if(binding.methodToCallOnRightClick) {
              child["methodToCallOnRightClick"] = binding.methodToCallOnRightClick.f;
              child["methodToCallOnRightClickParamValue"] = foundsetInfo.foundset.viewPort.rows[index][binding.methodToCallOnRightClick.param];
            }    				    				
          }

      return child;
    }

    hasChildren(foundsetInfo: any, index: number) {
      let hasChildren = false;
      for(let fsInfo of this.relatedFoundsets) {
        hasChildren = (foundsetInfo.foundsetInfoID === fsInfo.foundsetInfoParentID && index === fsInfo.indexOfTheParentRecord);
        if(hasChildren) break;
      }
      return hasChildren;
    }

    public refresh(): void {
      this.initTree();
    }

    public isNodeExpanded(pk: Array<number>): boolean {
      if (this.tree) {
          const node = this.findNode(this.tree.treeModel.virtualRoot, pk, 0);
          if(node) {
	  				return node.isExpanded;
	  			}
      }
      return false;
    }

    public setExpandNode(pk: Array<any>, state: boolean): void {
      if (this.tree) {
        if (pk && pk.length) {
          if(state) {
            this.expandedNodes = pk.slice(0, pk.length);
          }
          const node = this.tree ? this.findNode(this.tree.treeModel.virtualRoot, pk, 0) : null;
          if(node) {
            this.tree.treeModel.setExpandedNode(node, state);
          }
        }
      }
    }

    public getSelectionPath(): Array<any> {
      return this.selection;
    }

    public updateCheckBoxValues(datasource, pk, state): boolean {
      return false;
    }

    public getCheckBoxValues(datasource) {}

    findNode(node: TreeNode, pkarray: Array<any>, level: number) {
      if(pkarray && pkarray.length > 0) {
        const nodeChildren = node.children;
        if(nodeChildren) {
          for(let i = 0; i < nodeChildren.length; i++) {
            if(this.getPKFromNodeID(nodeChildren[i].id) == pkarray[level].toString()) {
              if(level + 1 < pkarray.length) {
                return this.findNode(nodeChildren[i], pkarray, level + 1);
              }
              else {
                return nodeChildren[i];
              }
            }
          }
        }
      }
      return null;
    }

    expandChildNodes(node: TreeNode, level: number, state: boolean) {
      if(level >= 1) {
        let nodeChildren = node.children;
        nodeChildren.forEach(child => {
          if (state) {
            child.setActiveAndVisible(true);
            child.setIsExpanded(state);
          } else if (level === 1) {
            child.setIsExpanded(state);
          }
        });
      }
    }

    isNodeSelected(node: any, selection: Array<any>) {
			if(selection && selection.length) {
				const nodePKPath = [];
				nodePKPath.unshift(this.getPKFromNodeID(node.id));

				let parentNode = node.parent;
				while(parentNode) {
          if (parentNode !== this.tree.treeModel.virtualRoot) {
            nodePKPath.unshift(this.getPKFromNodeID(parentNode.data.id));
            parentNode = parentNode.parent;
          } else {
            break;
          }
				}
				if(nodePKPath.length == selection.length) {
					for(let i = 0; i < nodePKPath.length; i++) {
						if(nodePKPath[i] != selection[i].toString()) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
		}		

    selectNode(selection: Array<any>) {
			if(selection && selection.length) {
				this.expandedNodes = selection.slice(0, selection.length);
        const node = this.findNode(this.tree.treeModel.virtualRoot, selection, 0);
				if(node && !node.isActive) {
					node.setActiveAndVisible(true);
				}
				else {
					this.refresh();
				}
	    }
  	}

    getPKFromNodeID(nodeID: string) {
			return nodeID.substring(nodeID.indexOf('_') + 1);
		}

    private getBinding(datasource) {
      for(let i = 0; i < this.bindings.length; i++) {
        if(datasource == this.bindings[i].datasource) {
          return this.bindings[i];
        }
      }
      return null;
    }

    getIconURL(iconPath: string) {
      if(iconPath && iconPath.indexOf("media://") == 0) {
        return "resources/fs/" + this.applicationService.getSolutionName() + iconPath.substring(8);
      }
      return iconPath;
    }

    private addOrRemoveFoundsetListeners(change) {
      if (change.currentValue) {
        change.currentValue.forEach(fsInfoCV => {
          if (change.previousValue && change.previousValue.length > 0) {
            if (!change.previousValue.find(fsInfoPV => {
              fsInfoCV.foundset.foundsetId === fsInfoPV.foundset.foundsetId;
            })) {
              this.addFoundsetListener(fsInfoCV.foundset);
            }
          } else {
            this.addFoundsetListener(fsInfoCV.foundset);
          }
        });
      }

      if (change.previousValue) {
        change.previousValue.forEach(fsInfoPV => {
          if (change.currentValue && change.currentValue.length > 0) {
            if (!change.currentValue.find(fsInfoCV => {
              fsInfoPV.foundset.foundsetId === fsInfoCV.foundset.foundsetId;
            })) {
              this.removeFoundsetListener(fsInfoPV.foundset);
            }
          } else {
            this.removeFoundsetListener(fsInfoPV.foundset);
          }
        });
      }
    }

    private addFoundsetListener(foundset: Foundset) {
      if (!this.removeListenerFunctionArray.find(listener => {
        listener.foundsetId === foundset.foundsetId;
      })) {
        this.removeListenerFunctionArray.push({listener: foundset.addChangeListener((event: FoundsetChangeEvent) => {
          if (event.viewportRowsUpdated || event.viewportRowsCompletelyChanged || event.serverFoundsetSizeChanged) {
            this.initTree();
          }

          // FIXME: when using the 'test_webcomponents' solution this event is triggered continuously
          // if (event.fullValueChanged && event.fullValueChanged.oldValue !== event.fullValueChanged.newValue) {
          //   this.initTree();
          // }
        }), foundsetId: foundset.foundsetId});
      }
    }

    private removeFoundsetListener(foundset: Foundset) {
      const fsListener = this.removeListenerFunctionArray.find(el => el.foundsetId === foundset.foundsetId);
      if (fsListener) fsListener.listener();
      this.removeListenerFunctionArray = this.removeListenerFunctionArray.filter(el => !(el.foundsetId === foundset.foundsetId));
    }

}