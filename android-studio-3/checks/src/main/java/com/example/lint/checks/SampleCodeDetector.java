/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.lint.checks;

import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;

import org.jetbrains.uast.UAnnotated;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClassInitializer;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_STRING;

/**
 * Sample detector showing how to analyze Kotlin/Java code.
 * This example flags all string literals in the code that contain
 * the word "lint".
 */
public class SampleCodeDetector extends Detector implements UastScanner {
    private static final String TYPE_CONTEXT = "android.content.Context";
    /** Issue describing the problem and pointing to the detector implementation */
    public static final Issue ISSUE = Issue.create(
            // ID: used in @SuppressLint warnings etc
            "MethodNameIsIncoherent",

            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            "Lint Mentions",

            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            "This check highlights string literals in code which mentions ",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    SampleCodeDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    public static final Issue LISTENER_ISSUE = Issue.create(
            // ID: used in @SuppressLint warnings etc
            "ListenerAccess",

            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            "Listener Mentions",

            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            "This check highlights method calls in code which mentions listener",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    SampleCodeDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    public static final Issue CONTEXT_ISSUE = Issue.create(
            // ID: used in @SuppressLint warnings etc
            "ContextAccess",

            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            "Context Mentions",

            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            "This check highlights method calls in code which mentions context",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    SampleCodeDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    public static final String LISTENER = "listener";

    public static final String CONTEXT = "context";

    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Arrays.asList(UCallExpression.class, UMethod.class);
    }

   //@android.annotation.SuppressLint("ContextAccess")
    @Override
    public UElementHandler createUastHandler(JavaContext context) {
        // Note: Visiting UAST nodes is a pretty general purpose mechanism;
        // Lint has specialized support to do common things like "visit every class
        // that extends a given super class or implements a given interface", and
        // "visit every call site that calls a method by a given name" etc.
        // Take a careful look at UastScanner and the various existing lint check
        // implementations before doing things the "hard way".
        // Also be aware of context.getJavaEvaluator() which provides a lot of
        // utility functionality.
        return new UElementHandler() {

//            @Override
//            public void visitInitializer(UClassInitializer node) {
//                node.
//            }
//
            @Override
            public void visitMethod(UMethod node) {
                String name = node.getName().toLowerCase(Locale.ROOT);
                List<UParameter> parameters = node.getUastParameters();

                if (parameters.isEmpty()) {
                    return;
                }

                JavaEvaluator evaluator = context.getEvaluator();
                PsiModifierList modifierList = node.getModifierList();
                if (modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
                    return;
                }

                {
                    boolean result = false;
                    for (UParameter parameter : parameters) {
                        String s = parameter.getName().toLowerCase(Locale.ROOT);
                        if (s.contains(LISTENER)) {
                            result = true;
                            break;
                        }
                    }
                    boolean matches = name.contains(LISTENER);
                    if (result && !matches) {
                        context.report(ISSUE, node, context.getLocation(node),
                                "This method has parameter named `listener`");
                    }

                    if (matches && !result) {
                        context.report(ISSUE, node, context.getLocation(node),
                                "This method mentions `listener` but has no parameter named `listener`");
                    }
                }
                {
                    boolean result = false;
                    for (UParameter parameter : parameters) {
                        String s = parameter.getName().toLowerCase(Locale.ROOT);
                        if (s.contains(CONTEXT)) {
                            result = true;

                            if (!evaluator.typeMatches(parameter.getType(), TYPE_CONTEXT)) {
                                context.report(CONTEXT_ISSUE, node, context.getLocation(node),
                                        "This code does not mention parameter of type " + TYPE_CONTEXT);
                                break;
                            }

                            break;
                        }
                    }
                    boolean matches = name.contains(CONTEXT);
                    /*
                    if (result && !matches) {
                        context.report(ISSUE, node, context.getLocation(node),
                                "This method does not mention `context` but has parameter named `context`");
                    }
                    */

                    if (matches && !result) {
                        context.report(ISSUE, node, context.getLocation(node),
                                "This method mentions `context` but has no parameter named `context`");
                    }
                }

                //System.out.println(name);
            }

            @Override
            public void visitCallExpression(UCallExpression node) {
                JavaEvaluator evaluator = context.getEvaluator();

                String name = node.getMethodName();

                if (name == null) {
                    return;
                }

                List<UExpression> valueArguments = node.getValueArguments();

                if (valueArguments.isEmpty())
                {
                    return;
                }

//                PsiMethod method = node.resolve();
//
//                if (method != null && evaluator.isOverride(method, true)) {
//                    return;
//                }

                name = name.toLowerCase(Locale.ROOT);

//                if (name.contains(LISTENER)) {
//                    context.report(LISTENER_ISSUE, node, context.getLocation(node),
//                            "This code mentions `listener`");
//                }
// todo
                if (name.contains(CONTEXT)) {
                    context.report(CONTEXT_ISSUE, node, context.getLocation(node),
                            "This code mentions `context`");
                }

                for (UExpression parameter : valueArguments) {
                    String s = parameter.toString().toLowerCase(Locale.ROOT);
                    if (s.contains(CONTEXT)) {
                        context.report(CONTEXT_ISSUE, node, context.getLocation(node),
                                "This code mentions parameter `context`");
                        break;
                    }
                    //PsiElement javaPsi = parameter.getSourcePsi();

                    //evaluator.isOverride()



                    if (evaluator.typeMatches(parameter.getExpressionType(), TYPE_CONTEXT)) {
                        context.report(CONTEXT_ISSUE, node, context.getLocation(node),
                                "This code mentions parameter of type " + TYPE_CONTEXT);
                        break;
                    }
                }
            }
        };
    }
}
