import { Component, SimpleChanges, Input, Output, Renderer2, ChangeDetectorRef, ContentChild, TemplateRef, EventEmitter, ViewChild, ElementRef } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public';
import { FormService } from '../../ngclient/form.service';

@Component({
    selector: 'servoyextra-sidenav',
    templateUrl: './sidenav.html'
})
export class ServoyExtraSidenav extends ServoyBaseComponent<HTMLDivElement> {

    @Input() enabled: boolean;
    @Input() styleClass: string;
    @Input() tabSeq: number;
    @Input() sidenavWidth: number;
    @Input() responsiveHeight: number;
    @Input() containedForm: string;
    @Input() relationName: string;
    @Input() iconOpenStyleClass: string;
    @Input() iconCloseStyleClass: string;
    @Input() iconExpandStyleClass: string;
    @Input() iconCollapseStyleClass: string;

    @Input() slidePosition: string;
    @Input() slideAnimation: string;
    @Input() togglePosition: string;
    @Input() scrollbarPosition: string;
    @Input() open: boolean;
    @Output() openChange = new EventEmitter();
    @Input() animate: boolean;

    @Input() selectedIndex: Object;
    @Output() selectedIndexChange = new EventEmitter();
    @Input() expandedIndex: Object;
    @Output() expandedIndexChange = new EventEmitter();
    @Input() menu: Array<MenuItem>;

    @Input() onMenuItemSelected: (id: string, event: MouseEvent) => Promise<boolean>;
    @Input() onMenuItemExpanded: (id: string, event: MouseEvent) => Promise<boolean>;
    @Input() onMenuItemCollapsed: (id: string, event: MouseEvent) => Promise<boolean>;
    @Input() onOpenToggled: (event: MouseEvent) => void;

    @ContentChild(TemplateRef, { static: true })
    templateRef: TemplateRef<any>;
    private realContainedForm: any;

