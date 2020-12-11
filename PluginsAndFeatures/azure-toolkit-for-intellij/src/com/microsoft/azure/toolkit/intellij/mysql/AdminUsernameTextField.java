/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.toolkit.intellij.mysql;

import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.StringUtils;

public class AdminUsernameTextField extends ValidationDebouncedTextInput {

    /**
     * Admin username must be at least 1 characters and at most 16 characters.
     * Admin username must only contain characters and numbers.
     * Admin login name cannot be 'azure_superuser', 'admin', 'administrator', 'root', 'guest' or 'public'
     */
    @NotNull
    public AzureValidationInfo doValidateValue() {
        final AzureValidationInfo info = super.doValidateValue();
        if (!AzureValidationInfo.OK.equals(info)) {
            return info;
        }
        final String value = this.getValue();
        // validate length
        if (StringUtils.length(value) < 1 || StringUtils.length(value) > 16) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this).message("Admin username must be at least 1 characters and at most 16 characters.")
                    .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate special character
        if (!StringUtils.isAlphanumeric(value)) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this).message("Admin username must only contain characters and numbers.").type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate special admin username
        if (StringUtils.equalsIgnoreCase(value, "azure_superuser")
                || StringUtils.equalsIgnoreCase(value, "admin")
                || StringUtils.equalsIgnoreCase(value, "administrator")
                || StringUtils.equalsIgnoreCase(value, "root")
                || StringUtils.equalsIgnoreCase(value, "guest")
                || StringUtils.equalsIgnoreCase(value, "public")) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this).message("Admin login name cannot be 'azure_superuser', 'admin', 'administrator', 'root', 'guest' or 'public'.")
                    .type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.OK;
    }

    @Override
    public boolean isRequired() {
        return true;
    }

}
