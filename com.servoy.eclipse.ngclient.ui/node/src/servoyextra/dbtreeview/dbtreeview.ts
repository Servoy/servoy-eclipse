import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, HostListener, Input, OnDestroy, Renderer2, SimpleChanges, ViewChild } from "@angular/core";
import { ServoyBaseComponent } from "@servoy/public";
import { IActionMapping, ITreeOptions, TreeComponent, TreeNode} from '@circlon/angular-tree-component'
import { LoggerService, LoggerFactory } from "@servoy/public";
import { ApplicationService } from "../../ngclient/services/application.service";
import { ServoyService } from "../../ngclient/servoy.service";
import { ITreeNode } from "@circlon/angular-tree-component/lib/defs/api";
import { LocalStorageService } from "../../sablo/webstorage/localstorage.service";
import { Foundset, FoundsetChangeEvent } from "../../ngclient/converters/foundset_converter";
import { BaseCustomObject } from "../../sablo/spectypes.service";
import { isEqual } from 'lodash-es';

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

    @Input() foundsets: Array<FoundsetInfo>; 
    @Input() relatedFoundsets: Array<FoundsetInfo>;
    @Input() autoRefresh: boolean;
    @Input() bindings: Array<Binding>;
    @Input() enabled: boolean;
    @Input() levelVisibility: LevelVisibilityType;
    @Input() responsiveHeight: number;
    @Input() selection: Array<Object>;
    @Input() visible: boolean;
    @Input() onReady: (e: Event, data?: any) => void;

    log: LoggerService;
    folderImgPath = "../../assets/images/folder.png"
    fileImgPath = "../../assets/images/file.png"
    useCheckboxes = false;
    expandedNodes: any = [];
    displayNodes: any = [];
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
          this.tree.treeModel.setActiveNode(node, true);
          if(node.data && node.data.callbackinfo) {
            const doubleClick = node.data.callbackinfo;
            this.servoyService.executeInlineScript(doubleClick.formname, doubleClick.script, [node.data.callbackinfoParamValue]);
          }
        },
        dblClick: (tree, node) => {
          if(node.data && node.data.methodToCallOnDoubleClick) {
            const doubleClick = node.data.methodToCallOnDoubleClick;
            this.servoyService.executeInlineScript(doubleClick.formname, doubleClick.script, [node.data.methodToCallOnDoubleClickParamValue]);
          }
        }, 
        contextMenu: (tree, node) => {
          if(node.data && node.data.methodToCallOnRightClick) {
            const doubleClick = node.data.methodToCallOnRightClick;
            this.servoyService.executeInlineScript(doubleClick.formname, doubleClick.script, [node.data.methodToCallOnRightClickParamValue]);
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
        if (this.selection) this.expandedNodes = this.selection.slice(0, this.selection.length);
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
                    this.expandChildNodes(this.tree.treeModel.virtualRoot, change.currentValue.level, change.currentValue.value);
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
        if (event.node.data.checked) {
          child.data.checked = true;
        }
      });
    }

    onNodeExpanded(event: any) {
      event.node.data["expanded"] = event.isExpanded;
    }

    onTreeLoad(event: any) {
      this.expandNodes(this.displayNodes);  
      if (this.onReady) {
        this.onReady(event);
      }
    }

    onUpdateTree(event: any) {
      this.checkNodes(event.treeModel.nodes);
    }

    checkNodes(nodes: any) {
      nodes.forEach(node => {
        if (node.parent && node.parent.data.checked) {
          node.checked = true;
        }
        if (node.hasChildren && node.children) this.checkNodes(node.children);
      });
    }

    expandNodes(nodes: any) {
      nodes.forEach(node => {
        if (node.expanded) {
          this.tree.treeModel.getNodeById(node.id).setIsExpanded(true);
        }
        if (node.hasChildren && node.children) this.expandNodes(node.children);
      });
    }

    async check(node: TreeNode, checked: boolean) {
      this.updateChildNodeCheckbox(node, checked);
      this.updateParentNodeCheckbox(node.realParent);

      if (node.data.checkboxvaluedataprovider) {
        let v: any = checked;
        if ("number" == node.data.checkboxvaluedataprovidertype) {
          v = v ? 1 : 0;
        } else if ("string" == node.data.checkboxvaluedataprovidertype) {
          v = v ? 'true' : 'false'
        }
        await this.servoyApi.callServerSideApi('updateFoundsetRow', 
          [node.isRoot, node.data.id.substring(0, node.data.id.lastIndexOf('_')), node.data.index, node.data.checkboxvaluedataprovider, v]);
      }

      if(node.data && node.data.methodToCallOnCheckBoxChange) {
        const checkboxChange = node.data.methodToCallOnCheckBoxChange;
        this.servoyService.executeInlineScript(checkboxChange.formname, checkboxChange.script, [node.data.methodToCallOnCheckBoxChangeParamValue]);
      }
    }

    updateChildNodeCheckbox(node: TreeNode, checked: boolean) {
      node.data.checked = checked;
      if (node.children) {
        node.children.forEach((child) => this.updateChildNodeCheckbox(child, checked));
      }
    }

    updateParentNodeCheckbox(node: TreeNode) {
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

    private buildChild(row: any, foundsetInfo: FoundsetInfo, index: number, level: number, parent: TreeNode) {
      let child : {parent?: TreeNode, id?: string, name?: string, hasChildren?: boolean, image?: string,
      checkbox?: boolean, checked?: boolean, active?: boolean, children?: Array<any>, tooltip?: string,
      datasourceID?: number, index?: number};

      child = { id: foundsetInfo.foundsetInfoID + '_' + row[foundsetInfo.foundsetpk], index: index,
       hasChildren: this.hasChildren(foundsetInfo, index), datasourceID: foundsetInfo.datasourceID};

       // save info about parent 
      if (child.hasChildren) {
        child["indexOfTheParentRecord"] = index;
        child["foundsetInfoID"] = foundsetInfo.foundsetInfoID;
      }

      child["parent"] = parent;
      child["level"] = level;

      const binding = this.getBinding(foundsetInfo.datasourceID);

      if (binding.textdataprovider) {
        child["name"] = row[binding.textdataprovider];
      }

      if (binding.imageurldataprovider) {
        child["image"] = this.getIconURL(row[binding.imageurldataprovider]);
      } else if (child.hasChildren) {
        child["image"] = this.folderImgPath;
      } else {
        child["image"] = this.fileImgPath;
      }

      if (binding.tooltiptextdataprovider) {
        child["tooltip"] = row[binding.tooltiptextdataprovider];
      }

      if (binding.checkboxvaluedataprovider) {
        child["checkbox"] = Boolean(row[binding.hascheckboxdataprovider]);
      }
      else if (binding.hasCheckboxValue) {
        child["checkbox"] = binding.hasCheckboxValue.indexOf("" + row[foundsetInfo.foundsetpk]) != -1;
      }
      else {
        child["checkbox"] = Boolean(binding.initialCheckboxValues);
      }

      if (child["checkbox"]) {
        if(parent && parent.data.checked) {
          child["checked"] = true;
        }
        if (binding.checkboxvaluedataprovider) {
          child["checked"] = Boolean(row[binding.checkboxvaluedataprovider]);
        }
        else if (binding.initialCheckboxValues) {
          child["checked"] = binding.initialCheckboxValues.indexOf("" + row[foundsetInfo.foundsetpk]) != -1;
        }
      }

      const isLevelVisible = this.levelVisibility && this.levelVisibility.value && (this.levelVisibility.level == level);
      const isNodeExpanded = (level <= this.expandedNodes.length) && (this.expandedNodes[level - 1].toString() == this.getPKFromNodeID(child.id));

      if (isLevelVisible || isNodeExpanded) {
        child["expanded"] = true;
      }

      child["active"] = this.isNodeSelected(child, this.selection);

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

      if(binding.checkboxvaluedataprovider) {
        child["checkboxvaluedataprovider"] = binding.checkboxvaluedataprovider;
        child["checkboxvaluedataprovidertype"] = typeof row[binding.checkboxvaluedataprovider];
      }

      if(binding.callbackinfo || binding.methodToCallOnCheckBoxChange || binding.methodToCallOnDoubleClick || binding.methodToCallOnRightClick)
          {
            if(binding.callbackinfo) {
              child["callbackinfo"] = binding.callbackinfo.f;
              child["callbackinfoParamValue"] = row[binding.callbackinfo.param];
            }
            if(binding.methodToCallOnCheckBoxChange) {
              child["methodToCallOnCheckBoxChange"] = binding.methodToCallOnCheckBoxChange.f;
              child["methodToCallOnCheckBoxChangeParamValue"] = row[binding.methodToCallOnCheckBoxChange.param];
            }
            if(binding.methodToCallOnDoubleClick) {
              child["methodToCallOnDoubleClick"] = binding.methodToCallOnDoubleClick.f;
              child["methodToCallOnDoubleClickParamValue"] = row[binding.methodToCallOnDoubleClick.param];
            }    				    				
            if(binding.methodToCallOnRightClick) {
              child["methodToCallOnRightClick"] = binding.methodToCallOnRightClick.f;
              child["methodToCallOnRightClickParamValue"] = row[binding.methodToCallOnRightClick.param];
            }    				    				
          }

      return child;
    }

    hasChildren(foundsetInfo: FoundsetInfo, index: number) {
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

    public updateCheckBoxValuesForTree(datasourceID: number, pks: Array<string> , state: boolean) {
      let updatedNodesPks = []; 
      if (this.tree) {
        for (let node of this.tree.treeModel.nodes) {
          updatedNodesPks.push(...this.updateCheckBoxValues(node, datasourceID, pks, state));
        }
      }

      // if node state was not update, it means it was not yet created, push the check state to the binding
			for(let index = 0; index < pks.length; index++) {
				if(updatedNodesPks.indexOf(pks[index]) == -1) {
					if(state) {
						if(!this.getBinding(datasourceID).initialCheckboxValues) {
							this.getBinding(datasourceID).initialCheckboxValues = [];
						}
						if(this.getBinding(datasourceID).initialCheckboxValues.indexOf(pks[index]) == -1) {
							this.getBinding(datasourceID).initialCheckboxValues.push(pks[index]);
						}
					}
					else if(this.getBinding(datasourceID).initialCheckboxValues) {
						const newIndex = this.getBinding(datasourceID).initialCheckboxValues.indexOf(pks[index]);
						if(newIndex != -1) {
							this.getBinding(datasourceID).initialCheckboxValues.splice(index, 1);
						}
					}
				}
			}
      this.tree.treeModel.update();
    }

    private updateCheckBoxValues(node: any, datasourceID: number, pks: Array<string> , state: boolean) {
      let updatedNodesPks = []; 
      if (node.datasourceID == datasourceID) {
        const pk = this.getPKFromNodeID(node.id);
        for(let index = 0; index < pks.length; index++) {
          if (pk === pks[index]) {
            node.checked = state;
            updatedNodesPks.push(pks[index]);
          }
        }
      } 
      if (node.children) {
        for(let child of node.children) {
          this.updateCheckBoxValues(child, datasourceID, pks, state);
        }
      }
      return updatedNodesPks;
    }

    public getCheckBoxValuesFromTree(datasourceID: number) {
      let checkBoxValues = [];
      if (this.tree) {
        for(let node of this.tree.treeModel.nodes) {
          checkBoxValues.push(...this.getCheckBoxValues(node, datasourceID));
        }
      }
      return checkBoxValues;
    }

    private getCheckBoxValues(node: any, datasourceID: number) {
      let checkBoxValues = [];
      if (node.checked && node.datasourceID === datasourceID) {
        checkBoxValues.push(...this.getPKFromNodeID(node.id));
      }
      if (node.children) {
        for (let child of node.children) {
          checkBoxValues.push(...this.getCheckBoxValues(child, datasourceID));
        }
      }
      return checkBoxValues;
    }

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
            child.setIsHidden(false);
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
				} else {
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
      if (this.autoRefresh) {
        if (!this.removeListenerFunctionArray.find(listener => {
          listener.foundsetId === foundset.foundsetId;
        })) {
          this.removeListenerFunctionArray.push({listener: foundset.addChangeListener((event: FoundsetChangeEvent): void => {
            if (event.viewportRowsCompletelyChanged) {
              this.reinitilizeTree(event.viewportRowsCompletelyChanged);
            }
            if (event.fullValueChanged) {
              this.reinitilizeTree(event.fullValueChanged);
            }
            if (event.serverFoundsetSizeChanged) {
              this.reinitilizeTree(event.serverFoundsetSizeChanged);
            }
            if (event.viewPortSizeChanged) {
              this.reinitilizeTree(event.viewPortSizeChanged);
            }
            if (event.viewportRowsUpdated) {
              this.initTree();
            }
          }), foundsetId: foundset.foundsetId});
        }
      }
    }

    private reinitilizeTree(change: any) {
      if (!isEqual(change.oldValue, change.newValue)) {
        this; this.initTree();
      }
    }

    private removeFoundsetListener(foundset: Foundset) {
      const fsListener = this.removeListenerFunctionArray.find(el => el.foundsetId === foundset.foundsetId);
      if (fsListener) fsListener.listener();
      this.removeListenerFunctionArray = this.removeListenerFunctionArray.filter(el => !(el.foundsetId === foundset.foundsetId));
    }

}