    @ViewChild('element', { static: true }) elementRef: ElementRef;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, private formService: FormService) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        if (!this.selectedIndex) this.selectedIndex = {};
        if (!this.expandedIndex) this.expandedIndex = {};
    }

    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
        if (changes) {
            for (const property of Object.keys(changes)) {
                const change = changes[property];
                switch (property) {
                    case 'enabled':
                        let nav = this.getNativeElement().querySelector('nav');
                        if (change.currentValue) {
                            this.renderer.removeAttribute(nav, 'disabled');
                            this.renderer.removeClass(nav, 'svy-sidenav-disabled')
                        }
                        else {
                            this.renderer.setAttribute(nav, 'disabled', 'disabled');
                            this.renderer.addClass(nav, 'svy-sidenav-disabled');
                        }
                        break;
                    case 'styleClass':
                        let sidenav = this.getNativeElement().querySelector('.svy-sidenav');
                        if (change.previousValue) {
                            const array = change.previousValue.split(' ');
                            array.forEach(element => this.renderer.removeClass(sidenav, element));
                        }
                        if (change.currentValue) {
                            const array = change.currentValue.split(' ');
                            array.forEach(element => this.renderer.addClass(sidenav, element));
                        }
                        break;
                    case "containedForm":
                        if (change.currentValue) {
                            this.renderer.addClass(this.getNativeElement(), "has-panel");
                        } else {
                            this.renderer.removeClass(this.getNativeElement(), "has-panel");
                        }
                        if (change.previousValue) {
                            this.servoyApi.hideForm(change.previousValue, null, null, this.containedForm, this.relationName).then(() => {
                                this.realContainedForm = this.containedForm;
                            }).finally(() => this.cdRef.detectChanges());
                        }
                        else if (change.currentValue) {
                            this.servoyApi.formWillShow(this.containedForm, this.relationName).then(() => {
                                this.realContainedForm = this.containedForm;
                            }).finally(() => this.cdRef.detectChanges());
                        }
                        break;
                    case "sidenavWidth":
                    case "responsiveHeight":
                        this.updateSidenavStyle();
                        break;
                    case 'open':
                        this.animateMenuHover(this.open);
                        this.animateSlideMenu(this.open);
                        break;
                    case 'expandedIndex':
                        if (typeof this.expandedIndex == 'string') {
                            this.expandedIndex = JSON.parse(this.expandedIndex);
                        }
                        break;
                    case 'selectedIndex':
                        if (typeof this.selectedIndex == 'string') {
                            this.selectedIndex = JSON.parse(this.selectedIndex);
                        }
                        break;
                }
            }
        }
    }

    getForm() {
        return this.realContainedForm;
    }

    getResponsiveHeight() {
        var height = 0;
        if (!this.servoyApi.isInAbsoluteLayout()) {
            if (this.responsiveHeight) {
                height = this.responsiveHeight;
            } else if (this.containedForm) {
                // for absolute form default height is design height, for responsive form default height is 0
                let formCache = this.formService.getFormCacheByName(this.containedForm);
                if (formCache && formCache.absolute) {
                    height = formCache.size.height;
                }
            }
        }
        return height;
    }

    getSidenavWidth() {
        if (this.sidenavWidth) {
            // if value is set and there is a responsiveForm or a containedForm. Note should react also if containeForm is set later
            if (!this.servoyApi.isInAbsoluteLayout() || this.containedForm) {
                return this.sidenavWidth;
            }
        }
        return 0;
    }

    updateSidenavStyle() {
        let sidenav = this.getNativeElement().querySelector('.svy-sidenav');
        var width = this.getSidenavWidth();
        if (width) {
            this.renderer.setStyle(sidenav, 'width', width + 'px');
        }

        // check height
        var height = this.getResponsiveHeight();
        if (height) {
            this.renderer.setStyle(sidenav, 'minHeight', height + 'px')
        }
    }

    getContainerStyle() {
        let height = this.getResponsiveHeight();
        let width = this.getSidenavWidth();
        let cssStyle = {
            "position": "relative",
            "min-height": height + "px"
        }
        switch (this.slidePosition) {
            case "left":
                cssStyle['marginLeft'] = width + "px";
                break;
            case "right":
                cssStyle['marginRight'] = width + "px";
            default:
                break;
        }

        return cssStyle;
    }

    animateSlideMenuTimeout: number;

    animateSlideMenu(open: boolean) {
        if (this.slidePosition && this.slidePosition != 'static') {
            let iconOpen = this.getNativeElement().querySelector('.svy-sidenav-action-open');
            let sidenav = this.getNativeElement().querySelector('.svy-sidenav');
            let svyextracontainer = this.getNativeElement();
            if (open) { // open the menu when hovering

                // remove all hover animation
                this.renderer.removeClass(sidenav, 'svy-hover-animate');
                this.renderer.removeClass(sidenav, 'svy-hover-remove');
                this.renderer.removeClass(sidenav, 'svy-hover-add');
                this.renderer.removeClass(sidenav, 'svy-hover');
                if (sidenav.classList.contains('svy-slide-out')) {

                    this.renderer.removeClass(svyextracontainer, 'svy-slide-out');
                    this.renderer.removeClass(sidenav, 'svy-slide-out');

                    // used to slide in the panel if. Use only if menu slides
                    if (this.slideAnimation === "slide-menu") {
                        this.renderer.addClass(svyextracontainer, 'svy-slide-out-remove-delay');

                        // stop remove animation clearing previous timeout
                        if (this.animateSlideMenuTimeout) {
                            window.clearTimeout(this.animateSlideMenuTimeout);
                            this.animateSlideMenuTimeout = undefined;
                        }

                        window.requestAnimationFrame(() => {
                            // complete hover animation
                            this.animateSlideMenuTimeout = window.setTimeout(() => {
                                this.renderer.removeClass(svyextracontainer, 'svy-slide-out-remove-delay');
                            }, 450);

                        });
                    }

                }
                this.iconCloseStyleClass.split(' ').forEach(element => this.renderer.removeClass(iconOpen, element));
                this.iconOpenStyleClass.split(' ').forEach(element => this.renderer.addClass(iconOpen, element));
            } else {
                if (!svyextracontainer.classList.contains('svy-slide-out')) {
                    this.renderer.addClass(sidenav, 'svy-slide-out');
                    this.renderer.addClass(svyextracontainer, 'svy-slide-out');
                }
                this.iconOpenStyleClass.split(' ').forEach(element => this.renderer.removeClass(iconOpen, element));
                this.iconCloseStyleClass.split(' ').forEach(element => this.renderer.addClass(iconOpen, element));
            }
        } else {
            this.open = true;
            this.openChange.emit(this.open);
        }
    }

    animateMenuHover(open: boolean) {
        if (open === false) { // add listener when menu closed, use a delay
            setTimeout(() => {
                this.bindOnHover();
            }, 300);
        } else { // remove listener when open
            this.unbindOnHover();
        }
    }

    mouseEnterTimeout: number;
    mouseLeaveTimeout: number;
    private onMouseEnter = (e: Event) => {
        // only if the menu is collapsed, use the mouseover
        if (this.slideAnimation === 'collapse-menu') {
            let sidenav = this.getNativeElement().querySelector('.svy-sidenav');
            // stop remove animation clearing previous timeout
            if (this.mouseLeaveTimeout) {
                this.renderer.removeClass(sidenav, 'svy-hover-remove');
                window.clearTimeout(this.mouseLeaveTimeout);
                this.mouseLeaveTimeout = undefined;
            }


            // to start animation add svy-hover-add to start animation and remove at next repaint
            this.renderer.addClass(sidenav, 'svy-hover');
            this.renderer.addClass(sidenav, 'svy-hover-add');
            this.renderer.addClass(sidenav, 'svy-hover-animate');
            window.requestAnimationFrame(() => {
                this.renderer.removeClass(sidenav, 'svy-hover-add');

                // complete hover animation
                this.mouseEnterTimeout = window.setTimeout(() => {
                    this.renderer.removeClass(sidenav, 'svy-hover-animate');
                }, 450);

            });

        }
    }
    private onMouseLeave = (e: Event) => {
        // only if the menu is collapsed, use the mouseover
        if (this.slideAnimation === 'collapse-menu') {
            let sidenav = this.getNativeElement().querySelector('.svy-sidenav');
            // stop add animation
            if (this.mouseEnterTimeout) {
                this.renderer.removeClass(sidenav, 'svy-hover-add');
                window.clearTimeout(this.mouseEnterTimeout);
                this.mouseEnterTimeout = undefined;
            }

            // start hover remove animation
            this.renderer.addClass(sidenav, 'svy-hover-animate');
            this.renderer.addClass(sidenav, 'svy-hover-remove ');
            this.renderer.removeClass(sidenav, 'svy-hover');

            // complete hover animation
            this.mouseLeaveTimeout = window.setTimeout(() => {
                this.renderer.removeClass(sidenav, 'svy-hover-animate');
                this.renderer.removeClass(sidenav, 'svy-hover-remove');
            }, 450);
        }
    }

    bindOnHover() {
        // register on mouse hover
        if (this.slideAnimation === 'collapse-menu') {
            let sidenav = this.getNativeElement().querySelector('.svy-sidenav');
            let nav = this.getNativeElement().querySelector('nav');
            nav.addEventListener('mouseenter', this.onMouseEnter);
            sidenav.addEventListener('mouseleave', this.onMouseLeave);
        }

    }

    unbindOnHover() {
        let sidenav = this.getNativeElement().querySelector('.svy-sidenav');
        let nav = this.getNativeElement().querySelector('nav');
        nav.removeEventListener('mouseenter', this.onMouseEnter);
        sidenav.removeEventListener('mouseleave', this.onMouseLeave);
    }

    slideMenu(event: MouseEvent) {
        let wasOpen = this.open;
        this.open = this.open === false ? true : false;

        this.animateMenuHover(this.open);
        this.animateSlideMenu(this.open);
        this.openChange.emit(this.open);

        // event on menu open
        if (this.onOpenToggled && wasOpen != this.open) {
            this.onOpenToggled(event);
        }

    }

    selectItem(level: number, index: number, item: MenuItem, event?: MouseEvent, preventSelectHandler?: boolean, preventExpandHandler?: boolean) {

        if (event) { //
            event.stopPropagation();
        } else { //
            event = this.createJSEvent();
        }

        // prevent selection if item is disabled
        if (this.isDisabled(item.id)) {
            return false;
        }

        let confirmSelection = function() {
            this.setSelectedIndex(level, index, item);

            // expand the item
            if (item.menuItems) { // expand the node if not leaf
                this.expandItem(level, index, item, event, preventExpandHandler); // TODO add collapsed argument
            } else { // expand the parent node if is a leaf
                var parentNode = this.getParentNode(item.id);
                if (parentNode) {
                    this.expandItem(level - 1, null, parentNode, event, preventExpandHandler);
                }
            }
        }

        if (preventSelectHandler != true && this.onMenuItemSelected) { // change selection only if onMenuItemSelected allows it
            this.onMenuItemSelected(item.id, event).then((result) => {
                if (result !== false) {
                    confirmSelection();
                }
            }, (err) => {
                // TODO use logging instead
                console.error(err);
            });
        } else {
            confirmSelection();
        }
        return true;
    }

    toggleExpandedItem(level: number, index: number, item: MenuItem, event: MouseEvent, preventHandler: boolean) {
        if (!this.isNodeExpanded(item.id, level)) { // expand the item
            this.expandItem(level, index, item, event, preventHandler);
        } else { // collapse the item
            this.collapseItem(level, index, item, event, preventHandler);
        }
    }

    expandItem(level: number, index: number, item: MenuItem, event?: MouseEvent, preventHandler?: boolean) {

        if (event) { //
            event.stopPropagation();
        } else { //
            event = this.createJSEvent();
        }

        // check if node is already collapsed
        if (this.isNodeExpanded(item.id, level)) {
            return true;
        }

        // prevent selection if item is disabled
        if (this.isDisabled(item.id)) {
            return false;
        }

        // if is expanded
        if (preventHandler != true && this.onMenuItemExpanded) { // change selection only if onMenuItemSelected allows it
            this.onMenuItemExpanded(item.id, event).then((result) => {
                // if (result == true) {
                this.setExpandedIndex(level, index, item);
                // }
            }, (err) => { // Error: "Oops something went wrong"
                // TODO use logging instead
                console.error(err);
            });
        } else {
            this.setExpandedIndex(level, index, item);
        }

        return true;
    }

    collapseItem(level: number, index: number, item: MenuItem, event?: MouseEvent, preventHandler?: boolean) {

        if (event) { //
            event.stopPropagation();
        } else { //
            event = this.createJSEvent();
        }

        // check if node is already collapsed
        if (!this.isNodeExpanded(item.id, level)) {
            return true;
        }

        // prevent selection if item is disabled
        if (this.isDisabled(item.id)) {
            return false;
        }

        // call handler onMenuItemCollapsed
        if (preventHandler != true && this.onMenuItemCollapsed) {
            this.onMenuItemCollapsed(item.id, event).then((result) => {
                // if (result == true) {
                this.clearExpandedIndex(level - 1);
                // }
            }, (err) => { // Error: "Oops something went wrong"
                // TODO use logging instead
                console.error(err);
            });
        } else {
            this.clearExpandedIndex(level - 1);
        }

        return true;
    }

    getNodeById(nodeId: string | number, nodes: Array<MenuItem>): MenuItem {
        if (nodes) {
            for (let i = 0; i < nodes.length; i++) { // search in each subtree
                let subTree = nodes[i];
                // TODO use type equality or not ?
                if (subTree.id == nodeId) { // find the node
                    return subTree;
                }
                let node = this.getNodeById(nodeId, subTree.menuItems);
                if (node) {
                    return node;
                }
            }
        }
        return null;
    }

    getNodeByIndexPath(path: Array<number>, nodes: Array<MenuItem>): MenuItem {
        let node = null;
        if (nodes) {
            if (path && path.length === 1) {
                node = nodes[path[0]];
            } else if (path && path.length) {
                var subPathIndex = path[0];
                var subtree = nodes[subPathIndex].menuItems;
                node = this.getNodeByIndexPath(path.slice(1, path.length), subtree);
            } else { // is the root
                node = nodes;
            }
        }
        return node;
    }

    getPathToNode(idOrNode, nodes: Array<MenuItem>, key?: string): Array<number> {
        if (!key) key = 'id';
        var nodeId = idOrNode[key] ? idOrNode[key] : idOrNode;

        if (nodes) { // for each node in nodes
            for (let i = 0; i < nodes.length; i++) { // search in each subtree
                let subTree = nodes[i];
                if (subTree[key] == nodeId) { // find the node
                    return [i];
                }
                let path = this.getPathToNode(nodeId, subTree.menuItems, key);
                if (path) {
                    return [i].concat(path);
                }
            }
        }
        return null;
    }

    getAllNodesToNodeId(nodeId: string | number): Array<MenuItem> {
        let nodes = this.menu;
        var pathIndex = this.getPathToNode(nodeId, nodes);
        let anchestors = [];
        let node;

        // returns all the anchestors of node
        for (var i = 0; pathIndex && i < pathIndex.length; i++) {
            node = nodes[pathIndex[i]];
            anchestors.push(node);
            nodes = node.menuItems;
        }
        return anchestors;
    }

    getNodeAnchestors(nodeId: string | number): Array<MenuItem> {
        var anchestors = this.getAllNodesToNodeId(nodeId);
        anchestors.pop();
        return anchestors;
    }

    getParentNode(nodeId: string | number): MenuItem {
        var anchestors = this.getNodeAnchestors(nodeId);
        if (anchestors && anchestors.length) {
            return anchestors[anchestors.length - 1];
        }
        return null;
    }

    getNodeLevel(nodeId: string | number): number {
        var path = this.getPathToNode(nodeId, this.menu);
        if (path) {
            return path.length;
        } else {
            return null;
        }
    }

    getSelectedNode(level: number): MenuItem {
        let levels = this.selectedIndex;
        let maxLevel = -1;

        // get the node at deeper level
        for (let lvl in levels) {
            if (Number(lvl) > maxLevel && (!level || Number(lvl) <= level)) {
                maxLevel = Number(lvl);
            }
        }

        var nodeId = levels[maxLevel];
        return this.getNodeById(nodeId, this.menu);
    }

    getSelectedIndexPath(level: number): Array<Number> {
        var selectedNode = this.getSelectedNode(level);
        var path = this.getPathToNode(selectedNode.id, this.menu);
        return path;
    }

    setSelectedIndex(level: number, index: number, item: MenuItem) {
        if (!this.selectedIndex) this.selectedIndex = {};
        var levels = this.selectedIndex;

        // clear level below selection
        this.clearSelectedIndex(level);

        //              // update levels above selection, all anchestors
        let newSelectedIndex = {}
        let anchestors = this.getNodeAnchestors(item.id);
        for (var i = 0; i < anchestors.length; i++) {
            if (newSelectedIndex[i + 1] != anchestors[i].id) {
                newSelectedIndex[i + 1] = anchestors[i].id;
            }
        }

        // TODO select all parents as well
        // set level index
        if (levels[level] == item.id) { // collapse the selected menu
            // TODO allow unselect !?
            newSelectedIndex[level] = item.id;
        } else {
            newSelectedIndex[level] = item.id;
        }
        this.selectedIndex = newSelectedIndex;
        this.selectedIndexChange.emit(this.selectedIndex);
    }

    clearSelectedIndex(level: number) {
        var levels = this.selectedIndex;
        // reset all sub levels
        for (var lvl in levels) {
            if (Number(lvl) > level) { // reset the next levels
                delete levels[lvl];
            }
        }
    }

    setExpandedIndex(level: number, index: number, item: MenuItem) {
        if (!this.expandedIndex) this.expandedIndex = {}
        var levels = this.expandedIndex;

        // clear sub levels
        this.clearExpandedIndex(level);

        // expand all anchestors
        var newExpandedIndex = {}
        var anchestors = this.getNodeAnchestors(item.id);
        for (var i = 0; i < anchestors.length; i++) {
            if (newExpandedIndex[i + 1] != anchestors[i].id) {
                newExpandedIndex[i + 1] = anchestors[i].id;
            }
        }

        // TODO select all parents as well
        // expand node index
        if (levels[level] != item.id) { // collapse the selected menu
            newExpandedIndex[level] = item.id;
        }
        this.expandedIndex = newExpandedIndex;
        this.expandedIndexChange.emit(this.expandedIndex);
    }


    clearExpandedIndex(level: number) {
        var levels = this.expandedIndex;

        // reset all sub levels
        for (let lvl in levels) {
            if (Number(lvl) > level) { // reset the next levels
                delete levels[lvl];
            }
        }
    }

    isDisabled = function(nodeId: string | number): boolean {
        // check if menu itself is disable
        if (this.enabled == false) {
            return true;
        }

        // TODO refactor: use getNodeAnchestors
        let indexPath = this.getPathToNode(nodeId, this.menu);
        let tree = this.menu;
        let node;

        if (!indexPath || !indexPath.length) {
            return null;
        }

        for (let i = 0; i < indexPath.length; i++) {
            node = tree[indexPath[i]];
            if (node.enabled == false) {
                return true;
            }
            tree = node.menuItems;
        }
        return false;
    }

    isNodeSelected(nodeId: string | number, level: number): boolean {
        var levels = this.selectedIndex;
        if (level) {
            return levels[level] == nodeId;
        } else {
            for (let level2 in levels) {
                if (levels[level2] == nodeId) {
                    return true;
                }
            }
        }
        return false;
    }

    isNodeExpanded(nodeId: string, level: number): boolean {
        var levels = this.expandedIndex;
        if (level) {
            return levels[level] == nodeId;
        } else {
            for (let level2 in levels) {
                if (levels[level2] == nodeId) {
                    return true;
                }
            }
        }
        return false;
    }

    createJSEvent(): MouseEvent {
        let x = this.getNativeElement().offsetLeft;
        let y = this.getNativeElement().offsetHeight;

        var event = document.createEvent("MouseEvents");
        event.initMouseEvent("click", false, true, window, 1, x, y, x, y, false, false, false, false, 0, null);
        return event;
    }

    getDOMElementByID(nodeId: string | number): Element {
        let indexPath = this.getPathToNode(nodeId, this.menu);
        if (indexPath) {
            let foundElement: Element = this.getNativeElement();
            for (var i = 0; i < indexPath.length; i++) {
                foundElement = foundElement.querySelector("ul.sn-level-" + (i + 1));
                foundElement = foundElement.children.item(i);
            }
            return foundElement;
        }
        return null;
    }

    /****************************************************************
    * API
    **************************************************************/

    getSelectedMenuItem(level: number): MenuItem {
        // TODO if level is greater then selected level, what should return ?
        return this.getSelectedNode(level);
    }

    setSelectedMenuItem(id: string | number, mustExecuteOnMenuItemSelect?: boolean, mustExecuteOnMenuItemExpand?: boolean, level?: number): boolean {
        var nodes;
        var levelPath = [];

        // if level is provided search only in the selected node
        if (level && level > 1) { // search in selected node only
            levelPath = this.getSelectedIndexPath(level - 1);
            var parentNode = this.getNodeByIndexPath(levelPath, this.menu); // retrieve the selected node at level
            if (parentNode) nodes = parentNode.menuItems;
        } else if (level === 1) { // search in root
            // FIXME it searches in the whole tree
            nodes = this.menu;
        } else {
            nodes = this.menu;
        }

        // search path to node
        var path = levelPath;
        var subPath = this.getPathToNode(id, nodes, 'id');
        if (subPath) { // not found in the selected node
            path = levelPath.concat(subPath);
        } else {
            return false;
        }

        // do nothing if the item is already selected
        if (this.isNodeSelected(id, path.length) && !this.selectedIndex[path.length + 1]) {
            return true;
        } else {
            // search the node
            var node = this.getNodeByIndexPath(subPath, nodes);

            // select the item
            var preventSelectHandler = mustExecuteOnMenuItemSelect == true ? false : true;
            var preventExpandHandler = mustExecuteOnMenuItemExpand == true ? false : true;
            return this.selectItem(path.length, path[path.length - 1], node, null, preventSelectHandler, preventExpandHandler);
        }
    }

    setSelectedMenuItemAsync(id: string | number) {
        this.setSelectedMenuItem(id, false, false);
    }

    setSelectedByIndexPath(path: Array<number>, mustExecuteOnSelectNode: boolean) {

        // search node in tree
        var node = this.getNodeByIndexPath(path, this.menu);
        var preventSelectHandler = mustExecuteOnSelectNode == true ? false : true;
        this.selectItem(path.length, path[path.length - 1], node, null, preventSelectHandler);
        return;
    }

    setMenuItemExpanded(menuItemId: string | number, expanded: boolean, mustExecuteOnMenuItemExpand?: boolean) {
        var node = this.getNodeById(menuItemId, this.menu);

        if (!node) {
            return false;
        }

        // expandItem/collapsItem requires node level
        var level = this.getNodeLevel(menuItemId);
        var preventHandler = mustExecuteOnMenuItemExpand == true ? false : true;

        if (expanded) {
            return this.expandItem(level, null, node, null, preventHandler);
        } else {
            return this.collapseItem(level, null, node, null, preventHandler);
        }

    }

    isMenuItemExpanded = function(menuItemId: string | number) {
        return this.isNodeExpanded(menuItemId);
    }

    isMenuItemEnabled = function(menuItemId: string | number) {
        var disabled = this.isDisabled(menuItemId);
        if (disabled === null) {
            return false
        } else {
            return !disabled;
        }
    }

    getLocation(nodeId: string | number): Object {
        var domElement = this.getDOMElementByID(nodeId);
        if (domElement) {
            var position = domElement.getBoundingClientRect();
            return { x: position.left, y: position.top };
        }
        return null;
    }

    getSize(nodeId: string | number): Object {
        var domElement = this.getDOMElementByID(nodeId);
        if (domElement) {
            var position = domElement.getBoundingClientRect();
            return { width: position.width, height: position.height };
        }
        return null;
    }
}
class MenuItem {
    public text: string;
    public id: string;
    public iconStyleClass: string;
    public styleClass: number;
    public enabled: boolean;
    public data: any;
    public menuItems: Array<MenuItem>;
    public isDivider: boolean;
}
