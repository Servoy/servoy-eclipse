/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.designer.editor.rfb;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.Activator;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.MoveDownCommand;
import com.servoy.eclipse.designer.editor.commands.MoveUpCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.CopyAction;
import com.servoy.eclipse.designer.editor.rfb.actions.PasteAction;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.AbstractGroupCommand.GroupCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.AbstractGroupCommand.UngroupCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentsHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.GetPartStylesHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.GhostHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.KeyPressedHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.MoveInResponsiveLayoutHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.OpenContainedFormHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.OpenElementWizardHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.OpenFormHierarchyHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.OpenPropertiesWizardHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.OpenScriptHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.PersistFinder;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.RevertFormCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.SetPropertiesHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.SetSelectionHandler;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.SetTabSequenceCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.SpacingCentersPack;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.UpdateFieldPositioner;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.UpdatePaletteOrder;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.ZOrderCommand;
import com.servoy.eclipse.designer.outline.FormOutlinePage;
import com.servoy.eclipse.designer.rfb.palette.PaletteFavoritesHandler;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.ui.CopySourceFolderAction;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Handle requests from the rfb html editor.
 *
 * @author rgansevles
 *
 */
public class EditorServiceHandler implements IServerService
{
	private final BaseVisualFormEditor editorPart;
	private final ISelectionProvider selectionProvider;
	private final RfbSelectionListener selectionListener;
	private final IFieldPositioner fieldPositioner;

	private final HashMap<String, IServerService> configuredHandlers = new HashMap<String, IServerService>();

