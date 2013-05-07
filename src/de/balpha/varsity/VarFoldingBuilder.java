package de.balpha.varsity;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VarFoldingBuilder extends FoldingBuilderEx {

    private static int mMinChars;

    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {

        boolean noFoldPrimitives = !PropertiesComponent.getInstance().getBoolean("foldprimitives", true);
        mMinChars = PropertiesComponent.getInstance().getOrInitInt("minchars", 3);

        List<FoldingDescriptor> descriptors = new ArrayList<FoldingDescriptor>();

        Collection<PsiMethod> methods = PsiTreeUtil.findChildrenOfType(root, PsiMethod.class);

        for (final PsiMethod method : methods) {

            Collection<PsiLocalVariable> vars = PsiTreeUtil.findChildrenOfType(method, PsiLocalVariable.class);
            Collection<PsiForeachStatement> iters = PsiTreeUtil.findChildrenOfType(method, PsiForeachStatement.class);
            FoldingGroup group = FoldingGroup.newGroup(method.getName());

            for (final PsiVariable var : vars) {
                PsiTypeElement typeElem = var.getTypeElement();
                if (typeElem == null)
                    continue;
                PsiType vartype = var.getType();
                PsiExpression initializer = var.getInitializer();

                if (initializer == null)
                    continue;

                PsiType rightType = initializer.getType();

                if (rightType == null)
                    continue;

                if (noFoldPrimitives && !(rightType instanceof PsiClassType || rightType instanceof PsiArrayType))
                    continue;

                boolean doFold = false;

                if (rightType instanceof PsiClassType) {
                    PsiClass rightClass = ((PsiClassType)rightType).resolve();
                    if (rightClass instanceof PsiAnonymousClass) {
                        if (((PsiAnonymousClass)rightClass).getBaseClassType().equals(vartype))
                            doFold = true;
                    }
                }

                doFold |= rightType.equals(vartype);

                TextRange range = typeElem.getTextRange();

                if (doFold && rangeMakesSenseToFold(range)) {
                    descriptors.add(new FoldingDescriptor(typeElem.getNode(), range, group));
                }
            }

            for (final PsiForeachStatement iter : iters) {
                
                PsiType vartype = iter.getIterationParameter().getType();
                PsiType iterType = (PsiType)iter.getIteratedValue().getType();

                if ((iterType instanceof PsiClassType && isIterableOf((PsiClassType)iterType, vartype))
                        ||
                        (iterType instanceof PsiArrayType && ((PsiArrayType)iterType).getComponentType().equals(vartype))
                    ) {
                    TextRange range = iter.getIterationParameter().getTypeElement().getTextRange();
                    if (rangeMakesSenseToFold(range))
                        descriptors.add(new FoldingDescriptor(iter.getIterationParameter().getTypeElement().getNode(), range, group));
                }
            }
        }
        return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
    }

    private static boolean rangeMakesSenseToFold(TextRange range) {
        return range.getLength() >= mMinChars;
    }

    private static boolean isIterableOf(PsiClassType iter, PsiType var) {
        if (iter.resolve() != null && "java.lang.Iterable".equals(iter.resolve().getQualifiedName()) && iter.getParameters()[0].equals(var))
            return true;
        for (PsiType interf : iter.getSuperTypes()) {
            if (interf instanceof PsiClassType && isIterableOf((PsiClassType)interf, var))
                return true;
        }
        return false;

    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        return "var";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return true;
    }
}