export class FoundsetInfo extends BaseCustomObject {
  public datasourceID: number;
  public foundsetInfoID: number;
  public foundsetInfoParentID: number;
  public indexOfTheParentRecord: number;
  public foundset: Foundset;
  public foundsetpk: string;
}

export class Binding extends BaseCustomObject {
  public datasource: string;
  public textdataprovider: string;
  public nrelationname: string;
  public hascheckboxdataprovider: string;
  public checkboxvaluedataprovider: string;
  public tooltiptextdataprovider: string;
  public imageurldataprovider: string;
  public childsortdataprovider: string;
  public foundsetpk: string;
  public callbackinfo: Callback;
  public methodToCallOnCheckBoxChange: Callback;
  public methodToCallOnDoubleClick: Callback;
  public methodToCallOnRightClick: Callback;
  public nRelationInfos: Array<RelationInfo>;
  public hasCheckboxValue: Array<Object>;
  public initialCheckboxValues: Array<Object>;
}

export class Datasource extends BaseCustomObject {
  public name: string;
  public id: number;
}

export class Callback extends BaseCustomObject {
   public f: Function;
   public param: string;
}

export class RelationInfo extends BaseCustomObject {
  public label: string;
  public nRelationName: string;
}

export class LevelVisibilityType extends BaseCustomObject {
  public value: boolean;
  public level: number;
}