	public EditorServiceHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider, RfbSelectionListener selectionListener,
		IFieldPositioner fieldPositioner)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
		this.selectionListener = selectionListener;
		this.fieldPositioner = fieldPositioner;
		configureHandlers();
	}

	private void configureHandlers()
	{
		configuredHandlers.put("getGhostComponents", new GhostHandler(editorPart));
		configuredHandlers.put("setSelection", new SetSelectionHandler(editorPart, selectionListener, selectionProvider));
		configuredHandlers.put("revertForm", new RevertFormCommand());

		configuredHandlers.put("setTabSequence", new SetTabSequenceCommand(editorPart, selectionProvider));

		configuredHandlers.put("z_order_bring_to_front_one_step", new ZOrderCommand(editorPart, selectionProvider, "z_order_bring_to_front_one_step"));
		configuredHandlers.put("z_order_send_to_back_one_step", new ZOrderCommand(editorPart, selectionProvider, "z_order_send_to_back_one_step"));
		configuredHandlers.put("z_order_bring_to_front", new ZOrderCommand(editorPart, selectionProvider, "z_order_bring_to_front"));
		configuredHandlers.put("z_order_send_to_back", new ZOrderCommand(editorPart, selectionProvider, "z_order_send_to_back"));

		configuredHandlers.put("horizontal_spacing", new SpacingCentersPack(editorPart, selectionProvider));
		configuredHandlers.put("vertical_spacing", new SpacingCentersPack(editorPart, selectionProvider));
		configuredHandlers.put("horizontal_centers", new SpacingCentersPack(editorPart, selectionProvider));
		configuredHandlers.put("vertical_centers", new SpacingCentersPack(editorPart, selectionProvider));
		configuredHandlers.put("horizontal_pack", new SpacingCentersPack(editorPart, selectionProvider));
		configuredHandlers.put("vertical_pack", new SpacingCentersPack(editorPart, selectionProvider));

		configuredHandlers.put("keyPressed", new KeyPressedHandler(editorPart, selectionProvider));
		configuredHandlers.put("setProperties", new SetPropertiesHandler(editorPart));
		configuredHandlers.put("moveComponent", new MoveInResponsiveLayoutHandler(editorPart));
		configuredHandlers.put("createComponent", new CreateComponentHandler(editorPart, selectionProvider));
		configuredHandlers.put("getPartsStyles", new GetPartStylesHandler(editorPart));
		configuredHandlers.put("getSystemFont", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				Font f = JFaceResources.getFont(JFaceResources.DEFAULT_FONT);
				JSONObject result = new JSONObject();
				result.put("font", f.getFontData()[0].getName());
				float systemDPI = Utils.isLinuxOS() ? 96f : 72f;
				int pxHeight = Math.round(f.getFontData()[0].getHeight() * Display.getDefault().getDPI().y / systemDPI);
				result.put("size", pxHeight);
				return result;
			}
		});
		configuredHandlers.put("createComponents", new CreateComponentsHandler(editorPart, selectionProvider));
		configuredHandlers.put("openElementWizard", new OpenElementWizardHandler(editorPart, fieldPositioner, selectionProvider));
		configuredHandlers.put("updateFieldPositioner", new UpdateFieldPositioner(editorPart, fieldPositioner));
		configuredHandlers.put("openScript", new OpenScriptHandler(editorPart));
		configuredHandlers.put("openFormHierarchy", new OpenFormHierarchyHandler(selectionProvider));
		configuredHandlers.put("updatePaletteOrder", new UpdatePaletteOrder());
		configuredHandlers.put("updateFavoritesComponents", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				PaletteFavoritesHandler.getInstance().updateFavorite(args.getString("name"));
				((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).refreshPalette();
				return null;
			}
		});
		configuredHandlers.put("openContainedForm", new OpenContainedFormHandler(editorPart));
		configuredHandlers.put("buildTiNG", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				CopySourceFolderAction copySourceFolderAction = new CopySourceFolderAction();
				copySourceFolderAction.run();
				return null;
			}
		});
		configuredHandlers.put("setInlineEditMode", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				if (args.has("inlineEdit"))
				{
					boolean inlineEdit = args.getBoolean("inlineEdit");
					if (inlineEdit)
					{
						editorPart.deactivateEditorContext();
					}
					else
					{
						editorPart.activateEditorContext();
					}
				}
				return null;
			}
		});


		configuredHandlers.put("zoomIn", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args)
			{
				PersistContext selection = null;
				if (selectionProvider != null && selectionProvider.getSelection() instanceof IStructuredSelection &&
					((IStructuredSelection)selectionProvider.getSelection()).size() == 1)
				{
					selection = (PersistContext)((IStructuredSelection)selectionProvider.getSelection()).getFirstElement();
				}
				if (selection != null)
				{
					IPersist currentPersist = selection.getPersist();
					while (currentPersist != null && !(currentPersist instanceof LayoutContainer))
					{
						currentPersist = currentPersist.getParent();
					}
					if (currentPersist instanceof LayoutContainer)
					{
						((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).zoomIn((LayoutContainer)currentPersist);
					}
				}
				return null;
			}
		});

		configuredHandlers.put("consoleLog", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args)
			{
				System.out.println(args.optString("message", ""));
				return null;
			}
		});


		configuredHandlers.put("zoomOut", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args)
			{
				((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).zoomOut();
				return null;
			}
		});

		configuredHandlers.put("copy", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args)
			{
				CopyAction cp = new CopyAction(editorPart);
				cp.update();
				cp.run();
				return null;
			}
		});

		configuredHandlers.put("paste", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args)
			{
				PasteAction pasteAction = new PasteAction(com.servoy.eclipse.core.Activator.getDefault().getDesignClient(), selectionProvider, editorPart,
					fieldPositioner);
				pasteAction.update();
				pasteAction.run();
				return null;
			}
		});

		configuredHandlers.put("toggleShowData", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args)
			{
				Activator.getDefault().toggleShowData();
				((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).refreshBrowserUrl(true);
				return null;
			}
		});

		configuredHandlers.put("requestSelection", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args)
			{
				((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).requestSelection();
				return null;
			}
		});

		configuredHandlers.put("toggleShow", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				if (args != null && args.has("show"))
				{
					String showValue = args.getString("show");
					Activator.getDefault().toggleShow(showValue);
					if (Activator.SHOW_I18N_VALUES_IN_ANGULAR_DESIGNER.equals(showValue))
					{
						((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).refreshBrowserUrl(true);
					}
					return Activator.getDefault().getPreferenceStore().contains(showValue)
						? Boolean.valueOf(Activator.getDefault().getPreferenceStore().getBoolean(showValue)) : Boolean.FALSE;
				}
				return Boolean.FALSE;
			}

		});

		configuredHandlers.put("getBooleanState", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				if (args != null)
				{
					if (args.has("isInheritedForm")) return Boolean.valueOf(editorPart.getForm().getExtendsID() > 0);
					if (args.has("showData"))
					{
						return Activator.getDefault().getPreferenceStore().contains(Activator.SHOW_DATA_IN_ANGULAR_DESIGNER)
							? Boolean.valueOf(Activator.getDefault().getPreferenceStore().getBoolean(Activator.SHOW_DATA_IN_ANGULAR_DESIGNER)) : Boolean.TRUE;
					}
					if (args.has("showWireframe"))
					{
						return Activator.getDefault().getPreferenceStore().contains(Activator.SHOW_WIREFRAME_IN_ANGULAR_DESIGNER)
							? Boolean.valueOf(Activator.getDefault().getPreferenceStore().getBoolean(Activator.SHOW_WIREFRAME_IN_ANGULAR_DESIGNER))
							: Boolean.FALSE;
					}
					if (args.has("showSolutionLayoutsCss"))
					{
						return Activator.getDefault().getPreferenceStore().contains(Activator.SHOW_SOLUTION_LAYOUTS_CSS_IN_ANGULAR_DESIGNER)
							? Boolean.valueOf(Activator.getDefault().getPreferenceStore().getBoolean(Activator.SHOW_SOLUTION_LAYOUTS_CSS_IN_ANGULAR_DESIGNER))
							: Boolean.TRUE;
					}
					if (args.has("showSolutionCss"))
					{
						return Activator.getDefault().getPreferenceStore().contains(Activator.SHOW_SOLUTION_CSS_IN_ANGULAR_DESIGNER)
							? Boolean.valueOf(Activator.getDefault().getPreferenceStore().getBoolean(Activator.SHOW_SOLUTION_CSS_IN_ANGULAR_DESIGNER))
							: Boolean.TRUE;
					}
					if (args.has("showHighlight"))
					{
						return Activator.getDefault().getPreferenceStore().contains(Activator.SHOW_HIGHLIGHT_IN_ANGULAR_DESIGNER)
							? Boolean.valueOf(Activator.getDefault().getPreferenceStore().getBoolean(Activator.SHOW_HIGHLIGHT_IN_ANGULAR_DESIGNER))
							: Boolean.TRUE;
					}
					if (args.has("showDynamicGuides"))
					{
						return Activator.getDefault().getPreferenceStore().contains(Activator.SHOW_DYNAMIC_GUIDES_IN_ANGULAR_DESIGNER)
							? Boolean.valueOf(Activator.getDefault().getPreferenceStore().getBoolean(Activator.SHOW_DYNAMIC_GUIDES_IN_ANGULAR_DESIGNER))
							: Boolean.FALSE;
					}
					if (args.has("sameSizeIndicator"))
					{
						return Boolean.valueOf(new DesignerPreferences().getShowSameSizeFeedback());
					}
					if (args.has("anchoringIndicator"))
					{
						return Boolean.valueOf(new DesignerPreferences().getShowAnchorFeedback());
					}
					if (args.has("isHideInherited"))
					{
						RfbVisualFormEditorDesignPage rfbVisualFormEditorDesignPage = (RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor();
						return Boolean.valueOf(rfbVisualFormEditorDesignPage != null
							? rfbVisualFormEditorDesignPage.getPartProperty(VisualFormEditor.PROPERTY_HIDE_INHERITED) : null);
					}
					if (args.has(Activator.SHOW_I18N_VALUES_IN_ANGULAR_DESIGNER))
					{
						return Activator.getDefault().getPreferenceStore().contains(Activator.SHOW_I18N_VALUES_IN_ANGULAR_DESIGNER)
							? Boolean.valueOf(Activator.getDefault().getPreferenceStore().getBoolean(Activator.SHOW_I18N_VALUES_IN_ANGULAR_DESIGNER))
							: Boolean.FALSE;
					}
				}
				return Boolean.FALSE;
			}

		});
		configuredHandlers.put("getShortcuts", new ShortcutsHandler(editorPart));
		configuredHandlers.put("activated", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				if (editorPart != PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart())
				{
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(editorPart);
				}

				return null;
			}
		});
		configuredHandlers.put("initialized", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				// first set focus is too early, make sure focus is properly set
				if (editorPart == PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart())
				{
					editorPart.setFocus();
				}
				return null;
			}
		});
		configuredHandlers.put("reload", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).refreshBrowserUrl(true);
				return null;
			}
		});

		configuredHandlers.put("getComponentPropertyWithTags", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				IPersist persist = PersistFinder.INSTANCE.searchForPersist(editorPart.getForm(), args.optString("svyId"));
				if (persist instanceof AbstractBase)
				{
					return ((AbstractBase)persist).getProperty(args.optString("propertyName"));
				}
				return null;
			}
		});
		configuredHandlers.put("openPackageManager", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				EditorUtil.openWebPackageManager();
				return null;
			}
		});
		configuredHandlers.put("createGroup", new GroupCommand(editorPart, selectionProvider));
		configuredHandlers.put("clearGroup", new UngroupCommand(editorPart, selectionProvider));
		configuredHandlers.put("getAllowedChildren", new LayoutsHandler());

		configuredHandlers.put("responsive_move_up", new MoveUpCommand(editorPart, selectionProvider));
		configuredHandlers.put("responsive_move_down", new MoveDownCommand(editorPart, selectionProvider));

		configuredHandlers.put("toggleHideInherited", new IServerService()
		{

			@Override
			public Object executeMethod(String methodName, JSONObject args)
			{
				RfbVisualFormEditorDesignPage rfbVisualFormEditorDesignPage = (RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor();
				Boolean hideInherited = Boolean.valueOf(rfbVisualFormEditorDesignPage.getPartProperty(VisualFormEditor.PROPERTY_HIDE_INHERITED));
				rfbVisualFormEditorDesignPage.setPartProperty(VisualFormEditor.PROPERTY_HIDE_INHERITED,
					Boolean.toString(!hideInherited.booleanValue()));
				if (DesignerUtil.getContentOutline() != null)
				{
					IPage outline = DesignerUtil.getContentOutline().getCurrentPage();
					if (outline instanceof FormOutlinePage)
					{
						((FormOutlinePage)outline).refresh();
					}
				}
				return null;
			}
		});

		configuredHandlers.put("getSuperForms", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				final Form openForm = editorPart != null ? editorPart.getForm() : null;
				if (openForm == null) return null;

				List<AbstractBase> forms = PersistHelper.getOverrideHierarchy(openForm);
				JSONArray superforms = new JSONArray();
				for (AbstractBase form : forms)
				{
					superforms.put(((ISupportName)form).getName());
				}
				return superforms;
			}

		});

		configuredHandlers.put("setCssAnchoring", new SetCssAnchoringHandler(editorPart));

		configuredHandlers.put("getFormFixedSize", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				JSONObject result = new JSONObject();
				IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
				if (preferenceStore.contains(editorPart.getForm().getUUID() + "_width"))
				{
					result.put("width", preferenceStore.getString(editorPart.getForm().getUUID() + "_width"));
				}
				if (preferenceStore.contains(editorPart.getForm().getUUID() + "_height"))
				{
					result.put("height", preferenceStore.getString(editorPart.getForm().getUUID() + "_height"));
				}
				return result;
			}
		});
		configuredHandlers.put("setFormFixedSize", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				if (args != null)
				{
					IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
					if (args.has("width"))
					{
						preferenceStore.setValue(editorPart.getForm().getUUID() + "_width", args.getString("width"));
					}
					if (args.has("height"))
					{
						preferenceStore.setValue(editorPart.getForm().getUUID() + "_height", args.getString("height"));
					}
				}
				return null;
			}
		});

		configuredHandlers.put("getZoomLevel", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				if (args != null)
				{
					IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
					if (preferenceStore.contains(editorPart.getForm().getUUID() + "_zoomLevel"))
					{
						return new Integer(preferenceStore.getInt(editorPart.getForm().getUUID() + "_zoomLevel"));
					}
				}
				return new Integer(3);
			}

		});

		configuredHandlers.put("setZoomLevel", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				if (args != null)
				{
					IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
					if (args.has("zoomLevel"))
					{
						preferenceStore.setValue(editorPart.getForm().getUUID() + "_zoomLevel", args.getInt("zoomLevel"));
					}
				}
				return null;
			}

		});

		configuredHandlers.put("openConfigurator", new OpenPropertiesWizardHandler(selectionProvider));
		configuredHandlers.put("getWizardProperties", new GetWizardProperties());

		configuredHandlers.put("getVariantsForCategory", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				FlattenedSolution fl = ServoyModelFinder.getServoyModel().getActiveProject().getEditingFlattenedSolution();
				String category = args.getString("variantCategory");
				JSONArray variants = fl.getVariantsHandler().getVariantsForCategory(category);
				return variants;
			}
		});

		configuredHandlers.put("getSnapThresholds", new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject args) throws Exception
			{
				JSONObject settings = new JSONObject();
				DesignerPreferences designerPreferences = new DesignerPreferences();
				settings.put("alignment", Integer.valueOf(designerPreferences.getTitaniumAlignmentThreshold()));
				settings.put("distance", Integer.valueOf(designerPreferences.getTitaniumSnapEqualDistanceThreshold()));
				settings.put("size", Integer.valueOf(designerPreferences.getTitaniumSnapEqualSizeThreshold()));
				return settings;
			}
		});
	}

	@Override
	public Object executeMethod(final String methodName, final JSONObject args)
	{
		try
		{
			return UIUtils.runInUI(new Callable<Object>()
			{
				@Override
				public Object call() throws Exception
				{
					return configuredHandlers.get(methodName).executeMethod(methodName, args);
				}
			});
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		return null;
	}
}