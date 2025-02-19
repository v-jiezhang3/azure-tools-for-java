/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.bicep.highlight;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.bicep.activities.BicepStartupActivity;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.plugins.textmate.TextMateService;
import org.jetbrains.plugins.textmate.language.TextMateLanguageDescriptor;
import org.jetbrains.plugins.textmate.language.syntax.highlighting.TextMateEditorHighlighterProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class BicepEditorHighlighterProvider extends TextMateEditorHighlighterProvider {
    protected static final Logger LOG = Logger.getInstance(BicepEditorHighlighterProvider.class);

    @Override
    @AzureOperation("platform/bicep.get_editor_highlighter")
    public EditorHighlighter getEditorHighlighter(@Nullable Project project, @Nonnull FileType fileType, @Nullable VirtualFile virtualFile, @Nonnull EditorColorsScheme colors) {
        try {
            final TextMateLanguageDescriptor descriptor = TextMateService.getInstance().getLanguageDescriptorByExtension("bicep");
            if (Objects.isNull(descriptor)) { // register textmate if not registered
                if (BicepStartupActivity.registerBicepTextMateBundle()) {
                    TextMateService.getInstance().reloadEnabledBundles();
                }
            }
            return super.getEditorHighlighter(project, fileType, virtualFile, colors);
        } catch (final ProcessCanceledException e) {
            LOG.warn(e);
            return new EmptyEditorHighlighter();
        }
    }
}
