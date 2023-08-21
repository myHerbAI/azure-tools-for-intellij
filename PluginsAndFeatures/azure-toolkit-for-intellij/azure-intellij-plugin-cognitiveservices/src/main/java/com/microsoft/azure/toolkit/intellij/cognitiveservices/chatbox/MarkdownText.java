/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownText {
    private final String markdown;
    @Getter
    private final List<Part> parts;

    public MarkdownText(String markdown) {
        this.markdown = markdown;
        this.parts = this.split();
    }

    public List<Part> split() {
        final List<Part> parts = new ArrayList<>();
        final Pattern codeBlockPattern = Pattern.compile("```(.*?)```", Pattern.DOTALL);
        final Matcher codeBlockMatcher = codeBlockPattern.matcher(markdown);
        int currentIndex = 0;

        while (codeBlockMatcher.find()) {
            final int codeBlockStartIndex = codeBlockMatcher.start();
            final int codeBlockEndIndex = codeBlockMatcher.end();

            if (codeBlockStartIndex > currentIndex) {
                parts.add(new Part(markdown.substring(currentIndex, codeBlockStartIndex), PartType.OTHER));
            }

            final String codeBlock = markdown.substring(codeBlockStartIndex, codeBlockEndIndex);
            parts.add(new CodePart(codeBlock));
            currentIndex = codeBlockEndIndex;
        }

        if (currentIndex < markdown.length()) {
            parts.add(new Part(markdown.substring(currentIndex), PartType.OTHER));
        }

        return parts;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Part {
        private final String text;
        private final PartType type;
    }

    @Getter
    public static class CodePart extends Part {
        private final String language;
        private final String code;

        public CodePart(String block) {
            super(block, PartType.CODE_BLOCK);
            final Pattern pattern = Pattern.compile("```(.*?)\n(.*?)```", Pattern.DOTALL);
            final Matcher matcher = pattern.matcher(block);
            if (matcher.find()) {
                this.language = matcher.group(1);
                this.code = matcher.group(2);
            } else {
                this.language = "txt";
                this.code = "";
            }
        }

        public CodePart(String code, String language) {
            super(String.format("```%s\n%s```", language, code), PartType.CODE_BLOCK);
            this.language = language;
            this.code = code;
        }
    }

    public enum PartType {
        CODE_BLOCK, OTHER
    }
}
