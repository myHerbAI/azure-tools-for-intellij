/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azureexplorer.helpers;

import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.azureexplorer.editors.container.ContainerRegistryExplorerEditor;
import com.microsoft.azuretools.azureexplorer.editors.container.ContainerRegistryExplorerEditorInput;
import com.microsoft.azuretools.azureexplorer.editors.rediscache.RedisExplorerEditor;
import com.microsoft.azuretools.azureexplorer.editors.rediscache.RedisExplorerEditorInput;
import com.microsoft.azuretools.azureexplorer.views.RedisPropertyView;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.tooling.msservices.helpers.UIHelper;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;

public class UIHelperImpl implements UIHelper {

    private static final String UNABLE_TO_OPEN_BROWSER = "Unable to open external web browser";
    private static final String UNABLE_TO_GET_PROPERTY = "Error opening view page";
    private static final String UNABLE_TO_OPEN_EXPLORER = "Unable to open explorer";

    @Override
    public void showException(final String message,
                              final Throwable ex,
                              final String title,
                              final boolean appendEx,
                              final boolean suggestDetail) {
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    PluginUtil.displayErrorDialogAndLog(null, title, message, ex);
                }
            });
        } else {
            PluginUtil.displayErrorDialogAndLog(null, title, message, ex);
        }
    }

    @Override
    public void showError(final String message, final String title) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                PluginUtil.displayErrorDialog(null, title, message);
            }
        });
    }

    @Override
    public boolean showConfirmation(@NotNull String message, @NotNull String title, @NotNull String[] options, String defaultOption) {
        boolean choice = MessageDialog.openConfirm(PluginUtil.getParentShell(),
                title,
                message);

        return choice;
    }

    @Override
    public void logError(String message, Throwable ex) {
        Activator.getDefault().log(message, ex);
    }

    @Override
    public File showFileChooser(String title) {
        FileDialog dialog = new FileDialog(new Shell(), SWT.SAVE);
        dialog.setOverwrite(true);
//        IProject selProject = PluginUtil.getSelectedProject();
//        if (selProject != null) {
//            String path = selProject.getLocation().toPortableString();
//            dialog.setFilterPath(path);
//        }
        dialog.setText(title);
        String fileName = dialog.open();
        if (fileName == null || fileName.isEmpty()) {
            return null;
        } else {
            return new File(fileName);
        }
    }

    @Override
    public File showFileSaver(String title, String name) {
        FileDialog dialog = new FileDialog(new Shell(), SWT.SAVE);
        dialog.setOverwrite(true);
        dialog.setText(title);
        dialog.setFileName(name);
//        dialog.setFilterExtensions(new String[] {FilenameUtils.getExtension(name)});
        String fileName = dialog.open();
        if (fileName == null || fileName.isEmpty()) {
            return null;
        } else {
            return new File(fileName);
        }
    }

    @Override
    public boolean isDarkTheme() {
        return false;
    }

    @Override
    public void openContainerRegistryPropertyView(@NotNull ContainerRegistryNode node) {
        String sid = node.getSubscriptionId();
        String resId = node.getResourceId();
        if (Utils.isEmptyString(sid) || Utils.isEmptyString(resId)) {
            return;
        }
        IWorkbench workbench = PlatformUI.getWorkbench();
        ContainerRegistryExplorerEditorInput input = new ContainerRegistryExplorerEditorInput(sid, resId, node.getName());
        IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(ContainerRegistryExplorerEditor.ID);
        openEditor(EditorType.CONTAINER_EXPLORER, input, descriptor);
    }

    @Override
    public void openInBrowser(String link) {
        try {
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(link));
        } catch (Exception e) {
            showException(UNABLE_TO_OPEN_BROWSER, e, UNABLE_TO_OPEN_BROWSER, false, false);
        }
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void openEditor(EditorType type, IEditorInput input, IEditorDescriptor descriptor) {
        try {
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
            if (activeWorkbenchWindow == null) {
                return;
            }
            IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
            if (page == null) {
                return;
            }
            switch (type) {
                case REDIS_EXPLORER:
                case CONTAINER_EXPLORER:
                case WEBAPP_EXPLORER:
                    page.openEditor(input, descriptor.getId());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            showException(UNABLE_TO_OPEN_EXPLORER, e, UNABLE_TO_OPEN_EXPLORER, false, false);
        }
    }

    private void openView(String viewId, String sid, String resId) {
        try {
            IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (activeWorkbenchWindow == null) {
                return;
            }
            IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
            if (page == null) {
                return;
            }
            switch (viewId) {
                case RedisPropertyView.ID:
                    final RedisPropertyView redisPropertyView = (RedisPropertyView) page.showView(RedisPropertyView.ID,
                            resId, IWorkbenchPage.VIEW_ACTIVATE);
                    break;
                default:
                    break;
            }
        } catch (PartInitException e) {
            showException(UNABLE_TO_GET_PROPERTY, e, UNABLE_TO_GET_PROPERTY, false, false);
        }
    }

    @Override
    public void showInfo(Node node, String message) {
        // TODO Auto-generated method stub

    }

    @Override
    public void showError(Node node, String s) {
        // TODO Auto-generated method stub
    }

}
