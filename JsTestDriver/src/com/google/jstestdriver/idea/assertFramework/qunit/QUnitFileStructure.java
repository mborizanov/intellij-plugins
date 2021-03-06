package com.google.jstestdriver.idea.assertFramework.qunit;

import com.google.common.collect.Lists;
import com.google.inject.internal.Maps;
import com.google.jstestdriver.idea.assertFramework.AbstractTestFileStructure;
import com.google.jstestdriver.idea.assertFramework.JstdRunElement;
import com.google.jstestdriver.idea.util.JsPsiUtils;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class QUnitFileStructure extends AbstractTestFileStructure {

  private final List<QUnitModuleStructure> myNonDefaultModuleStructures = Lists.newArrayList();
  private final Map<String, QUnitModuleStructure> myNonDefaultModuleStructureByNameMap = Maps.newHashMap();
  private final DefaultQUnitModuleStructure myDefaultModuleStructure = new DefaultQUnitModuleStructure(this);
  private Map<PsiElement, String> myNameByPsiElementMap;

  public QUnitFileStructure(@NotNull JSFile jsFile) {
    super(jsFile);
  }

  @Override
  public boolean isEmpty() {
    return !hasQUnitSymbols();
  }

  public int getAllModuleCount() {
    return myNonDefaultModuleStructures.size() + 1;
  }

  public int getNonDefaultModuleCount() {
    return myNonDefaultModuleStructures.size();
  }

  public void addModuleStructure(@NotNull QUnitModuleStructure moduleStructure) {
    myNonDefaultModuleStructureByNameMap.put(moduleStructure.getName(), moduleStructure);
    myNonDefaultModuleStructures.add(moduleStructure);
  }

  @Nullable
  public String getNameByPsiElement(@NotNull PsiElement psiElement) {
    return myNameByPsiElementMap.get(psiElement);
  }

  @Nullable
  public AbstractQUnitModuleStructure findQUnitModuleByName(@NotNull String qunitModuleName) {
    AbstractQUnitModuleStructure moduleStructure = myNonDefaultModuleStructureByNameMap.get(qunitModuleName);
    if (moduleStructure == null) {
      if (myDefaultModuleStructure.getName().equals(qunitModuleName)) {
        moduleStructure = myDefaultModuleStructure;
      }
    }
    return moduleStructure;
  }

  @NotNull
  public DefaultQUnitModuleStructure getDefaultModuleStructure() {
    return myDefaultModuleStructure;
  }

  public boolean hasQUnitSymbols() {
    return myDefaultModuleStructure.getTestCount() > 0 || getNonDefaultModuleCount() > 0;
  }

  @Nullable
  public QUnitModuleStructure findModuleStructureContainingOffset(int offset) {
    for (QUnitModuleStructure moduleStructure : myNonDefaultModuleStructures) {
      TextRange moduleTextRange = moduleStructure.getEnclosingCallExpression().getTextRange();
      if (JsPsiUtils.containsOffsetStrictly(moduleTextRange, offset)) {
        return moduleStructure;
      }
    }
    return null;
  }

  @Nullable
  public QUnitTestMethodStructure findTestMethodStructureContainingOffset(int offset) {
    QUnitTestMethodStructure testMethodStructure = myDefaultModuleStructure.findTestMethodStructureContainingOffset(
      offset);
    if (testMethodStructure != null) {
      return testMethodStructure;
    }
    for (QUnitModuleStructure moduleStructure : myNonDefaultModuleStructures) {
      testMethodStructure = moduleStructure.findTestMethodStructureContainingOffset(offset);
      if (testMethodStructure != null) {
        return testMethodStructure;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public JstdRunElement findJstdRunElement(@NotNull TextRange textRange) {
    for (QUnitModuleStructure nonDefaultModuleStructure : myNonDefaultModuleStructures) {
      JstdRunElement jstdRunElement = nonDefaultModuleStructure.findJstdRunElement(textRange);
      if (jstdRunElement != null) {
        return jstdRunElement;
      }
    }
    return myDefaultModuleStructure.findJstdRunElement(textRange);
  }

  @Override
  public PsiElement findPsiElement(@NotNull String testCaseName, @Nullable String testMethodName) {
    AbstractQUnitModuleStructure qunitModuleStructure = findQUnitModuleByName(testCaseName);
    if (qunitModuleStructure != null) {
      if (testMethodName != null) {
        String name = StringUtil.trimStart(testMethodName, QUnitTestMethodStructure.JSTD_NAME_PREFIX);
        QUnitTestMethodStructure test = qunitModuleStructure.getTestMethodStructureByName(name);
        if (test != null) {
          return test.getCallExpression();
        }
      } else {
        QUnitModuleStructure nonDefault = ObjectUtils.tryCast(qunitModuleStructure, QUnitModuleStructure.class);
        if (nonDefault != null) {
          return nonDefault.getEnclosingCallExpression();
        }
      }
    }
    return null;
  }

  @NotNull
  @Override
  public List<String> getTopLevelElements() {
    List<String> out = new ArrayList<String>(myNonDefaultModuleStructures.size() + 1);
    if (myDefaultModuleStructure.getTestCount() > 0) {
      out.add(myDefaultModuleStructure.getName());
    }
    for (QUnitModuleStructure structure : myNonDefaultModuleStructures) {
      out.add(structure.getName());
    }
    return out;
  }

  @NotNull
  @Override
  public List<String> getChildrenOf(@NotNull String topLevelElementName) {
    AbstractQUnitModuleStructure moduleStructure = findQUnitModuleByName(topLevelElementName);
    if (moduleStructure == null) {
      return Collections.emptyList();
    }
    List<String> out = new ArrayList<String>(moduleStructure.getTestCount());
    for (QUnitTestMethodStructure methodStructure : moduleStructure.getTestMethodStructures()) {
      out.add("test " + methodStructure.getName());
    }
    return out;
  }

  @Override
  public boolean contains(@NotNull String testCaseName, @Nullable String testMethodName) {
    AbstractQUnitModuleStructure qunitModuleStructure = findQUnitModuleByName(testCaseName);
    if (qunitModuleStructure == null) {
      return false;
    }
    if (testMethodName != null) {
      String name = StringUtil.trimStart(testMethodName, QUnitTestMethodStructure.JSTD_NAME_PREFIX);
      QUnitTestMethodStructure test = qunitModuleStructure.getTestMethodStructureByName(name);
      return test != null;
    }
    return true;
  }

  void postProcess() {
    myNameByPsiElementMap = createNameByPsiElementMap();
  }

  @NotNull
  private Map<PsiElement, String> createNameByPsiElementMap() {
    int count = myDefaultModuleStructure.getTestCount();
    for (QUnitModuleStructure nonDefaultModuleStructure : myNonDefaultModuleStructures) {
      count += nonDefaultModuleStructure.getTestCount() + 1;
    }
    if (count == 0) {
      return Collections.emptyMap();
    }
    Map<PsiElement, String> nameMap = new IdentityHashMap<PsiElement, String>(count);
    for (QUnitModuleStructure moduleStructure : myNonDefaultModuleStructures) {
      PsiElement moduleElement = moduleStructure.getEnclosingCallExpression();
      nameMap.put(moduleElement, moduleStructure.getName());
      handleModuleStructure(moduleStructure, nameMap);
    }
    handleModuleStructure(myDefaultModuleStructure, nameMap);
    return nameMap;
  }

  private static void handleModuleStructure(@NotNull AbstractQUnitModuleStructure moduleStructure,
                                            @NotNull Map<PsiElement, String> nameMap) {
    for (QUnitTestMethodStructure testMethodStructure : moduleStructure.getTestMethodStructures()) {
      PsiElement methodElement = testMethodStructure.getCallExpression();
      nameMap.put(methodElement, testMethodStructure.getName());
    }
  }

}
