package de.balpha.varsity;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


public class VarExpander extends EnterHandlerDelegateAdapter {

    @Override
    public Result preprocessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull Ref<Integer> caretOffset, @NotNull Ref<Integer> caretAdvance, @NotNull DataContext dataContext, EditorActionHandler originalHandler) {
        if (!(file instanceof PsiJavaFile))
            return null;

        final PsiElement onCursor = file.findElementAt(caretOffset.get());

        boolean handleVal = PropertiesComponent.getInstance().getBoolean("val", false);

        for (PsiTypeElement element : PsiTreeUtil.findChildrenOfType(file, PsiTypeElement.class)) {
            boolean isVal = false;

            if (!element.getType().getCanonicalText().equals("var")) {
                if (!handleVal || !element.getType().getCanonicalText().equals("val"))
                    continue;
                isVal = true;
            }
            if (element.getTextOffset() > caretOffset.get())
                continue;

            PsiDeclarationStatement dec = PsiTreeUtil.getParentOfType(element, PsiDeclarationStatement.class);

            if (dec != null) {

                if (dec.getChildren().length != 1 || !(dec.getChildren()[0] instanceof PsiLocalVariable))
                    continue;

                final PsiLocalVariable locVar = (PsiLocalVariable)dec.getChildren()[0];
                String typeText = locVar.getType().getCanonicalText();
                if (!(typeText.equals("var") || typeText.equals("val")))
                    continue;

                PsiExpression init = locVar.getInitializer();
                if (init == null)
                    continue;

                PsiType rightType = init.getType();
                replace(locVar, rightType, editor, file, onCursor, caretOffset, isVal);
                continue;
            }

            PsiForeachStatement forEachStatement = getAncestorOfType(element, PsiForeachStatement.class);

            if (forEachStatement != null && element.equals(forEachStatement.getIterationParameter().getTypeElement())) {
                PsiParameter var = forEachStatement.getIterationParameter();
                PsiType iterType = (PsiType)forEachStatement.getIteratedValue().getType();

                PsiType iteratedType;
                if (iterType instanceof PsiArrayType) {
                    iteratedType = ((PsiArrayType) iterType).getComponentType();
                } else if (iterType instanceof PsiClassType) {
                    iteratedType = getIteratedType((PsiClassType)iterType);
                    if (iteratedType == null)
                        iteratedType = PsiClassType.getJavaLangObject(PsiManager.getInstance(file.getProject()), iterType.getResolveScope());
                } else {
                    continue;
                }

                replace(var, iteratedType, editor, file, onCursor, caretOffset, isVal);
            }
        }
        return Result.Continue;
    }

    private <T extends PsiElement> T getAncestorOfType(PsiElement el, Class<T> type) {
        PsiElement parent = el.getParent();
        if (parent == null)
            return null;
        if (type.isInstance(parent))
            return (T)parent;
        return getAncestorOfType(parent, type);

    }

    private PsiType getIteratedType(PsiClassType iter) {
        if (iter.resolve() != null
                && "java.lang.Iterable".equals(iter.resolve().getQualifiedName())
                && iter.getParameters().length > 0)
            return iter.getParameters()[0];
        for (PsiType interf : iter.getSuperTypes()) {
            if (interf instanceof PsiClassType) {
                PsiType t = getIteratedType((PsiClassType)interf);

                if (t != null)
                    return t;
                }
        }
        return null;

    }


    private static void replace(PsiVariable var, PsiType realType, Editor editor, PsiFile file, PsiElement refElement, Ref<Integer> caretOffset, boolean makeFinal) {
        if(realType == null)
            return;
        if (realType instanceof PsiClassReferenceType && ((PsiClassReferenceType) realType).resolve() == null)
            return;

        PsiElementFactory factory = PsiElementFactory.SERVICE.getInstance(editor.getProject());

        PsiTypeElement copy = factory.createTypeElement(realType);

        int old = refElement.getTextOffset();
        var.getTypeElement().replace(copy);
        if (makeFinal)
            var.getModifierList().setModifierProperty("final", true);

        if (realType instanceof PsiClassType) {
            PsiElement ref = ((PsiJavaFile)file).findImportReferenceTo(((PsiClassType) realType).resolve());

            if (ref != null) {
                PsiElement imp = ref.getParent();
                PsiElement next = imp.getNextSibling();

                if (next == null || !(next instanceof PsiWhiteSpace)) {

                    PsiWhiteSpace ws = PsiTreeUtil.findChildrenOfType(factory.createExpressionFromText("1\n+1", imp), PsiWhiteSpace.class).iterator().next();

                    Collection<PsiForeachStatement> test = PsiTreeUtil.findChildrenOfType(file, PsiForeachStatement.class);


                    imp.getParent().addAfter(ws, imp);
                }
            }
        }

        int delta = refElement.getTextOffset() - old;
        caretOffset.set(caretOffset.get() + delta);



    }
}
